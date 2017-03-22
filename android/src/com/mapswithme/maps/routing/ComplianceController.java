package com.mapswithme.maps.routing;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import au.net.transtech.geo.model.MultiPointRoute;
import au.net.transtech.geo.model.VehicleProfile;
import com.mapswithme.maps.Framework;
import com.mapswithme.maps.R;
import com.mapswithme.maps.location.DemoLocationProvider;
import com.mapswithme.maps.location.LocationHelper;
import com.mapswithme.maps.location.LocationListener;
import com.mapswithme.maps.location.MockLocation;
import com.mapswithme.maps.sound.TtsPlayer;
import com.mapswithme.transtech.MessageBuilder;
import com.mapswithme.transtech.Setting;
import com.mapswithme.transtech.SettingConstants;
import com.mapswithme.transtech.TranstechConstants;
import com.mapswithme.transtech.route.RouteConstants;
import com.mapswithme.transtech.route.RouteGeofence;
import com.mapswithme.transtech.route.RouteOffset;
import com.mapswithme.transtech.route.RouteUtil;
import com.mapswithme.util.concurrency.ThreadPool;
import com.mapswithme.util.concurrency.UiThread;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengts.util.GeoPoint;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

/**
 * Class ComplianceController
 * <p/>
 * Created by agough on 3/08/16 3:17 PM
 */
public class ComplianceController implements LocationListener, GraphHopperRouter.RouteListener
{
    private static final String TAG = "SmartNav2_ComplianceController";

    private static final long CHECK_FREQUENCY_MS = 3000L; //check network compliance frequency
    private static final long NOTIFY_FREQUENCY_MS = 10000L; //check network compliance frequency
    public static final double OFFROUTE_THRESHOLD = 150.0;  //150 metres

    public static final ComplianceController INSTANCE = new ComplianceController();

    public static enum ComplianceMode
    {
        ROUTE_COMPLIANCE,
        NETWORK_ADHERENCE,
        NONE
    }

    private ComplianceMode defaultMode = ComplianceMode.NONE;
    private ComplianceMode requestedMode = ComplianceMode.NONE;
    private ComplianceMode currentMode = ComplianceMode.NONE;

    private ComplianceController()
    {
    }

    public static ComplianceController get()
    {
        return INSTANCE;
    }

    private LocationHelper.UiCallback callback;
    private Location lastLoc;
    private UUID groupId;
    private long lastTts = 0L;
    private GraphHopperRouter ghRouter;
    private int currentRouterType = Framework.ROUTER_TYPE_EXTERNAL;
    private Integer plannedRouteId;
    private MultiPointRoute currentRoute;
    private List<RouteGeofence> geofences;

    static boolean DEMO_MODE = false;

    private static enum ComplianceState
    {
        UNKNOWN,
        ON_ROUTE,
        OFF_ROUTE
    }

    private ComplianceState complianceState = ComplianceState.UNKNOWN;

    public void init(final LocationHelper.UiCallback callback)
    {
        this.callback = callback;
        requestedMode = defaultMode;

        String imei = Setting.getString( callback.getActivity(),
                Setting.Environment.ALL,
                Setting.Scope.COMMON,
                SettingConstants.GLOBAL_DEVICE_ID,
                null );

        Log.i( TAG, "Retrieved IMEI is " + imei );
        if( !TextUtils.isEmpty( imei ) && "358683065071954".equals( imei ) ) //GOUGHY TEST
            DEMO_MODE = true;

        //trigger GraphHopper initialisation
        ThreadPool.getWorker().submit( new Runnable()
        {
            @Override
            public void run()
            {
                final GraphHopperRouter router = getRouter( callback.getActivity() );
                Framework.nativeSetExternalRouter( router );

                setDefaultMode( GraphHopperRouter.NETWORK_CAR.equals( router.getSelectedProfile().getCode() )
                        ? ComplianceMode.NONE
                        : ComplianceMode.NETWORK_ADHERENCE );

                router.setListener(INSTANCE);
            }
        } );
    }

    public GraphHopperRouter getRouter( Context context )
    {
        if( ghRouter == null )
            ghRouter = new GraphHopperRouter( context );

        return ghRouter;
    }

    @Override
    public GraphHopperRouter.RouteListener.Response onRouteCalculated( int type, MultiPointRoute route )
    {
        GraphHopperRouter.RouteListener.Response response = Response.SUCCESS;

        if( currentRoute == null )
            currentRoute = route;
        else if( currentMode == ComplianceMode.ROUTE_COMPLIANCE )
        {
            RouteOffset offset = checkPositionAgainstRoute( lastLoc.getLatitude(), lastLoc.getLongitude() );
            Log.i( TAG, "REROUTE while performing ROUTE COMPLIANCE: " + offset );

            tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_REROUTE,
                    RouteConstants.SUB_TYPE_ROUTE_PLANNED, offset.distance );

            //houston, we have a problem.  We are currently following a route and have had
            //to recalculate, so we must be off-route
            if( complianceState != ComplianceState.OFF_ROUTE )
                doOffRouteProcessing( lastLoc, offset.distance );

            //do we need to have the GH router recalculate the route from our current position to
            //the nearest point on the current pre-planned route
            response = Response.REROUTE_TO_PLANNED_ROUTE;
        }
        else
        {
            tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_REROUTE, RouteConstants.SUB_TYPE_DEVICE_NETWORK, null );
            response = Response.REROUTE_TO_DESTINATION;
        }
        return response;
    }

    public void selectPlannedRoute( Integer routeId )
    {
        if( currentRoute != null )
            stop();

        currentRoute = null;
        plannedRouteId = routeId;

        if( currentRouterType == Framework.ROUTER_TYPE_EXTERNAL && routeId != null )
        {
            ghRouter.setPlannedRouteId( routeId );
            Log.i( TAG, "Setting selected planned route to trip: " + routeId );
            requestedMode = ComplianceMode.ROUTE_COMPLIANCE;
        }
        else
            requestedMode = defaultMode;
    }

    public boolean setNetworkProfile(String profile)
    {
        Log.i( TAG, "Network profile is: " + profile );
        if( ghRouter != null )
            ghRouter.setSelectedProfile( profile );

        setDefaultMode( GraphHopperRouter.NETWORK_CAR.equalsIgnoreCase( profile )
                ? ComplianceMode.NONE
                : ComplianceMode.NETWORK_ADHERENCE );

        return true;
    }

    public void setDefaultMode( ComplianceMode mode )
    {
        Log.i( TAG, "Default network adherence mode is: " + mode.name() );
        defaultMode = mode;
    }

    public ComplianceMode getDefaultMode() { return defaultMode; }
    public ComplianceMode getRequestedMode() { return requestedMode; }
    public ComplianceMode getCurrentMode() { return currentMode; }

    public void start()
    {
        if( currentMode != requestedMode )
            stop();

        complianceState = ComplianceState.ON_ROUTE; //benefit of the doubt
        LocationHelper.INSTANCE.addListener( this, false );
        lastTts = 0L;

        if( currentMode != requestedMode || groupId == null )
        {
            currentMode = requestedMode;

            groupId = UUID.randomUUID();
            Log.i( TAG, "ComplianceController::start() starting a " + currentMode + " trip: groupId " + groupId );

            switch( currentMode )
            {
                case ROUTE_COMPLIANCE:
                    ghRouter.setPlannedRouteId( plannedRouteId );
                    tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_STARTED,
                            RouteConstants.SUB_TYPE_ROUTE_PLANNED, null );
                    geofences = RouteUtil.findTripGeofences( callback.getActivity(), plannedRouteId.intValue() );
                    break;

                case NETWORK_ADHERENCE:
//                    tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_STARTED,
//                            RouteConstants.SUB_TYPE_DEVICE_NETWORK, null );
                    break;

                case NONE:
                default:
                    break; //no trip event
            }
        }
    }

    public void stop()
    {
        complianceState = ComplianceState.UNKNOWN;
        lastTts = 0L;

        Log.i( TAG, "ComplianceController::stop() stopping " + currentMode + " trip: groupId " + groupId );
        if( ghRouter != null && groupId != null )
        {
            switch( currentMode )
            {
                case ROUTE_COMPLIANCE:
                    tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_FINISHED,
                            RouteConstants.SUB_TYPE_ROUTE_PLANNED, null );
                    break;

                case NETWORK_ADHERENCE:
//                    tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_FINISHED,
//                            RouteConstants.SUB_TYPE_DEVICE_NETWORK, null );
                    break;

                case NONE:
                default:
                    break; //no trip event
            }
        }

        ghRouter.setPlannedRouteId( null );

        setDefaultMode( GraphHopperRouter.NETWORK_CAR.equals( ghRouter.getSelectedProfile().getCode() )
                ? ComplianceMode.NONE
                : ComplianceMode.NETWORK_ADHERENCE );

        currentMode = defaultMode;
        groupId = null;
    }

    @Override
    public void onLocationUpdated( final Location location )
    {
        if( complianceState == ComplianceState.UNKNOWN || currentMode == ComplianceMode.NONE )
            return;

        if( isCheckRequired( location ) )
        {
            lastLoc = location;

            RouteOffset offset = checkPositionAgainstRoute( location.getLatitude(), location.getLongitude() );
            if( offset != null && offset.distance > OFFROUTE_THRESHOLD )
            {
                //we are only off route
                if( currentMode != ComplianceMode.ROUTE_COMPLIANCE || offset.geofenceCount == 0 )
                {
                    Log.w( TAG, "BEEEP! OFF-ROUTE!" );
                    complianceState = ComplianceState.OFF_ROUTE;
                    doOffRouteProcessing( location, offset.distance );
                }
            }
            else
            {
                if( complianceState == ComplianceState.OFF_ROUTE )
                {
                    Log.w( TAG, "Whew, Back ON-ROUTE!" );
                    lastTts = 0L;
                    doOnRouteProcessing( location );
                }
                complianceState = ComplianceState.ON_ROUTE;
            }

        }
    }

    @Override
    public void onCompassUpdated( long time, double magneticNorth, double trueNorth, double accuracy )
    {

    }

    @Override
    public void onLocationError( int errorCode )
    {

    }

    private boolean isCheckRequired( Location loc )
    {
        if( ghRouter == null || loc == null || currentRouterType != Framework.ROUTER_TYPE_EXTERNAL )
            return false;

        if( lastLoc == null )
            return true;

        return loc.getTime() - lastLoc.getTime() > CHECK_FREQUENCY_MS;
    }

    private boolean isNotifyRequired( Location loc )
    {
        if( ghRouter == null || loc == null || currentRouterType != Framework.ROUTER_TYPE_EXTERNAL )
            return false;

        return loc.getTime() - lastTts > NOTIFY_FREQUENCY_MS;
    }

    public RouteOffset checkPositionAgainstRoute( double fromLat, double fromLon )
    {
        RouteOffset offset = new RouteOffset();
        switch( currentMode )
        {
            case ROUTE_COMPLIANCE:
                //check whether we are off route by seeing if we are within threshold distance or
                // in _any_ of the trips geofences...
                offset = RouteUtil.distanceFromPath( fromLat, fromLon, currentRoute == null ? null : currentRoute.getPath() );
                offset.geofenceCount = countInsideGeofences( fromLat, fromLon );
                Log.i( TAG, "Checking route compliance: current pos [" + fromLat + "," + fromLon + "] nearest point is ["
                        + (offset.nearestPoint == null ? "???" : offset.nearestPoint.getLatitude())
                        + "," + (offset.nearestPoint == null ? "???" : offset.nearestPoint.getLongitude())
                        + "] and " + userFacingDistance( offset.distance )
                        + " from route and inside " + offset.geofenceCount + " geofences" );
                break;

            case NETWORK_ADHERENCE:
                offset.distance = ghRouter.distanceFromNetwork( fromLat, fromLon );
                Log.i( TAG, "Checking network compliance: distance from network is " + userFacingDistance( offset.distance ) );
                break;
            case NONE:
            default:
                break;
        }

        return offset;
    }

    private int countInsideGeofences( double lat, double lng )
    {
        if( geofences == null || geofences.size() == 0 )
            return 0;

        GeoPoint pos = new GeoPoint(lat, lng);
        int numInside = 0;
        for( RouteGeofence gf : geofences )
        {
            if( gf.getGeoPolygon() != null && gf.getGeoPolygon().isPointInside( pos ) )
                numInside++;
        }
        return numInside;
    }

    private void doOffRouteProcessing( Location location, double distanceFromRoute )
    {
        if( currentMode == ComplianceMode.ROUTE_COMPLIANCE )
        {
            if( complianceState != ComplianceState.OFF_ROUTE )
            {
                tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_EXIT,
                        RouteConstants.SUB_TYPE_ROUTE_PLANNED, distanceFromRoute );
            }
            if( isNotifyRequired( location ) )
            {
                TtsPlayer.INSTANCE.sayThis( "you are currently off the planned rowt" );
                lastTts = location.getTime();
            }
            showToast( "RouteCompliance", "You are approximately " + userFacingDistance( distanceFromRoute ) + " from the planned route", Color.parseColor( "#FFF54137" ) );
        }
        else if( currentMode == ComplianceMode.NETWORK_ADHERENCE )
        {
            VehicleProfile profile = ghRouter.getSelectedProfile();
            if( complianceState != ComplianceState.OFF_ROUTE )
            {
                tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_EXIT,
                        RouteConstants.SUB_TYPE_DEVICE_NETWORK, distanceFromRoute );
            }
            if( isNotifyRequired( location ) )
            {
                TtsPlayer.INSTANCE.sayThis( "you are currently off the " + profile.getDescription() + " network" );
                lastTts = location.getTime();
            }

            StringBuilder msg = new StringBuilder();
            msg.append( "You are off the \"" )
                    .append( profile.getDescription() )
                    .append( "\" network" );
            showToast( "Network Adherence", msg.toString(), Color.parseColor( "#FFF54137" ) );
        }
    }

    private void doOnRouteProcessing( Location location )
    {
        if( currentMode == ComplianceMode.ROUTE_COMPLIANCE )
        {
            if( complianceState != ComplianceState.ON_ROUTE )
            {
                tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_ENTRY,
                        RouteConstants.SUB_TYPE_ROUTE_PLANNED, null );

                TtsPlayer.INSTANCE.sayThis( "you are back on the planned rowt" );
                showToast( "RouteCompliance", "You are back on the planned route", Color.parseColor( "#FF558B2F" ) );
            }
        }
        else if( currentMode == ComplianceMode.NETWORK_ADHERENCE )
        {
            if( complianceState != ComplianceState.ON_ROUTE )
            {
                VehicleProfile profile = ghRouter.getSelectedProfile();
                tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_ENTRY,
                        RouteConstants.SUB_TYPE_DEVICE_NETWORK, null );

                TtsPlayer.INSTANCE.sayThis( "you are back on the " + profile.getDescription() + " network" );

                StringBuilder msg = new StringBuilder();
                msg.append( "You are now back on the \"" )
                        .append( profile.getDescription() )
                        .append( "\" network" );
                showToast( "Network Adherence", msg.toString(), Color.parseColor( "#FF558B2F" ) );
            }
        }
    }

    public void showToast( String title, final String msg, final int color )
    {
        UiThread.runLater( new Runnable()
        {
            @Override
            public void run()
            {
//                Utils.toastShortcut(callback.getActivity(), msg);
                LayoutInflater inflater = callback.getActivity().getLayoutInflater();
                ViewGroup group = (ViewGroup) callback.getActivity().findViewById( R.id.custom_toast_container );
                View layout = inflater.inflate( R.layout.custom_toast, group );
                layout.setBackgroundColor( color );
                layout.setTextAlignment( View.TEXT_ALIGNMENT_CENTER );

                TextView text = (TextView) layout.findViewById( R.id.compliance_text );
                text.setText( msg );
                text.setTextAlignment( View.TEXT_ALIGNMENT_CENTER );

                Toast toast = new Toast( callback.getActivity().getApplicationContext() );
                toast.setGravity( Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 50 );
                toast.setDuration( Toast.LENGTH_SHORT );
                toast.setView( layout );
                toast.show();
            }
        } );
    }

    private void tripEvent( String type, String subType, Double distFrom )
    {
        try
        {
            VehicleProfile profile = ghRouter.getSelectedProfile();

            JSONObject event = new JSONObject();
            event.put( "GPS", toJSON( lastLoc ) );
            event.put( "RecordType", type );
            event.put( RouteConstants.SOURCE, "Device" );
            event.put( RouteConstants.MODE, currentMode != null ? currentMode.name() : ComplianceMode.NONE.name() );

            if( subType != null )
                event.put( RouteConstants.SUB_TYPE, subType );

            if( groupId != null )
                event.put( RouteConstants.GROUP_ID, groupId.toString() );

            if( profile != null )
                event.put( RouteConstants.NETWORK, toJSON( profile ) );

            if( plannedRouteId != null )
                event.put( RouteConstants.TRIP_ID, plannedRouteId.intValue() );
            else
                event.put( RouteConstants.TRIP_ID, -1L );

            if( distFrom != null )
                event.put( "Distance", distFrom );

            Intent intent = new MessageBuilder()
                    .routingKey( TranstechConstants.AMQP_ROUTING_KEY_ROUTE_TRIP )
                    .priority( MessageBuilder.Priority.NORMAL )
                    .content( event )
                    .build();

            callback.getActivity().startService( intent );
            Log.e( TAG, "Sent trip event " + type + ", groupId " + groupId );
        }
        catch( Exception e )
        {
            Log.e( TAG, "Failed to send trip event " + type, e );
        }
    }

    public final static DecimalFormat sixDecimalDigitFormat = new DecimalFormat( "0.000000" );
    public final static DecimalFormat oneDecimalDigitFormat = new DecimalFormat( "0.0" );
    public final static DecimalFormat twoDecimalDigitFormat = new DecimalFormat( "0.00" );

    private JSONObject toJSON( Location loc ) throws JSONException
    {
        JSONObject gps = new JSONObject();
        if( loc == null )
        {
            gps.put( "Lat", JSONObject.NULL );
            gps.put( "Lng", JSONObject.NULL );
            gps.put( "Dir", JSONObject.NULL );
            gps.put( "Alt", JSONObject.NULL );
            gps.put( "Spd", -1 );
            gps.put( "NSat", DemoLocationProvider.NUM_SATELLITES_INVALID );
            gps.put( "HDOP", DemoLocationProvider.HDOP_INVALID );
        }
        else
        {
            gps.put( "Lat", Double.valueOf( sixDecimalDigitFormat.format( loc.getLatitude() ) ) );
            gps.put( "Lng", Double.valueOf( sixDecimalDigitFormat.format( loc.getLongitude() ) ) );
            gps.put( "Dir", !loc.hasBearing() ? JSONObject.NULL : loc.getBearing() );
            gps.put( "Alt", !loc.hasAltitude() ? JSONObject.NULL : Double.valueOf( oneDecimalDigitFormat.format( loc.getAltitude() ) ) );
            gps.put( "Spd", !loc.hasSpeed() ? -1 : Double.valueOf( oneDecimalDigitFormat.format( loc.getSpeed() * DemoLocationProvider.MPS_TO_KMH ) ) );

            if( loc instanceof MockLocation )
            {
                float accuracy = loc.getAccuracy();
                int nsat = (int) ((accuracy / 10.0f) - (accuracy % 10));
                float hdop = accuracy - (nsat * 10.0f);

                gps.put( "NSat", nsat );
                gps.put( "HDOP", Double.valueOf( oneDecimalDigitFormat.format( hdop ) ) );
            }
        }
        return gps;
    }

    private JSONObject toJSON( VehicleProfile profile ) throws JSONException
    {
        JSONObject obj = new JSONObject();
        obj.put( RouteConstants.NETWORK_CODE, profile.getCode() );
        obj.put( RouteConstants.NETWORK_DESC, profile.getDescription() );
        return obj;
    }

    private String userFacingDistance( double distInMetres )
    {
        DecimalFormat df = new DecimalFormat( "#.#" );
        if( distInMetres < 1000 )
            return df.format( distInMetres ) + "m";

        return df.format( distInMetres / 1000.0 ) + "km";
    }
}

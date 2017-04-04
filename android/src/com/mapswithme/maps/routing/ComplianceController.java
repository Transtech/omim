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
import au.net.transtech.geo.model.Position;
import au.net.transtech.geo.model.VehicleProfile;
import com.mapswithme.maps.Framework;
import com.mapswithme.maps.R;
import com.mapswithme.maps.bookmarks.data.MapObject;
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
    private static final String TAG = "ComplianceController";

    private static final long CHECK_FREQUENCY_MS = 5000L; //check network compliance frequency
    private static final long NOTIFY_FREQUENCY_MS = 15000L; //driver notification frequency
    public static final double OFFROUTE_THRESHOLD = 150.0;  //150 metres

    private static final String NOTIFY_GREEN = "#C0197841";
    private static final String NOTIFY_RED   = "#C0F54137";
    private static final String NOTIFY_BLUE  = "#C01E96F0";

    public static final ComplianceController INSTANCE = new ComplianceController();

    public static enum ComplianceMode
    {
        ROUTE_COMPLIANCE,
        NETWORK_ADHERENCE,
        NONE
    }

    private ComplianceMode defaultMode = ComplianceMode.NONE;
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
    private List<RouteGeofence> geofences;
    private int rerouteCount = 0;

    private static String imei = null;

    private static enum ComplianceState
    {
        UNKNOWN,
        ON_ROUTE,
        OFF_ROUTE
    }

    private ComplianceState complianceState = ComplianceState.UNKNOWN;

    public String init( final LocationHelper.UiCallback callback )
    {
        this.callback = callback;
        rerouteCount = 0;

        if( imei == null )
        {
            String imei = Setting.getString( callback.getActivity(),
                    Setting.Environment.ALL,
                    Setting.Scope.COMMON,
                    SettingConstants.GLOBAL_DEVICE_ID,
                    null );

            Log.i( TAG, "Retrieved IMEI is " + imei );
//            if( !TextUtils.isEmpty( imei ) && "358683065071954".equals( imei ) ) //GOUGHY TEST
//            {
//                DemoLocationProvider.DEMO_MODE = true;
//                DemoLocationProvider.setLocation( -37.8496619, 145.0684303 );
//            }

            //trigger GraphHopper initialisation
            ThreadPool.getWorker().submit( new Runnable()
            {
                @Override
                public void run()
                {
                    final GraphHopperRouter router = getRouter( callback.getActivity() );
                    if( Framework.nativeGetRouter() != Framework.ROUTER_TYPE_EXTERNAL )
                        Framework.nativeSetExternalRouter( router );

                    setDefaultMode( GraphHopperRouter.NETWORK_CAR.equals( router.getSelectedProfile().getCode() )
                            ? ComplianceMode.NONE
                            : ComplianceMode.NETWORK_ADHERENCE );

                    router.setListener( INSTANCE );
                }
            } );
        }
        return null;
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

        if( rerouteCount++ == 0 )
            ; //do nothing
        else if( currentMode == ComplianceMode.ROUTE_COMPLIANCE )
        {
            RouteOffset offset = checkPositionAgainstRoute( lastLoc.getLatitude(), lastLoc.getLongitude() );
            Log.i( TAG, "REROUTE while performing ROUTE COMPLIANCE: " + offset );

            tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_REROUTE,
                    RouteConstants.SUB_TYPE_ROUTE_PLANNED, offset.toJSON() );

            //houston, we have a problem.  We are currently following a route and have had
            //to recalculate, so we must be off-route
            if( complianceState != ComplianceState.OFF_ROUTE )
                doOffRouteProcessing( lastLoc, offset );

            //do we need to have the GH router recalculate the route from our current position to
            //the nearest point on the current pre-planned route
            response = Response.REROUTE_TO_PLANNED_ROUTE;
        }
        else
        {
            tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_REROUTE,
                    RouteConstants.SUB_TYPE_DEVICE_NETWORK, null );
            response = Response.REROUTE_TO_DESTINATION;
        }
        return response;
    }

    public void selectPlannedRoute( Integer routeId, String routeName )
    {
        if( ghRouter.getPlannedRoute() != null )
            stop( "ComplianceController::selectPlannedRoute()" );

        plannedRouteId = routeId;

        if( currentRouterType == Framework.ROUTER_TYPE_EXTERNAL && routeId != null )
        {
            ghRouter.setPlannedRouteId( routeId );
            Log.i( TAG, "Setting selected planned route to trip: " + routeId );
        }
    }

    public boolean setNetworkProfile( String profile )
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

    public ComplianceMode getDefaultMode()
    {
        return defaultMode;
    }

    public ComplianceMode getCurrentMode()
    {
        return currentMode;
    }

    public void start( String where )
    {
        Log.i( TAG, "ComplianceController::start() called from " + where + ". Currently " + currentMode + " trip: groupId " + groupId );
        boolean restartRequired = shouldRestartRouteComplianceTrip();

//        if( !TextUtils.isEmpty( imei ) && "358683065071954".equals( imei ) ) //GOUGHY TEST
//        {
//            DemoLocationProvider.DEMO_MODE = true;
//            LocationHelper.INSTANCE.restart();
//        }
//
        if( restartRequired )
            return;

        complianceState = ComplianceState.ON_ROUTE; //benefit of the doubt
        LocationHelper.INSTANCE.addListener( this, false );
        lastTts = 0L;
        rerouteCount = 0;

        if( (currentMode == ComplianceMode.NONE && defaultMode != ComplianceMode.NONE)
                || (plannedRouteId != null && currentMode != ComplianceMode.ROUTE_COMPLIANCE) )
        {
            currentMode = (plannedRouteId == null)
                    ? ComplianceMode.NETWORK_ADHERENCE
                    : ComplianceMode.ROUTE_COMPLIANCE;

            if( groupId == null )
                groupId = UUID.randomUUID();

            Log.i( TAG, "ComplianceController::start() starting a " + currentMode + " trip: groupId " + groupId );

            switch( currentMode )
            {
                case ROUTE_COMPLIANCE:
                    startRouteComplianceTrip( groupId );
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

    public void stop( String where )
    {
        DemoLocationProvider.DEMO_MODE = false;

        complianceState = ComplianceState.UNKNOWN;
        lastTts = 0L;
        rerouteCount = 0;

        Log.i( TAG, "ComplianceController::stop() called from " + where + ". Stopping " + currentMode + " trip: groupId " + groupId );
        if( ghRouter != null && groupId != null )
        {
            switch( currentMode )
            {
                case ROUTE_COMPLIANCE:
                    stopRouteComplianceTrip();
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
        currentMode = ComplianceMode.NONE;
        groupId = null;
        complianceState = ComplianceState.UNKNOWN;

        if( defaultMode != ComplianceMode.NONE )
        {
            UiThread.runLater( new Runnable()
            {
                @Override
                public void run()
                {
                    start( "ComplianceController::stop()" );
                }
            });
        }
    }

    private boolean shouldRestartRouteComplianceTrip()
    {
        if( plannedRouteId == null )
        {
            final String previousRoute = Setting.getString( callback.getActivity(),
                    Setting.currentEnvironment( callback.getActivity() ),
                    Setting.Scope.SMARTNAV2,
                    SettingConstants.ROUTE_CURRENT,
                    "" );

            if( !TextUtils.isEmpty( previousRoute ) )
            {
                try
                {
                    JSONObject obj = new JSONObject( previousRoute );
                    final int plannedId = obj.optInt( "id", 0 );
                    final double endLat = obj.optDouble( "endLat", 0.0 );
                    final double endLng = obj.optDouble( "endLng", 0.0 );
                    final String grpId = obj.optString( "groupId" );

                    if( plannedId > 0 )
                    {
                        groupId = UUID.fromString( grpId );
                        selectPlannedRoute( plannedId, "Restarting" );
                        Log.i( TAG, "We were in the middle of planned route " + plannedId + " - restarting" );

                        UiThread.runLater( new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                showToast( "Route", "Resuming planned route " + plannedId,
                                        Color.parseColor( NOTIFY_BLUE ) );

                                Log.i( TAG, "ComplianceController::start() Calling RoutingController::start() for planned route " + plannedId );
                                RoutingController.get().setEndPoint( new MapObject( MapObject.POI, "", null, null,
                                        endLat, endLng, null, null, false ) );
                                RoutingController.get().start();
                            }
                        }, 2000 );

                        return true;
                    }
                }
                catch( Exception e )
                {
                }
            }
        }
        return false;
    }

    private void startRouteComplianceTrip( UUID groupId )
    {
        ghRouter.setPlannedRouteId( plannedRouteId );
        tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_STARTED,
                RouteConstants.SUB_TYPE_ROUTE_PLANNED, null );
        geofences = RouteUtil.findTripGeofences( callback.getActivity(), plannedRouteId.intValue() );

        Position lastPos = null;
        if( ghRouter.getPlannedRoute() != null
                && ghRouter.getPlannedRoute().getPath() != null
                && ghRouter.getPlannedRoute().getPath().size() > 0 )
        {
            lastPos = ghRouter.getPlannedRoute().getPath().get( ghRouter.getPlannedRoute().getPath().size() - 1 );
        }

        try
        {
            JSONObject obj = new JSONObject();
            obj.put( "id", plannedRouteId );
            obj.put( "groupId", groupId.toString() );
            obj.put( "endLat", lastPos == null ? JSONObject.NULL : lastPos.getLatitude() );
            obj.put( "endLat", lastPos == null ? JSONObject.NULL : lastPos.getLongitude() );

            //persist the current route compliance trip id...
            Setting.setString( callback.getActivity(),
                    Setting.currentEnvironment( callback.getActivity() ),
                    Setting.Scope.SMARTNAV2,
                    SettingConstants.ROUTE_CURRENT,
                    obj.toString(),
                    Setting.Origin.LOCAL,
                    "ComplianceController" );
        }
        catch( JSONException e )
        {
        }

    }

    private void stopRouteComplianceTrip()
    {
        tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_FINISHED,
                RouteConstants.SUB_TYPE_ROUTE_PLANNED, null );
        ghRouter.setPlannedRouteId( null );
        geofences.clear();
        groupId = null;
        plannedRouteId = null;

        Setting.setString( callback.getActivity(),
                Setting.currentEnvironment( callback.getActivity() ),
                Setting.Scope.SMARTNAV2,
                SettingConstants.ROUTE_CURRENT,
                "",
                Setting.Origin.LOCAL,
                "ComplianceController" );
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
                    doOffRouteProcessing( location, offset );
                }
            }
            else
            {
                if( complianceState == ComplianceState.OFF_ROUTE )
                {
                    Log.w( TAG, "Whew, Back ON-ROUTE!" );
                    lastTts = 0L;
                    doOnRouteProcessing( location, offset );
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

        //network adherence takes a _lot_ more effort, so do it less...
        if( currentMode == ComplianceMode.NETWORK_ADHERENCE )
            return loc.getTime() - lastLoc.getTime() > CHECK_FREQUENCY_MS * 4;

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
                offset = RouteUtil.distanceFromPath( fromLat, fromLon,
                        ghRouter.getPlannedRoute() == null ? null : ghRouter.getPlannedRoute().getPath() );
                offset.geofenceCount = countInsideGeofences( fromLat, fromLon );
                Log.i( TAG, "Checking route compliance: current pos [" + fromLat + "," + fromLon + "] nearest point is ["
                        + (offset.nearestPoint == null ? "???" : offset.nearestPoint.getLatitude())
                        + "," + (offset.nearestPoint == null ? "???" : offset.nearestPoint.getLongitude())
                        + "] and " + df.format( offset.distance )
                        + "m from route and inside " + offset.geofenceCount + " geofences" );
                break;

            case NETWORK_ADHERENCE:
                offset.distance = ghRouter.distanceFromNetwork( fromLat, fromLon );
                Log.i( TAG, "Checking network compliance: distance from network is "
                        + df.format( offset.distance ) + "m" );
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

        GeoPoint pos = new GeoPoint( lat, lng );
        int numInside = 0;
        for( RouteGeofence gf : geofences )
        {
            if( gf.getGeoPolygon() != null && gf.getGeoPolygon().isPointInside( pos ) )
                numInside++;
        }
        return numInside;
    }

    private void doOffRouteProcessing( Location location, RouteOffset offset )
    {
        if( currentMode == ComplianceMode.ROUTE_COMPLIANCE )
        {
            if( complianceState != ComplianceState.OFF_ROUTE )
            {
                tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_EXIT,
                        RouteConstants.SUB_TYPE_ROUTE_PLANNED, offset.toJSON() );
            }
            if( isNotifyRequired( location ) )
            {
                TtsPlayer.INSTANCE.sayThis( "you are currently off the planned rowt" );
                lastTts = location.getTime();
            }
            showToast( "Route", "You are approximately "
                    + userFacingDistance( offset.distance ) + " from the planned route",
                    Color.parseColor( NOTIFY_RED ) );
        }
        else if( currentMode == ComplianceMode.NETWORK_ADHERENCE )
        {
            VehicleProfile profile = ghRouter.getSelectedProfile();
            if( complianceState != ComplianceState.OFF_ROUTE )
            {
                tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_EXIT,
                        RouteConstants.SUB_TYPE_DEVICE_NETWORK, offset.toJSON() );
            }
            if( isNotifyRequired( location ) )
            {
                TtsPlayer.INSTANCE.sayThis( "you are currently off the "
                        + profile.getDescription() + " net werk" );
                lastTts = location.getTime();
            }

            StringBuilder msg = new StringBuilder();
            msg.append( "You are off the \"" )
                    .append( profile.getDescription() )
                    .append( "\" network" );
            showToast( "Network", msg.toString(), Color.parseColor( NOTIFY_RED ) );
        }
    }

    private void doOnRouteProcessing( Location location, RouteOffset offset )
    {
        if( currentMode == ComplianceMode.ROUTE_COMPLIANCE )
        {
            if( complianceState != ComplianceState.ON_ROUTE )
            {
                tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_ENTRY,
                        RouteConstants.SUB_TYPE_ROUTE_PLANNED, offset.toJSON() );

                TtsPlayer.INSTANCE.sayThis( "you are back on the planned rowt" );
                showToast( "Route", "You are back on the planned route", Color.parseColor( NOTIFY_GREEN ) );
            }
        }
        else if( currentMode == ComplianceMode.NETWORK_ADHERENCE )
        {
            if( complianceState != ComplianceState.ON_ROUTE )
            {
                VehicleProfile profile = ghRouter.getSelectedProfile();
                tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_ENTRY,
                        RouteConstants.SUB_TYPE_DEVICE_NETWORK, offset.toJSON() );

                TtsPlayer.INSTANCE.sayThis( "you are back on the " + profile.getDescription() + " network" );

                StringBuilder msg = new StringBuilder();
                msg.append( "You are back on the \"" )
                        .append( profile.getDescription() )
                        .append( "\" network" );
                showToast( "Network", msg.toString(), Color.parseColor( NOTIFY_GREEN ) );
            }
        }
    }

    public void showToast( final String title, final String msg, final int color )
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
                text.setText( title + ": " + msg );
                text.setTextAlignment( View.TEXT_ALIGNMENT_CENTER );

                Toast toast = new Toast( callback.getActivity().getApplicationContext() );
                toast.setGravity( Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 50 );
                toast.setDuration( Toast.LENGTH_SHORT );
                toast.setView( layout );
                toast.show();
            }
        } );
    }

    private void tripEvent( String type, String subType, JSONObject attrs )
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

            if( attrs != null )
                event.put( RouteConstants.PARAMS, attrs );

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

    private DecimalFormat df = new DecimalFormat( "#.#" );
    private String userFacingDistance( double distInMetres )
    {
        if( distInMetres < 1000 )
            return ((int)(Math.floor( distInMetres / 10.0 ) * 10.0)) + "m";

        return df.format( distInMetres / 1000.0 ) + "km";
    }
}

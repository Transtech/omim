package com.mapswithme.maps.routing;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import au.net.transtech.geo.model.Position;
import au.net.transtech.geo.model.VehicleProfile;
import com.mapswithme.maps.Framework;
import com.mapswithme.maps.R;
import com.mapswithme.maps.location.DemoLocationProvider;
import com.mapswithme.maps.location.LocationHelper;
import com.mapswithme.maps.location.LocationListener;
import com.mapswithme.maps.location.MockLocation;
import com.mapswithme.maps.sound.TtsPlayer;
import com.mapswithme.transtech.Const;
import com.mapswithme.transtech.TranstechUtil;
import com.mapswithme.transtech.route.RouteConstants;
import com.mapswithme.transtech.route.RouteTrip;
import com.mapswithme.util.concurrency.ThreadPool;
import com.mapswithme.util.concurrency.UiThread;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

/**
 * Class ComplianceController
 * <p/>
 * Created by agough on 3/08/16 3:17 PM
 */
public class ComplianceController implements LocationListener
{
    private static final String TAG = "SmartNav2_ComplianceController";

    private static final long CHECK_FREQUENCY_MS = 3000L; //check network compliance frequency
    private static final long NOTIFY_FREQUENCY_MS = 10000L; //check network compliance frequency
    public static final double OFFROUTE_THRESHOLD = 250.0;  //250 metres

    public static final ComplianceController INSTANCE = new ComplianceController();

    public static enum ComplianceMode
    {
        ROUTE_COMPLIANCE,
        NETWORK_ADHERENCE,
        NONE
    }

    private ComplianceMode requestedMode = ComplianceMode.NONE;
    private ComplianceMode currentMode = ComplianceMode.NONE;

    private ComplianceController()
    {
    }

    public static ComplianceController get()
    {
        return INSTANCE;
    }

    private Activity context;
    private Location lastLoc;
    private UUID groupId;
    private long lastTts = 0L;
    private GraphHopperRouter carRouter;
    private GraphHopperRouter truckRouter;
    private int currentRouterType = Framework.ROUTER_TYPE_EXTERNAL;

    static final boolean DEMO_MODE = true;

    private static enum ComplianceState
    {
        UNKNOWN,
        ON_ROUTE,
        OFF_ROUTE
    }

    private ComplianceState complianceState = ComplianceState.UNKNOWN;

    public void init(final Activity context)
    {
        this.context = context;

        //trigger GraphHopper initialisation
        ThreadPool.getWorker().submit( new Runnable()
        {
            @Override
            public void run()
            {
                Log.i( TAG, "Enforcing routing engine to external TRUCK router (GH)" );
                GraphHopperRouter router = getRouter( context );
                Framework.nativeSetExternalRouter( router );
                router.getGeoEngine(); //trigger initialisation
            }
        } );
    }

    public GraphHopperRouter getRouter( Context context )
    {
        if( truckRouter == null )
            truckRouter = new GraphHopperRouter( context );

        return truckRouter;
    }

    public void setCurrentRouterType( int routerType )
    {
        currentRouterType = routerType;
    }

    public int getCurrentRouterType()
    {
        return currentRouterType;
    }

    public void setPlannedRoute( Integer routeId, String routeName )
    {
        if( getCurrentRouterType() == Framework.ROUTER_TYPE_EXTERNAL && truckRouter != null && routeId != null )
        {
            truckRouter.setPlannedRouteId( routeId );
            Log.i( TAG, "Setting selected planned route to trip: " + routeId + " - " + routeName );
            requestedMode = ComplianceMode.ROUTE_COMPLIANCE;
        }
        else
            requestedMode = ComplianceMode.NONE;
    }

    public ComplianceMode getRequestedMode() { return requestedMode; }
    public ComplianceMode getCurrentMode() { return currentMode; }

    public void start()
    {
        complianceState = ComplianceState.ON_ROUTE; //benefit of the doubt
        LocationHelper.INSTANCE.addListener( this, false );
        lastTts = 0L;
        currentMode = requestedMode;

        if( truckRouter != null )
        {
            VehicleProfile profile = truckRouter.getSelectedProfile();
            groupId = UUID.randomUUID();
            switch( currentMode )
            {
                case ROUTE_COMPLIANCE:
                    tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_STARTED,
                            RouteConstants.SUB_TYPE_ROUTE_PLANNED,
                            groupId.toString(), profile, truckRouter.getComplianceTrip() );
                    break;

                case NETWORK_ADHERENCE:
                    tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_STARTED,
                            RouteConstants.SUB_TYPE_DEVICE_NETWORK,
                            groupId.toString(), profile, null );
                    break;

                case NONE:
                default:
                    break; //no trip event
            }
        }
    }

    public void stop()
    {
        LocationHelper.INSTANCE.removeListener( this );
        complianceState = ComplianceState.UNKNOWN;
        lastTts = 0L;

        if( truckRouter != null && groupId != null )
        {
            VehicleProfile profile = truckRouter.getSelectedProfile();

            switch( currentMode )
            {
                case ROUTE_COMPLIANCE:
                    tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_FINISHED,
                            RouteConstants.SUB_TYPE_ROUTE_PLANNED,
                            groupId.toString(), profile, truckRouter.getComplianceTrip() );
                    break;

                case NETWORK_ADHERENCE:
                    tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_FINISHED,
                            RouteConstants.SUB_TYPE_DEVICE_NETWORK,
                            groupId.toString(), profile, null );
                    break;

                case NONE:
                default:
                    break; //no trip event
            }
        }
        currentMode = requestedMode = ComplianceMode.NONE;
    }

    @Override
    public void onLocationUpdated( final Location location )
    {
        if( complianceState == ComplianceState.UNKNOWN )
            return;

        if( isCheckRequired( location ) )
        {
            lastLoc = location;

            boolean isOffRoute = isOffRoute( location.getLatitude(), location.getLongitude() );
            if( isOffRoute )
            {
                Log.w( TAG, "BEEEP! OFF ROUTE!" );
                complianceState = ComplianceState.OFF_ROUTE;
                doOffRouteProcessing( location );
            }
            else
            {
                if( complianceState == ComplianceState.OFF_ROUTE )
                {
                    Log.w( TAG, "Whew, Back ON route!" );
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
        if( truckRouter == null || loc == null || currentRouterType != Framework.ROUTER_TYPE_EXTERNAL )
            return false;

        if( lastLoc == null )
            return true;

        return loc.getTime() - lastLoc.getTime() > CHECK_FREQUENCY_MS;
    }

    private boolean isNotifyRequired( Location loc )
    {
        if( truckRouter == null || loc == null || currentRouterType != Framework.ROUTER_TYPE_EXTERNAL )
            return false;

        return loc.getTime() - lastTts > NOTIFY_FREQUENCY_MS;
    }

    public boolean isOffRoute( double fromLat, double fromLon )
    {
        boolean offRoute = false;
        switch( currentMode )
        {
            case ROUTE_COMPLIANCE:
                //TODO: check whether we are off route by seeing if we are in _any_ of
                //the trips geofences...
                int numGeofences = 1; //TODO!!
                double minDist = distanceFromRoute(fromLat, fromLon, truckRouter.getComplianceTrip().getPath());
                offRoute = numGeofences == 0 || minDist > OFFROUTE_THRESHOLD;
                Log.i( TAG, "Checking route compliance: we are " + minDist + "m from route and inside " + numGeofences + " geofences" );
                break;
            case NETWORK_ADHERENCE:
                Double dist = truckRouter.distanceFromNetwork( fromLat, fromLon );
                offRoute = dist > OFFROUTE_THRESHOLD;
                Log.i( TAG, "Checking network compliance: distance from network is " + dist + " meters" );
                break;
            case NONE:
            default:
                break;
        }

        return offRoute;
    }

    private double distanceFromRoute( double fromLat, double fromLon, List<Position> path )
    {
        if( path == null || path.size() == 0 )
            return 0.0;

        double minDist = Double.MAX_VALUE;
        for( Position p : path )
        {
            double d = TranstechUtil.haversineDistance( fromLat, fromLon, p.getLatitude(), p.getLongitude() );
            minDist = Math.min( minDist, d );
        }
        return minDist == Double.MAX_VALUE ? 0.0 : minDist;
    }

    private void doOffRouteProcessing( Location location )
    {
        if( currentMode == ComplianceMode.ROUTE_COMPLIANCE )
        {
            VehicleProfile profile = truckRouter.getSelectedProfile();
            tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_EXIT,
                    RouteConstants.SUB_TYPE_ROUTE_PLANNED,
                    groupId.toString(), profile, truckRouter.getComplianceTrip() );

            if( isNotifyRequired( location ) )
            {
                TtsPlayer.INSTANCE.sayThis( "you are currently off the planned rowt" );
                lastTts = location.getTime();
            }
        }
        else if( currentMode == ComplianceMode.NETWORK_ADHERENCE )
        {
            VehicleProfile profile = truckRouter.getSelectedProfile();
            tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_EXIT,
                    RouteConstants.SUB_TYPE_DEVICE_NETWORK,
                    groupId.toString(), profile, null );

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
            VehicleProfile profile = truckRouter.getSelectedProfile();
            tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_ENTRY,
                    RouteConstants.SUB_TYPE_ROUTE_PLANNED,
                    groupId.toString(), profile, truckRouter.getComplianceTrip() );

            TtsPlayer.INSTANCE.sayThis( "you are back on the planned rowt" );
        }
        else if( currentMode == ComplianceMode.NETWORK_ADHERENCE )
        {
            VehicleProfile profile = truckRouter.getSelectedProfile();
            tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_ENTRY,
                    RouteConstants.SUB_TYPE_DEVICE_NETWORK,
                    groupId.toString(), profile, null );

            TtsPlayer.INSTANCE.sayThis( "you are back on the " + profile.getDescription() + " network" );

            StringBuilder msg = new StringBuilder();
            msg.append( "You are now back on the \"" )
                    .append( profile.getDescription() )
                    .append( "\" network" );
            showToast( "Network Adherence", msg.toString(), Color.parseColor( "#FF558B2F" ) );
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
                LayoutInflater inflater = context.getLayoutInflater();
                ViewGroup group = (ViewGroup) context.findViewById( R.id.custom_toast_container );
                View layout = inflater.inflate( R.layout.custom_toast, group );
                layout.setBackgroundColor( color );
                layout.setTextAlignment( View.TEXT_ALIGNMENT_CENTER );

                TextView text = (TextView) layout.findViewById( R.id.compliance_text );
                text.setText( msg );
                text.setTextAlignment( View.TEXT_ALIGNMENT_CENTER );

                Toast toast = new Toast( context.getApplicationContext() );
                toast.setGravity( Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 80 );
                toast.setDuration( Toast.LENGTH_LONG );
                toast.setView( layout );
                toast.show();
            }
        } );
    }

    private void tripEvent( String type, String subType, String groupId, VehicleProfile profile, RouteTrip trip )
    {
        try
        {
            JSONObject event = new JSONObject();
            event.put( "GPS", toJSON( lastLoc ) );
            event.put( "RecordType", type );
            event.put( RouteConstants.SUB_TYPE, subType );
            if( profile != null )
                event.put( RouteConstants.NETWORK, toJSON( profile ) );
            event.put( RouteConstants.GROUP_ID, groupId );
            event.put( RouteConstants.SOURCE, "Device" );
            event.put( RouteConstants.MODE, currentMode.name() );

            if( trip != null )
            {
                event.put( RouteConstants.TRIP_ID, trip.getId() );
                event.put( RouteConstants.TRIP_VERSION, trip.getVersion() );
            }
            else
                event.put( RouteConstants.TRIP_ID, -1L );

            TranstechUtil.publish( context, Const.AMQP_ROUTING_KEY_ROUTE_TRIP,
                    Const.COMMS_EVENT_PRIORITY_NORMAL, event );
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

    private String userFacingDistance( int distInMetres )
    {
        if( distInMetres < 1000 )
            return distInMetres + "m";

        DecimalFormat df = new DecimalFormat( "#.#" );
        return df.format( distInMetres / 1000.0 ) + "km";
    }
}

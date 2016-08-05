package com.mapswithme.maps.routing;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import au.net.transtech.geo.model.VehicleProfile;
import com.mapswithme.maps.location.DemoLocationProvider;
import com.mapswithme.maps.location.LocationHelper;
import com.mapswithme.maps.location.LocationListener;
import com.mapswithme.maps.location.MockLocation;
import com.mapswithme.maps.sound.TtsPlayer;
import com.mapswithme.transtech.RouteConstants;
import com.mapswithme.util.Utils;
import com.mapswithme.util.concurrency.UiThread;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.UUID;

/**
 * Class ComplianceController
 * <p/>
 * Created by agough on 3/08/16 3:17 PM
 */
public class ComplianceController implements LocationListener
{
    private static final String TAG = "Map_ComplianceController";

    private static final long CHECK_FREQUENCY_MS = 3000L; //check network compliance frequency
    public static final double OFFROUTE_THRESHOLD = 150.0;  //150 metres

    private static final ComplianceController sInstance = new ComplianceController();

    public static ComplianceController get()
    {
        return sInstance;
    }

    private LocationHelper.UiCallback callback;
    private GraphHopperRouter router;
    private Location lastLoc;
    private UUID groupId;

    private static enum State
    {
        UNKNOWN,
        ON_ROUTE,
        OFF_ROUTE
    }

    private State complianceState = State.UNKNOWN;

    public GraphHopperRouter getRouter( Context context )
    {
        if( router == null )
            this.router = new GraphHopperRouter( context );

        return router;
    }

    public void attach( LocationHelper.UiCallback cb )
    {
        callback = cb;
    }

    public void detach()
    {
        callback = null;
    }

    public void start()
    {
        complianceState = State.ON_ROUTE; //benefit of the doubt
        LocationHelper.INSTANCE.addListener( this, false );

        if( router != null )
        {
            VehicleProfile profile = router.getSelectedProfile();
            groupId = UUID.randomUUID();
            tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_STARTED,
                    RouteConstants.SUB_TYPE_DEVICE_NETWORK,
                    groupId.toString(), profile );
        }
    }

    public void stop()
    {
        LocationHelper.INSTANCE.removeListener( this );
        complianceState = State.UNKNOWN;

        if( router != null )
        {
            VehicleProfile profile = router.getSelectedProfile();
            tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_FINISHED,
                    RouteConstants.SUB_TYPE_DEVICE_NETWORK,
                    groupId.toString(), profile );
        }
    }

    @Override
    public void onLocationUpdated( Location location )
    {
        if( complianceState == State.UNKNOWN )
            return;

        if( isCheckRequired( location ) )
        {
            final Double dist = router.distanceFromNetwork( location.getLatitude(), location.getLongitude() );
            Log.i( TAG, "Checking network compliance: distance from network is " + dist + " meters" );
            if( dist > OFFROUTE_THRESHOLD )
            {
                Log.w( TAG, "BEEEP! OFF ROUTE!" );
                complianceState = State.OFF_ROUTE;

                VehicleProfile profile = router.getSelectedProfile();
                tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_EXIT,
                        RouteConstants.SUB_TYPE_DEVICE_NETWORK,
                        groupId.toString(), profile );

                if( callback != null )
                {
                    TtsPlayer.INSTANCE.sayThis( "you are currently off route" );
                    StringBuilder msg = new StringBuilder();
                    msg.append( "You are approx. " )
                            .append( dist.intValue() )
                            .append( " meters off the " )
                            .append( profile.getDescription() )
                            .append( " network.\n\nPlease navigate back as soon as possible." );
                    showToast( "Route Compliance", msg.toString() );
                }
            }
            else
            {
                if( complianceState == State.OFF_ROUTE )
                {
                    Log.w( TAG, "Whew, Back ON route!" );

                    VehicleProfile profile = router.getSelectedProfile();
                    tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_ENTRY,
                            RouteConstants.SUB_TYPE_DEVICE_NETWORK,
                            groupId.toString(), profile );

                    if( callback != null )
                    {
                        TtsPlayer.INSTANCE.sayThis( "yabba dabba doo" );
                        StringBuilder msg = new StringBuilder();
                        msg.append( "You are now back on the " )
                                .append( profile.getDescription() )
                                .append( " network.\n\nHappy times :)" );
                        showToast( "Route Compliance", msg.toString() );
                    }
                }
                complianceState = State.ON_ROUTE;
            }
            lastLoc = location;
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
        if( router == null || loc == null )
            return false;

        //don't bother with a distance check if we're only on the car network
        if( router.getSelectedProfile().getCode().equals( GraphHopperRouter.NETWORK_CAR ) )
            return false;

        if( lastLoc == null )
            return true;

        return loc.getTime() - lastLoc.getTime() > CHECK_FREQUENCY_MS;
    }

    public void showToast( String title, final String msg )
    {
        UiThread.runLater( new Runnable()
        {
            @Override
            public void run()
            {
                Utils.toastShortcut(callback.getActivity(), msg);
            }
        } );
    }

    private void tripEvent( String type, String subType, String groupId, VehicleProfile profile )
    {
        try
        {
            JSONObject event = new JSONObject();
            event.put( "GPS", toJSON( lastLoc ) );
            event.put( "RecordType", type );
            event.put( RouteConstants.SUB_TYPE, subType );
            event.put( RouteConstants.TRIP_ID, -1L );
            event.put( RouteConstants.NETWORK, toJSON( profile ) );
            event.put( RouteConstants.GROUP_ID, groupId );
            event.put( RouteConstants.SOURCE, "Device" );

            Intent intent = new Intent( RouteConstants.ACTION_COMMS_RECORD );
            intent.putExtra( "CommsEventRoute", RouteConstants.AMQP_ROUTING_KEY_ROUTE_TRIP );
            intent.putExtra( "CommsEventContent", event.toString() );

            callback.getActivity().startService( intent );
        }
        catch( Exception e )
        {
            Log.e(TAG, "Failed to send trip event " + type, e);
        }
    }

    public final static DecimalFormat sixDecimalDigitFormat = new DecimalFormat("0.000000");
    public final static DecimalFormat oneDecimalDigitFormat = new DecimalFormat("0.0");
    public final static DecimalFormat twoDecimalDigitFormat = new DecimalFormat("0.00");

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
}

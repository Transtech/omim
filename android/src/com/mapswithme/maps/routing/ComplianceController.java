package com.mapswithme.maps.routing;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import au.net.transtech.geo.model.VehicleProfile;
import com.mapswithme.maps.Framework;
import com.mapswithme.maps.R;
import com.mapswithme.maps.location.DemoLocationProvider;
import com.mapswithme.maps.location.LocationHelper;
import com.mapswithme.maps.location.LocationListener;
import com.mapswithme.maps.location.MockLocation;
import com.mapswithme.maps.sound.TtsPlayer;
import com.mapswithme.transtech.RouteConstants;
import com.mapswithme.transtech.Setting;
import com.mapswithme.transtech.SettingConstants;
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
    private static final long NOTIFY_FREQUENCY_MS = 10000L; //check network compliance frequency
    public static final double OFFROUTE_THRESHOLD = 150.0;  //150 metres

    private static final ComplianceController sInstance = new ComplianceController();

    private ComplianceController() {}

    public static ComplianceController get()
    {
        return sInstance;
    }

    private LocationHelper.UiCallback callback;
    private Location lastLoc;
    private UUID groupId;
    private long lastTts = 0L;
    private GraphHopperRouter carRouter;
    private GraphHopperRouter truckRouter;
    private int currentRouterType = Framework.ROUTER_TYPE_TRUCK;

    private static enum State
    {
        UNKNOWN,
        ON_ROUTE,
        OFF_ROUTE
    }

    private State complianceState = State.UNKNOWN;

    public GraphHopperRouter getRouter( Context context, int routerType )
    {
        if( routerType == Framework.ROUTER_TYPE_TRUCK )
        {
            if( truckRouter == null )
                truckRouter = new GraphHopperRouter( context, routerType );

            return truckRouter;
        }

        if( carRouter == null )
            carRouter = new GraphHopperRouter( context, Framework.ROUTER_TYPE_VEHICLE );

        return carRouter;
    }

    public void setCurrentRouterType( int routerType )
    {
        currentRouterType = routerType;
    }

    public int getCurrentRouterType()
    {
        return currentRouterType;
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
        lastTts = 0L;

        if( truckRouter != null )
        {
            VehicleProfile profile = truckRouter.getSelectedProfile();
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
        lastTts = 0L;

        if( truckRouter != null && groupId != null )
        {
            VehicleProfile profile = truckRouter.getSelectedProfile();
            tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_FINISHED,
                    RouteConstants.SUB_TYPE_DEVICE_NETWORK,
                    groupId.toString(), profile );
        }
    }

    @Override
    public void onLocationUpdated( final Location location )
    {
        if( complianceState == State.UNKNOWN )
            return;

        if( isCheckRequired( location ) )
        {
            final Double dist = truckRouter.distanceFromNetwork( location.getLatitude(), location.getLongitude() );
            Log.i( TAG, "Checking network compliance: distance from network is " + dist + " meters" );
            if( dist > OFFROUTE_THRESHOLD )
            {
                Log.w( TAG, "BEEEP! OFF ROUTE!" );
                complianceState = State.OFF_ROUTE;

                VehicleProfile profile = truckRouter.getSelectedProfile();
                tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_EXIT,
                        RouteConstants.SUB_TYPE_DEVICE_NETWORK,
                        groupId.toString(), profile );

                if( callback != null )
                {
                    if( isNotifyRequired( location ) )
                    {
                        TtsPlayer.INSTANCE.sayThis( "you are currently off rowt" );
                        lastTts = location.getTime();
                    }

                    StringBuilder msg = new StringBuilder();
                    msg.append( "You are approximately " )
                            .append( userFacingDistance( dist.intValue() ) )
                            .append( " off the \"" )
                            .append( profile.getDescription() )
                            .append( "\" network" );
                    showToast( "Route Compliance", msg.toString(), Color.parseColor( "#FFF54137" ) );
                }
            }
            else
            {
                if( complianceState == State.OFF_ROUTE )
                {
                    Log.w( TAG, "Whew, Back ON route!" );
                    lastTts = 0L;

                    VehicleProfile profile = truckRouter.getSelectedProfile();
                    tripEvent( RouteConstants.MESSAGE_TYPE_TRIP_ENTRY,
                            RouteConstants.SUB_TYPE_DEVICE_NETWORK,
                            groupId.toString(), profile );

                    if( callback != null )
                    {
                        TtsPlayer.INSTANCE.sayThis( "you are back on a compliant rowt" );

                        StringBuilder msg = new StringBuilder();
                        msg.append( "You are now back on the \"" )
                                .append( profile.getDescription() )
                                .append( "\" network" );
                        showToast( "Route Compliance", msg.toString(), Color.parseColor("#FF558B2F") );
                    }
                }
                complianceState = State.ON_ROUTE;
            }
            lastLoc = location;

            if( LocationHelper.INSTANCE.useDemoGPS() )
            {
                UiThread.run( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        callback.onLocationUpdated( location );
                    }
                } );
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
        if( truckRouter == null || loc == null || currentRouterType != Framework.ROUTER_TYPE_TRUCK )
            return false;

        if( lastLoc == null )
            return true;

        return loc.getTime() - lastLoc.getTime() > CHECK_FREQUENCY_MS;
    }

    private boolean isNotifyRequired( Location loc )
    {
        if( truckRouter == null || loc == null || currentRouterType != Framework.ROUTER_TYPE_TRUCK )
            return false;

        return loc.getTime() - lastTts > NOTIFY_FREQUENCY_MS;
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
                View layout = inflater.inflate(R.layout.custom_toast, group);
                layout.setBackgroundColor( color );
                layout.setTextAlignment( View.TEXT_ALIGNMENT_CENTER );

                TextView text = (TextView) layout.findViewById(R.id.compliance_text);
                text.setText(msg);
                text.setTextAlignment( View.TEXT_ALIGNMENT_CENTER );

                Toast toast = new Toast(callback.getActivity().getApplicationContext());
                toast.setGravity( Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 80);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(layout);
                toast.show();
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

    private String userFacingDistance( int distInMetres )
    {
        if( distInMetres < 1000 )
            return distInMetres + "m";

        DecimalFormat df = new DecimalFormat("#.#");
        return df.format( distInMetres / 1000.0 ) + "km";
    }

}

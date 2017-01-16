package com.mapswithme.maps;

import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.mapswithme.maps.base.BaseMwmFragmentActivity;
import com.mapswithme.maps.bookmarks.data.BookmarkManager;
import com.mapswithme.maps.bookmarks.data.MapObject;
import com.mapswithme.maps.downloader.MapManager;
import com.mapswithme.maps.location.DemoLocationProvider;
import com.mapswithme.maps.location.LocationHelper;
import com.mapswithme.maps.location.MockLocation;
import com.mapswithme.maps.routing.ComplianceController;
import com.mapswithme.maps.routing.GraphHopperRouter;
import com.mapswithme.maps.routing.RoutingController;
import com.mapswithme.util.statistics.AlohaHelper;
import com.mapswithme.util.statistics.Statistics;

/**
 * Class DemoActivity
 * <p/>
 * Created by agough on 15/08/16 11:37 AM
 */
public class DemoActivity extends BaseMwmFragmentActivity
{
    private static final String TAG = "Maps_DemoActivity";
    private static final Location TRANSTECH_OFFICE;
    private static final Location DEMO2_START;
    private static final Location DEMO3_START;

    private static final String NETWORK_CAR = "car";
    private static final String NETWORK_BDOUBLE = "bd";
    private static final String NETWORK_CRANE = "crane";
    private static final String NETWORK_HPFV = "hpfv";

    static {
        TRANSTECH_OFFICE = new MockLocation("DemoLocationProvider");
        TRANSTECH_OFFICE.setLatitude( -37.847002 );
        TRANSTECH_OFFICE.setLongitude( 145.0618573 );
        TRANSTECH_OFFICE.setTime( System.currentTimeMillis() );
        TRANSTECH_OFFICE.setSpeed( 0.0f );
        TRANSTECH_OFFICE.setAccuracy( 20 );

        DEMO2_START = new MockLocation("DemoLocationProvider");
        DEMO2_START.setLatitude( -37.871822609262665 );
        DEMO2_START.setLongitude( 144.9894905090332 );
        DEMO2_START.setTime( System.currentTimeMillis() );
        DEMO2_START.setSpeed( 0.0f );
        DEMO2_START.setAccuracy( 20 );

        DEMO3_START = new MockLocation("DemoLocationProvider");
        DEMO3_START.setLatitude( -37.842240 );
        DEMO3_START.setLongitude( 145.121452 );
        DEMO3_START.setTime( System.currentTimeMillis() );
        DEMO3_START.setSpeed( 0.0f );
        DEMO3_START.setAccuracy( 20 );
    }

    public static MwmActivity mwmActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
//        View demoView = getLayoutInflater().inflate( R.layout.fragment_demo, null );
        setContentView( R.layout.demo_layout );
        setTitle( "ITS Demo" );

        LinearLayout layout = (LinearLayout) findViewById( R.id.demo_layout );

        for( int i = 0; i < 3; i++ )
        {
            View v = getLayoutInflater().inflate( R.layout.demo_item_layout, null );

            ImageView iv = (ImageView) v.findViewById( R.id.img_icon );
            TextView tv = (TextView) v.findViewById( R.id.demo_item_text );
            Button b = (Button) v.findViewById( R.id.demo_item_go );

            iv.setImageResource( R.drawable.ic_rate_full );
            tv.setText( getDemoDescription( i ) );

            final int x = i;
            b.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick( View v )
                {
                    startDemo( x );
                }
            } );

            if( layout != null )
                layout.addView( v );
        }

        //locate the 2 router types to trigger initilisation
        GraphHopperRouter carRouter = ComplianceController.get().getRouter( get(), Framework.ROUTER_TYPE_VEHICLE);
        GraphHopperRouter truckRouter = ComplianceController.get().getRouter( get(), Framework.ROUTER_TYPE_TRUCK);

        processIntent( getIntent() );
    }

    private void processIntent( Intent intent )
    {

    }

    private Spanned getDemoDescription( int id )
    {
        switch( id )
        {
            case 0:
                return formatEntry( "Network Compliance (On-route/Off-route)",
                        "Alert the driver when they drive off the compliant network (B-Double)");
            case 1:
                return formatEntry( "Route Compliance",
                        "Only allow the local device to route on a compliant network (B-Double)");
            case 2:
                return formatEntry("Network Compliance when Rerouting",
                        "The local device reroutes on a compliant network when driver diverts (B-Double)");
        }

        return formatEntry("Whoops", "Not that many demos defined!");
    }

    private Spanned formatEntry( String header, String desc)
    {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append( header );
        ssb.setSpan( new StyleSpan( Typeface.BOLD ), 0, header.length(), 0 );
        ssb.setSpan( new RelativeSizeSpan( 1.8f ), 0, header.length(), 0 );
        if( desc != null )
            ssb.append( "\n" ).append( desc );

        return ssb;
    }

    private void startDemo( int id )
    {
        switch( id )
        {
            case 0:
                startNetworkComplianceDemo();
                break;

            case 1:
                startRouteComplianceDemo();
                break;

            case 2:
                startReRouteDemo();
                break;

            default:
                Toast.makeText( get(), "Demo " + id + " not yet implemented, sorry.", Toast.LENGTH_SHORT ).show();
                break;
        }
    }

    private void startNetworkComplianceDemo()
    {
        Toast.makeText( get(), "Preparing network compliance demo", Toast.LENGTH_SHORT ).show();
        Log.i( TAG, "Starting network compliance demo" );

        //1. Start DemoLocationProvider with gpsdata for this demo
        //2. Center map view on the current position
        //3. Show map activity

        String country = MapManager.nativeFindCountry( TRANSTECH_OFFICE.getLatitude(), TRANSTECH_OFFICE.getLongitude() );
        if ( TextUtils.isEmpty( country ))
            return;

        Log.i( TAG, "Found required country " + country);
        GraphHopperRouter truckRouter = ComplianceController.get().getRouter( get(), Framework.ROUTER_TYPE_TRUCK );
        if( !truckRouter.setSelectedProfile( NETWORK_BDOUBLE ) )
            Log.i( TAG, "Failed to set selected GH profile to '" + NETWORK_BDOUBLE + "'");

        RoutingController.get().setRouterType( Framework.ROUTER_TYPE_TRUCK );
        ComplianceController.get().setCurrentRouterType( Framework.ROUTER_TYPE_TRUCK );

        //set our start position
        LocationHelper.INSTANCE.onLocationUpdated( TRANSTECH_OFFICE );

        DemoLocationProvider.GPS_DATA_SOURCE = "/sdcard/MapsWithMe/Demo0.txt";
        Log.i( TAG, "Set GPS data source to " + DemoLocationProvider.GPS_DATA_SOURCE );
        LocationHelper.INSTANCE.setUseDemoGPS( true );
        Log.i( TAG, "Started demo location provider" );

        get().startActivity( new Intent( get(), MwmActivity.class ) );

        Log.i( TAG, "Started demo 1" );
        get().finish();
    }

    private void startRouteComplianceDemo()
    {
        Toast.makeText( get(), "Preparing route compliance demo", Toast.LENGTH_SHORT ).show();
        Log.i( TAG, "Starting network compliance demo" );

        String country = MapManager.nativeFindCountry( TRANSTECH_OFFICE.getLatitude(), TRANSTECH_OFFICE.getLongitude() );
        if ( TextUtils.isEmpty( country ))
            return;

        Log.i( TAG, "Found required country " + country);
        GraphHopperRouter carRouter = ComplianceController.get().getRouter( get(), Framework.ROUTER_TYPE_TRUCK );
        GraphHopperRouter truckRouter = ComplianceController.get().getRouter( get(), Framework.ROUTER_TYPE_TRUCK );
        if( !truckRouter.setSelectedProfile( NETWORK_BDOUBLE ) )
            Log.i( TAG, "Failed to set selected GH profile to '" + NETWORK_BDOUBLE + "'");


        RoutingController.get().setRouterType( Framework.ROUTER_TYPE_TRUCK );
        ComplianceController.get().setCurrentRouterType( Framework.ROUTER_TYPE_TRUCK );

        //turn off demo GPS feed
        LocationHelper.INSTANCE.setUseDemoGPS( false );

        //set our start position
        LocationHelper.INSTANCE.onLocationUpdated( DEMO2_START );
        Log.i( TAG, "Set start position to " + DEMO2_START.getLatitude() + ", " + DEMO2_START.getLongitude() );
        //create a dummy bookmarked end point as our end position
        MapObject endPoint = new MapObject(MapObject.API_POINT, "Spirit of Tasmania", null, null, -37.84241170038242, 144.93850708007812, null );
        Log.i( TAG, "Set end position to " + endPoint.getLat() + ", " + endPoint.getLon() );

        RoutingController.get().attach( mwmActivity );
        mwmActivity.startLocationToPoint( Statistics.EventName.MENU_P2P, AlohaHelper.MENU_POINT2POINT, endPoint );
        Log.i( TAG, "Prepared route... " );

        get().startActivity( new Intent( get(), MwmActivity.class ) );
        Log.i( TAG, "Started demo 2" );
        get().finish();
    }

    private void startReRouteDemo()
    {
        Toast.makeText( get(), "Preparing compliant re-routing demo", Toast.LENGTH_SHORT ).show();

        Log.i( TAG, "Starting rerouting demo" );

        String country = MapManager.nativeFindCountry( TRANSTECH_OFFICE.getLatitude(), TRANSTECH_OFFICE.getLongitude() );
        if ( TextUtils.isEmpty( country ))
            return;

        Log.i( TAG, "Found required country " + country);
        GraphHopperRouter carRouter = ComplianceController.get().getRouter( get(), Framework.ROUTER_TYPE_TRUCK );
        GraphHopperRouter truckRouter = ComplianceController.get().getRouter( get(), Framework.ROUTER_TYPE_TRUCK );
        if( !truckRouter.setSelectedProfile( NETWORK_BDOUBLE ) )
            Log.i( TAG, "Failed to set selected GH profile to '" + NETWORK_BDOUBLE + "'");

        RoutingController.get().setRouterType( Framework.ROUTER_TYPE_TRUCK );
        ComplianceController.get().setCurrentRouterType( Framework.ROUTER_TYPE_TRUCK );

        //set our start position
        LocationHelper.INSTANCE.onLocationUpdated( DEMO3_START );

        Log.i( TAG, "Set start position to " + DEMO3_START.getLatitude() + ", " + DEMO3_START.getLongitude() );
        //create a dummy bookmarked end point as our end position
        MapObject endPoint = new MapObject(MapObject.API_POINT, "Mitcham Hotel", null, null, -37.816931, 145.193893, null );
        Log.i( TAG, "Set end position to " + endPoint.getLat() + ", " + endPoint.getLon() );

        RoutingController.get().attach( mwmActivity );
        mwmActivity.startLocationToPoint( Statistics.EventName.MENU_P2P, AlohaHelper.MENU_POINT2POINT, endPoint );
        Log.i( TAG, "Prepared route... " );

        DemoLocationProvider.GPS_DATA_SOURCE = "/sdcard/MapsWithMe/Demo2.txt";
        DemoLocationProvider.LOOP = false; //just once around the block
        Log.i( TAG, "Set GPS data source to " + DemoLocationProvider.GPS_DATA_SOURCE );
        LocationHelper.INSTANCE.setUseDemoGPS( true );
        Log.i( TAG, "Started demo location provider" );

        get().startActivity( new Intent( get(), MwmActivity.class ) );
        Log.i( TAG, "Started demo 3" );
        get().finish();
    }
}

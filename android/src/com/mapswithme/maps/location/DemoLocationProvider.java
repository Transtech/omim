package com.mapswithme.maps.location;

import android.location.Location;
import android.os.SystemClock;
import android.util.Log;
import com.mapswithme.util.concurrency.UiThread;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class DemoLocationProvider
 * <p/>
 * Created by agough on 2/08/16 4:47 PM
 */
public class DemoLocationProvider extends BaseLocationProvider
{
    private static final String TAG = "DemoLocationProvider";
    public static String GPS_DATA_SOURCE = "/sdcard/MapsWithMe/GpsData.txt";
    public static boolean LOOP = false;
    public static boolean DEMO_MODE = false;

    /**
     * Conversion factor converting MPS to KNOTS.
     */
    public static final float MPS_TO_KNOTS = 1.9438444924406f;

    /**
     * Conversion factor converting MPS to KM/H.
     */
    public static final float MPS_TO_KMH = 3.6f;

    /**
     * Conversion factor converting KM/H to MPS.
     */
    public static final float KMH_TO_MPS = 0.277777778f;

    /**
     * Conversion factor converting Knots to Meter per second
     */
    public final static float KNOTS_TO_MPS = 0.514444F;

    /**
     * GPS HDOP Valid - GPS signal is considered to be valid if the HDOP is
     * equal to or less than this value.
     */
    public final static float MAX_VALID_HDOP = 4.0f;

    /**
     * GPS HDOP invalid number.
     */
    public final static float HDOP_INVALID = 99.0f;

    /**
     * GPS Number Of Satellites Valid - GPS signal is considered to be valid if
     * the number of satellites is equal to or greater than this value.
     */
    public final static int MIN_VALID_SATELLITES = 3;

    /**
     * GPS Number Of Satellites Invalid - the number of satellites we consider as
     * invalid, i.e. for initializing variables etc.
     */
    public final static int NUM_SATELLITES_INVALID = 0;


    private List<Location> fakeData = null;
    private FakerThread fakeThread;
    private static AtomicInteger thrCount = new AtomicInteger();

    DemoLocationProvider()
    {
        this(GPS_DATA_SOURCE);
    }

    DemoLocationProvider( String fileName )
    {
        setDataSource( fileName );
    }

    private DemoLocationProvider( int ignored )
    {
    }

    public static void setLocation( double lat, double lng )
    {
        MockLocation fakeLoc = new MockLocation( "DemoLocationProvider" );
        fakeLoc.setLatitude( lat );
        fakeLoc.setLongitude( lng );
        fakeLoc.setTime( System.currentTimeMillis() );
        fakeLoc.setSpeed( 0.0f );
        fakeLoc.setBearing( 0.0f );
        fakeLoc.setAltitude( 0.0f );

        new DemoLocationProvider( 0 ).sendUpdatedLocation( fakeLoc );
    }

    @Override
    protected boolean start()
    {
        Log.i(TAG, "DemoGPS thread starting...");
        if( fakeThread != null && fakeThread.keepSending == true )
        {
            Log.i(TAG, "DemoGPS thread already started - leave early");
            return true;
        }

        LocationHelper.INSTANCE.startSensors();

        fakeThread = new FakerThread();
        fakeThread.start();
        return true;
    }

    @Override
    protected void stop()
    {
        if( fakeThread != null )
        {
            Log.i(TAG, "DemoGPS thread stopping...");
            fakeThread.keepSending = false;
            fakeThread.interrupt();
            try
            {
                fakeThread.join();
            }
            catch( InterruptedException ie ) {}
        }

        fakeThread = null;
    }

    /**
     * this method reads a txt or json file and generates a list of fake GPS data based on the content of the file, txt files are meant to contain the IVU's blackbox data,
     * whereas json files are meant to be used for end-to-end testing, so for instance if you need to replicate an speeding event you can simply copy the couchDB's record into
     * a file (start record followed by update record(s) and the end record)
     *
     * @param source
     */
    private void setDataSource( String source )
    {
        final String fileName = (source == null ? GPS_DATA_SOURCE : source);

        fakeData = new ArrayList<Location>();
        Log.d( TAG, "Reading the file  ->" + fileName );

        BufferedReader br = null;
        try
        {
            if( fileName.endsWith( ".txt" ) )
            {
                br = new BufferedReader( new FileReader( fileName ) );
                String line;
                while( (line = br.readLine()) != null )
                {
                    String data[] = line.split( "," );
                    MockLocation fakeLoc = null;

                    // -1 Signifies an invalid speed and normally when speed is invalid Location object is null, so let's do the same here
                    fakeLoc = new MockLocation( "DemoLocationProvider" );
                    if( !data[ 5 ].equals( "-1" ) )
                    {
                        fakeLoc.setLatitude( toDegrees( data[ 0 ] ) );
                        fakeLoc.setLongitude( toDegrees( data[ 1 ] ) );
                        fakeLoc.setTime( Long.parseLong( data[ 3 ] ) * 1000 );
                        fakeLoc.setSpeed( Float.parseFloat( data[ 5 ] ) * KNOTS_TO_MPS );
                        fakeLoc.setBearing( Float.parseFloat( data[ 6 ] ) );
                        fakeLoc.setAltitude( Double.parseDouble( data[ 7 ] ) );
                        float hdop = Float.parseFloat( data[ 8 ] );
                        int numSats = Integer.parseInt( data[ 9 ] );
                        fakeLoc.setAccuracy( numSats * 10 + hdop );
                    }
                    fakeData.add( fakeLoc );
                }

                br.close();
            }
            else if( fileName.endsWith( ".json" ) )
            {
                StringBuilder sb = new StringBuilder();
                br = new BufferedReader( new FileReader( fileName ) );
                String line;
                while( (line = br.readLine()) != null )
                    sb.append( line ).append( '\n' );

                JSONArray events = new JSONObject( sb.toString() ).getJSONArray( "GPS" );
                for( int i = 0; i < events.length(); i++ )
                {
                    JSONObject event = events.getJSONObject( i );
                    MockLocation fakeLoc = null;

                    // -1 Signifies an invalid speed and normally when speed is invalid Location object is null, so let's do the same here
                    fakeLoc = new MockLocation( "DemoLocationProvider" );
                    if( event.getInt( "Spd" ) != -1.0 )
                    {
                        fakeLoc.setLatitude( event.getDouble( "Lat" ) );
                        fakeLoc.setLongitude( event.getDouble( "Lng" ) );
                        fakeLoc.setTime( System.currentTimeMillis() );
                        fakeLoc.setSpeed( (float) (event.getDouble( "Spd" ) * KMH_TO_MPS) );
                        fakeLoc.setBearing( event.getInt( "Dir" ) );
                        fakeLoc.setAltitude( event.getDouble( "Alt" ) );

                        double hdop = event.getDouble( "HDOP");
                        int numSats = event.getInt( "NSat" );
                        fakeLoc.setAccuracy( (float) (numSats * 10 + hdop) );
                    }
                    fakeData.add( fakeLoc );
                }
            }

        }
        catch( IOException e )
        {
            Log.e( TAG, Log.getStackTraceString( e ) );
        }
        catch( JSONException e )
        {
            Log.e( TAG, Log.getStackTraceString( e ) );
        }

        if( fakeData != null && fakeData.size() > 0 )
            sendUpdatedLocation( fakeData.get( 0 ) );
    }

    /**
     * Convert Lat/Long from NMEA format to the original decimal degrees
     *
     * @param NMEA
     * @return
     */
    private double toDegrees( String NMEA )
    {
        final double signum = Math.signum( Double.parseDouble( NMEA ) );
        final String temp = NMEA.substring( NMEA.indexOf( "." ) - 2 );

        double t = Double.parseDouble( temp );
        int f = Integer.parseInt( NMEA.substring( 0, NMEA.length() - temp.length() ) );

        return f + (signum * (t / 60));
    }

    private void sendUpdatedLocation( Location location )
    {
        try
        {
            final Location loc = new Location( location );
            loc.setTime( new Date().getTime() );
            loc.setElapsedRealtimeNanos( SystemClock.elapsedRealtimeNanos() );

            UiThread.runLater( new Runnable()
            {
                @Override
                public void run()
                {
                    LocationHelper.INSTANCE.onLocationUpdated( loc );
                    LocationHelper.INSTANCE.notifyLocationUpdated();
                }
            } );
        }
        catch( Exception e )
        {
            Log.e( TAG, "Error creating a fake location", e );
        }
    }
    private class FakerThread extends Thread
    {
        volatile boolean keepSending = true;
        volatile int currentIndex = 0;

        FakerThread()
        {
            Log.i( TAG, "Creating new fake GPS thread" );
        }

        public void run()
        {
            int num = thrCount.incrementAndGet();
            Log.i( TAG, "Creating new fake GPS thread (" + num + ")" );
            Location prevLoc = null;

            while( keepSending )
            {
                if( fakeData == null || fakeData.size() == 0 )
                {
                    Log.w( TAG, "No fake data available - aborting fake GPS thread" );
                    keepSending = false;
                    return;
                }

                currentIndex = ++currentIndex % fakeData.size(); //this keeps going around until the thread is stopped
                if( !LOOP && currentIndex == fakeData.size() - 1)
                    break;

                Location mockLoc = currentIndex < fakeData.size() ? fakeData.get( currentIndex ) : null;
                if( mockLoc != null )
                    sendUpdatedLocation( mockLoc );

                long delay = 250;
                if( mockLoc != null && prevLoc != null )
                    delay = Math.min(Math.max( Math.abs( mockLoc.getTime() - prevLoc.getTime() ) / 4, 250), 1000);

                try
                {
                    Thread.sleep( delay );
                }
                catch( InterruptedException ie )
                {
                    Log.w( TAG, "Fake GPS thread sleep interrupted - aborting fake GPS thread" );
                    keepSending = false;
                }
                prevLoc = mockLoc;
            }
            num = thrCount.decrementAndGet();
            Log.i(TAG, "DemoGPS thread stopped (" + num + ")");
        }
    }

}

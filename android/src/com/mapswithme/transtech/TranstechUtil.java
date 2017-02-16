package com.mapswithme.transtech;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class TranstechUtil
 * <p/>
 * Created by agough on 8/02/17 10:15 AM
 */
public class TranstechUtil
{
    private static final String LOG_TAG = "TranstechUtil";

    public static void publish(Context ctx, String routingKey, int priority, JSONObject payload)
    {
        try
        {
            // / infer & add the record type from the routing key
            if (!payload.has("RecordType"))
                payload.put( "RecordType", routingKey.substring( routingKey.lastIndexOf( "." ) + 1 ).toUpperCase() );

            Intent intent = new Intent( Const.CREATE_COMMS_RECORD);
            intent.putExtra(Const.EXTRA_COMMS_EVENT_ROUTE, routingKey);
            intent.putExtra(Const.EXTRA_COMMS_EVENT_CONTENT, payload.toString());
            intent.putExtra(Const.EXTRA_COMMS_EVENT_PRIORITY, priority);

            ctx.startService(intent);
        }
        catch (JSONException e)
        {
            Log.e( LOG_TAG, Log.getStackTraceString( e ) );
        }
    }

    /**
     * Calculate the distance between 2 lat/lon pairs using the haversine equation
     * <p/>
     * http://www.movable-type.co.uk/scripts/latlong.html
     *
     * @param sLat
     * @param sLon
     * @param fLat
     * @param fLon
     * @return distance in meters between the 2 points
     */
    private static final double R = 6371e3;

    public static double haversineDistance( double lat1, double lon1, double lat2, double lon2 )
    {
        double r1 = toRad( lat1 );
        double r2 = toRad( lat2 );

        double lat3 = toRad( lat2 - lat1 );
        double lon3 = toRad( lon2 - lon1 );

        double a = Math.sin( lat3 / 2 ) * Math.sin( lat3 / 2 ) +
                Math.cos( r1 ) * Math.cos( r2 ) *
                        Math.sin( lon3 / 2 ) * Math.sin( lon3 / 2 );
        double c = 2 * Math.atan2( Math.sqrt( a ), Math.sqrt( 1 - a ) );

        return R * c;
    }

    private static double toRad( double deg )
    {
        return deg * Math.PI / 180.0;
    }

    private static double toDeg( double rad )
    {
        return rad * 180.0 / Math.PI;
    }

}

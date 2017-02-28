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

            Intent intent = new Intent( TranstechConstants.CREATE_COMMS_RECORD);
            intent.putExtra( TranstechConstants.EXTRA_COMMS_EVENT_ROUTE, routingKey);
            intent.putExtra( TranstechConstants.EXTRA_COMMS_EVENT_CONTENT, payload.toString());
            intent.putExtra( TranstechConstants.EXTRA_COMMS_EVENT_PRIORITY, priority);

            ctx.startService(intent);
        }
        catch (JSONException e)
        {
            Log.e( LOG_TAG, Log.getStackTraceString( e ) );
        }
    }

}

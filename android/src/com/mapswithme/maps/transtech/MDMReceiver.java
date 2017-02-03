package com.mapswithme.maps.transtech;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Class MDMReceiver
 * <p/>
 * Created by agough on 30/01/17 1:22 PM
 */
public class MDMReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive( Context context, Intent intent )
    {
        Log.i( "MDMReceiver(COMMON)", "Received MDM broadcast message" );
        //we only process AMQP_MESSAGE actions of type 'transtech.command'
        if( intent.getAction().equalsIgnoreCase( "transtech.AF.Android.Comms.action.AMQP_MESSAGE" ) &&
                intent.hasCategory( "transtech.AF.Android.Comms.category.transtech.command" ) )
        {
            //we have an MDM command - lets process it
            MDMHandler handler = new MDMHandler( context, intent );
            handler.process(); // all done - processing is done in a background task and the response sent to TranstechCommon
        }
    }
}

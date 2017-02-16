package com.mapswithme.transtech;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import com.mapswithme.transtech.Const;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class MDMHandler
 * <p/>
 * Created by agough on 30/01/17 1:23 PM
 */
public class MDMHandler
{
    private static final String SERVICE = "SMARTNAV2";
    private static final String LOG_TAG = "MDMHandler(" + SERVICE + ")";

    private final Context context;
    private final Intent intent;

    public MDMHandler(Context context, Intent intent)
    {
        this.context = context;
        this.intent = intent;
    }

    public void process()
    {
        MDMOfflineTask mdmTask = new MDMOfflineTask();
        mdmTask.execute(intent);
    }

    public class MDMOfflineTask extends AsyncTask<Intent, Void, Void>
    {
        @Override
        protected Void doInBackground( Intent... params )
        {
            Intent intent = params[0];

            String jsonHeaders = intent.getStringExtra("CommsEventHeaders");
            String jsonBody = intent.getStringExtra("CommsEventContent");
            if (jsonHeaders == null || jsonBody == null)
            {
                Log.w( LOG_TAG, "Received MDM broadcast without either the headers or body or both" );
                return null;
            }

            JSONObject headers = null, body = null;
            String replyTo = null, command = null, service = null;

            try
            {
                headers = new JSONObject(jsonHeaders);
                body = new JSONObject(jsonBody);

                replyTo = body.getString("ReplyTo");
                command = body.getString("RecordType");
                JSONObject cmdData = body.optJSONObject( "CmdData" );
                if(cmdData != null)
                    service = cmdData.optString( "Service" );
            }
            catch (Exception e)
            {
                Log.e(LOG_TAG, "Received MDM broadcast without invalid JSON objects as headers or body or both");
                return null;
            }

            if( headers == null || body == null || replyTo == null || command == null )
            {
                Log.e(LOG_TAG, "Received MDM broadcast with invalid structure");
                return null;
            }

            //we only process application level commands in this receiver ONLY if the command
            // is not a file related, then we skip this check as the "service" data is missing
            // in file related commands from MDM
            if ("file.get".equals(command)) {

            } else if( service == null || !"SMARTNAV2".equalsIgnoreCase( service ) ) {
                Log.w(LOG_TAG, "Received MDM broadcast for a different service: this is 'SMARTNAV2' and the request is for '" + service + "' - ignoring");
                return null;
            }

            //dispatch the command
            String responseBody = null;
            try
            {
                if( "application.ping".equals(command) )
                    responseBody = processPing( headers, body );
                else if( "application.exec".equals(command) )
                    responseBody = processExec( headers, body );
                else if( "application.log".equals(command) )
                    responseBody = processLog( headers, body );
                else if( "application.db".equals(command) )
                    responseBody = processDatabase( headers, body );
                else if( "file.get".equals(command) )
                    responseBody = processFile( headers, body );
            }
            catch(Exception e)
            {
                Log.e(LOG_TAG, "Failed to process MDM message", e);
                responseBody = exceptionResponse(headers, body, e);
            }

            if( responseBody == null )
            {
                Log.w(LOG_TAG, "Failed to process command '" + command + "' into a coherent response");
                return null;
            }

            try
            {
                //package the response back into a 'transtech.response' for publishing
                JSONObject respHeaders = new JSONObject();
                respHeaders.put( Const.AMQP_HEADER_MESSAGE_TYPE, Const.AMQP_MSG_TYPE_MDM_RESPONSE);
                respHeaders.put(Const.AMQP_HEADER_MESSAGE_VERSION, "1.0.0" );
                respHeaders.put(Const.AMQP_HEADER_MESSAGE_FORMAT, "application/json");
                respHeaders.put(Const.AMQP_HEADER_CORRELATION_ID, headers.getString(Const.AMQP_HEADER_MESSAGE_ID));
                //any missing headers (device id/company id/message id etc will be filled in during publishing...)

                Log.d(LOG_TAG, "Starting Comms service with Intent " + Const.CREATE_COMMS_RECORD + ", route " + replyTo);
                intent = new Intent(Const.CREATE_COMMS_RECORD);
                intent.putExtra(Const.EXTRA_COMMS_EVENT_ROUTE, replyTo);
                intent.putExtra(Const.EXTRA_COMMS_EVENT_HEADERS, respHeaders.toString());
                intent.putExtra(Const.EXTRA_COMMS_EVENT_CONTENT, responseBody);
                intent.putExtra(Const.EXTRA_COMMS_EVENT_SKIP_ROUTING_KEY_SUBSTITUTION, true);

                context.startService(intent);
            }
            catch(Exception e)
            {
                Log.e(LOG_TAG, "Failed to publish MDM response", e);
            }
            return null;
        }
    }

    // --------------------------------------------------------------------- //

    public SimpleDateFormat getDateTimeFormatter() {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat;
    }

    // --------------------------------------------------------------------- //

    private String processPing( JSONObject headers, JSONObject body ) throws Exception
    {
        JSONObject resp = new JSONObject();
        resp.put("RecordType", body.getString("RecordType"));
        resp.put("DeviceId", headers.getString(Const.AMQP_HEADER_DEVICE_ID));
        resp.put( "EventDateTime", getDateTimeFormatter().format( new Date()));

        JSONObject rspData = new JSONObject();
        rspData.put("Status", "OK");

        JSONObject cmdData = body.getJSONObject("CmdData");
        rspData.put("Service", cmdData.getString( "Service" ));
        rspData.put("Info", "application.pong");
        resp.put("RspData", rspData);
        return resp.toString();
    }

    // --------------------------------------------------------------------- //

    private String processFile( JSONObject headers, JSONObject body ) throws Exception
    {

        JSONObject resp = new JSONObject();
        resp.put("RecordType", body.getString("RecordType"));
        resp.put("DeviceId", headers.getString(Const.AMQP_HEADER_DEVICE_ID));
        resp.put("EventDateTime", getDateTimeFormatter().format(new Date()));

        JSONObject rspData = new JSONObject();
        rspData.put("Status", "OK");

        JSONObject cmdData = body.getJSONObject("CmdData");
        String filePath = cmdData.getString("Source");

        rspData.put("Service", SERVICE);
        rspData.put("Algo", "none");
        rspData.put("Content", Base64.encodeToString( getStringFromFile( filePath ).getBytes(), Base64.NO_WRAP ));
        resp.put("RspData", rspData);
        return resp.toString();
    }

    public String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    // --------------------------------------------------------------------- //


    private String processExec( JSONObject headers, JSONObject body ) throws Exception
    {
        String eventDate = getDateTimeFormatter().format(new Date());
        JSONObject resp = new JSONObject();
        resp.put("RecordType", body.getString("RecordType"));
        resp.put("DeviceId", headers.getString(Const.AMQP_HEADER_DEVICE_ID));
        resp.put("EventDateTime", eventDate);

        JSONObject rspData = new JSONObject();
        rspData.put("Status", "OK");

        JSONObject cmdData = body.getJSONObject("CmdData");
        rspData.put("Service", cmdData.getString( "Service" ));

        String cmd = cmdData.getString("Program");
        JSONArray args = cmdData.optJSONArray( "Arguments" );
        List<String> fullCmd = new ArrayList<String>();
        fullCmd.add( cmd );
        if( args != null && args.length() > 0 )
        {
            for( int i = 0; i < args.length(); ++i )
                fullCmd.add( args.getString( i ) );
        }

        Process p = new ProcessBuilder()
                .command( fullCmd )
                .redirectErrorStream( true )
                .start();

        // Attempt to write a file to a root-only
        Log.i( LOG_TAG, "Executing " + cmd );

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
        JSONArray content = new JSONArray();
        String line = null;
        while ((line = r.readLine()) != null)
        {
            content.put( line );
        }

        rspData.put("Content", content);
        resp.put("RspData", rspData);
        return resp.toString();
    }

    // --------------------------------------------------------------------- //

    private String processLog( JSONObject headers, JSONObject body ) throws Exception
    {
        JSONObject resp = new JSONObject();
        resp.put("RecordType", body.getString("RecordType"));
        resp.put("DeviceId", headers.getString(Const.AMQP_HEADER_DEVICE_ID));
        resp.put("EventDateTime", getDateTimeFormatter().format(new Date()));

        JSONObject rspData = new JSONObject();
        rspData.put("Status", "ERR");
//        rspData.put("Service", body.getString( "Service" ));
        rspData.put("Severity", "INFO");
        rspData.put("Info", "'application.log' command not yet implemented");

        resp.put("RspData", rspData);
        return resp.toString();
    }

    // --------------------------------------------------------------------- //

    private String processDatabase( JSONObject headers, JSONObject body ) throws Exception
    {
        String eventDate = getDateTimeFormatter().format(new Date());
        JSONObject resp = new JSONObject();
        resp.put("RecordType", body.getString("RecordType"));
        resp.put("DeviceId", headers.getString(Const.AMQP_HEADER_DEVICE_ID));
        resp.put("EventDateTime", eventDate);

        JSONObject rspData = new JSONObject();
        rspData.put("Status", "OK");

        JSONObject cmdData = body.getJSONObject( "CmdData" );
        rspData.put("Service", cmdData.getString( "Service" ));

        String db = cmdData.optString( "Database" );
        String sqlCmd = cmdData.getString( "Command" );
        String firstWord = sqlCmd;
        if( sqlCmd.indexOf( ' ' ) != -1 )
            firstWord = sqlCmd.substring( 0, sqlCmd.indexOf( ' ' ) );

        SQLiteDatabase sqlDb = null;
        //execute 'sqlCmd' on database 'db' and return packaged results...
        JSONObject content = new JSONObject();

        try
        {
            content.put( "Command", sqlCmd );
            if( "list".equalsIgnoreCase( firstWord.toLowerCase() ) )
            {
                Log.i( LOG_TAG, "Listing databases" );

                //we want to return a list of available databases
                JSONArray arr = new JSONArray();
                for( String s : context.databaseList() )
                    arr.put( s );

                content.put( "Data", arr );
            }
            else if( "select".equalsIgnoreCase( firstWord.toLowerCase() ) )
            {
                Log.i( LOG_TAG, "Executing '" + sqlCmd + "' on database '" + db + "'" );

                sqlDb = context.openOrCreateDatabase( db, 0, null );
                Cursor cursor = sqlDb.rawQuery( sqlCmd, new String[]{ } );

                //extract some metadata
                JSONArray columns = new JSONArray();
                int colCount = cursor.getColumnCount();
                for( int i = 0; i < colCount; ++i )
                {
                    JSONObject column = new JSONObject();
                    column.put( "Column", i );
                    column.put( "Name", cursor.getColumnName( i ) );
                    //                column.put("Type", fieldType(cursor.getType( i )));
                    columns.put( column );
                }
                content.put( "Columns", columns );

                JSONArray data = new JSONArray();
                while( cursor.moveToNext() )
                {
                    ContentValues cv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues( cursor, cv );

                    JSONObject obj = new JSONObject();
                    for( Map.Entry<String, Object> entry : cv.valueSet() )
                        obj.put( entry.getKey(), entry.getValue() );

                    data.put( obj );
                }
                content.put( "Data", data );
            }
            else if( "update".equalsIgnoreCase( firstWord.toLowerCase() ) ||
                    "delete".equalsIgnoreCase( firstWord.toLowerCase() ) ||
                    "insert".equalsIgnoreCase( firstWord.toLowerCase() ) )
            {
                Log.i( LOG_TAG, "Executing '" + sqlCmd + "' on database '" + db + "'" );

                sqlDb = context.openOrCreateDatabase( db, 0, null );
                sqlDb.execSQL( sqlCmd );
            }
        }
        finally
        {
            if( sqlDb != null )
            {
                try
                {
                    sqlDb.close();
                }
                catch( Exception e ) {}
            }
        }
        rspData.put("Content", content);
        resp.put("RspData", rspData);
        return resp.toString();
    }

    // --------------------------------------------------------------------- //

    private String exceptionResponse(JSONObject headers, JSONObject body, Exception e)
    {
        try
        {
            JSONObject resp = new JSONObject();
            resp.put("RecordType", body.getString("RecordType"));
            resp.put("DeviceId", headers.getString(Const.AMQP_HEADER_DEVICE_ID));
            resp.put("EventDateTime", getDateTimeFormatter().format(new Date()));

            JSONObject rspData = new JSONObject();
            rspData.put("Service", SERVICE);
            rspData.put("Status", "ERR");
            rspData.put("Severity", "ERROR");
            if( e.getCause() != null)
                rspData.put("Info", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            else
                rspData.put("Info", e.getClass().getName() + ": " + e.getMessage());

            JSONArray st = new JSONArray( Arrays.asList( e.getStackTrace() ));
            rspData.put("StackTrace", st);
            resp.put("RspData", rspData);
            return resp.toString();
        }
        catch(JSONException e1)
        {
            Log.w(LOG_TAG, "Got an exception packaging an exception! - better quit while we're ahead",e1);
        }
        return null;
    }
}

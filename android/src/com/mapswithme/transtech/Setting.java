package com.mapswithme.transtech;

/**
 * Class Settings
 * <p/>
 * Created by agough on 4/08/16 9:24 AM
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import org.json.JSONObject;

public class Setting implements Parcelable
{
    private final static String LOG_TAG = "Setting";

    public static enum Environment
    {
        ALL,
        PRODUCTION,
        DEMO,
        STAGING,
        TEST
    }

    public static enum Scope
    {
        COMMON,
        COMPMGMT,
        TRACKING,
        DURESS,
        SPEED,
        SPEEDASSIST,
        LOGIN,
        QUICKJOBS,
        MESSAGING,
        GEOFENCE,
        ROUTECOMPLIANCE,
        JOURNEYPLANNER,
        VPM,
        TPMS,
        SENTINEL3,
        SENTINEL4,
        MASS,
        PRETRIP,
        EASYDOCS,
        IAP,
        OBM,
        SMARTJOBS,
        LOGBOOK,
        SMARTNAV,
        BETA,
        CRANENAV,
        SPEEDSIREN,
        SENTINEL3NAVMAN,
        SENTINEL3KIOSK,
        MNAVMSG,
        MNAVLOGIN,
        MNAVEMS,
        MNAVSTATS,
        TRAILERID,
        SMARTNAV2
    }

    public static enum Source
    {
        USER,
        DEVICE,
    }

    public static enum Origin
    {
        LOCAL,
        REMOTE
    }

    /**
     * Authority used to identify this provider to Android
     */
    public static final String AUTHORITY = "transtech.af.android.common.settingsProvider";
    /**
     * URI used to represent the list of messages.
     * Individual messages will be represented with this plus /ID on the end
     */
    public static final Uri CONTENT_URI = Uri.parse( "content://" + AUTHORITY + "/settings" );

    /**
     * Column IDs
     */
    public static final String SETTING_ID = "_ID";
    public static final String ENV = "ENV";
    public static final String SCOPE = "SCOPE";
    public static final String KEY = "KEY";
    public static final String VALUE = "VALUE";
    public static final String SOURCE = "SOURCE";

    private int settingId;
    private String env;
    private String scope;
    private String key;
    private String value;
    private String source;

    public Setting()
    {
    }

    public Setting( Cursor c )
    {
        settingId = c.getInt( c.getColumnIndex( SETTING_ID ) );
        env = c.getString( c.getColumnIndex( ENV ) );
        scope = c.getString( c.getColumnIndex( SCOPE ) );
        key = c.getString( c.getColumnIndex( KEY ) );
        value = c.getString( c.getColumnIndex( VALUE ) );
        source = c.getString( c.getColumnIndex( SOURCE ) );
    }

    public Setting( ContentValues v )
    {
        settingId = v.getAsInteger( SETTING_ID );
        env = v.getAsString( ENV );
        scope = v.getAsString( SCOPE );
        key = v.getAsString( KEY );
        value = v.getAsString( VALUE );
        source = v.getAsString( SOURCE );
    }

    public Setting( Parcel p )
    {
        env = p.readString();
        scope = p.readString();
        key = p.readString();
        value = p.readString();
        source = p.readString();
    }

    public Setting( Environment env, Scope scope, String key, String value, String source )
    {
        this.env = env.toString();
        this.scope = scope.toString();
        this.key = key;
        this.value = value;
        this.source = source;
    }

    public Setting( Environment env, Scope scope, String key, boolean value, String source )
    {
        this.env = env.toString();
        this.scope = scope.toString();
        this.key = key;
        this.value = String.valueOf( value );
        this.source = source;

    }

    public Setting( Environment env, Scope scope, String key, int value, String source )
    {
        this.env = env.toString();
        this.scope = scope.toString();
        this.key = key;
        this.value = String.valueOf( value );
        this.source = source;
    }

    public Setting( Environment env, Scope scope, String key, float value, String source )
    {
        this.env = env.toString();
        this.scope = scope.toString();
        this.key = key;
        this.value = String.valueOf( value );
        this.source = source;
    }

    public Setting( Environment env, Scope scope, String key, long value, String source )
    {
        this.env = env.toString();
        this.scope = scope.toString();
        this.key = key;
        this.value = String.valueOf( value );
        this.source = source;
    }

    public Setting( Environment env, Scope scope, String key, double value, String source )
    {
        this.env = env.toString();
        this.scope = scope.toString();
        this.key = key;
        this.value = String.valueOf( value );
        this.source = source;
    }

    public Setting( Environment env, Scope scope, String key, JSONObject value, String source )
    {
        this.env = env.toString();
        this.scope = scope.toString();
        this.key = key;
        this.value = value.toString();
        this.source = source;
    }


    public ContentValues getContentValues()
    {
        ContentValues v = new ContentValues();

        v.put( ENV, env );
        v.put( SCOPE, scope );
        v.put( KEY, key );
        v.put( VALUE, value );
        v.put( SOURCE, source );

        return v;
    }

    public int getId()
    {
        return settingId;
    }

    public void setId( int settingId )
    {
        this.settingId = settingId;
    }

    public String getEnv()
    {
        return env;
    }

    public void setEnv( String env )
    {
        this.env = env;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource( String source )
    {
        this.source = source;
    }

    public Uri getURI()
    {
        return Uri.withAppendedPath( CONTENT_URI, Long.toString( settingId ) );
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Setting> CREATOR = new Parcelable.Creator<Setting>()
    {
        public Setting createFromParcel( Parcel in )
        {
            return new Setting( in );
        }

        public Setting[] newArray( int size )
        {
            return new Setting[ size ];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel( Parcel out, int flags )
    {
        out.writeString( this.env );
        out.writeString( this.scope );
        out.writeString( this.key );
        out.writeString( this.value );
        out.writeString( this.source );
    }

    public static String getString( Context context, Environment env, Scope scope, String key, String defaultValue )
    {
        String value = getSetting( context, env, scope, key );
        return value == null ? defaultValue : value;
    }

    public static boolean getBoolean( Context context, Environment env, Scope scope, String key, boolean defaultValue )
    {
        String valueStr = getSetting( context, env, scope, key );
        try
        {
            return valueStr == null ? defaultValue : (valueStr.equalsIgnoreCase( "true" ) ? true : false);
        }
        catch( Throwable exp )
        {
            Log.w( LOG_TAG, Log.getStackTraceString( exp ) );
        }

        return defaultValue;
    }

    public static int getInt( Context context, Environment env, Scope scope, String key, int defaultValue )
    {
        String valueStr = getSetting( context, env, scope, key );
        try
        {
            return valueStr == null ? defaultValue : Integer.parseInt( valueStr );
        }
        catch( Throwable exp )
        {
            Log.w( LOG_TAG, Log.getStackTraceString( exp ) );
        }

        return defaultValue;
    }

    public static double getDouble( Context context, Environment env, Scope scope, String key, double defaultValue )
    {
        String valueStr = getSetting( context, env, scope, key );
        try
        {
            return valueStr == null ? defaultValue : Double.parseDouble( valueStr );
        }
        catch( Throwable exp )
        {
            Log.w( LOG_TAG, Log.getStackTraceString( exp ) );
        }

        return defaultValue;
    }

    public static float getFloat( Context context, Environment env, Scope scope, String key, float defaultValue )
    {
        String valueStr = getSetting( context, env, scope, key );
        try
        {
            return valueStr == null ? defaultValue : Float.parseFloat( valueStr );
        }
        catch( Throwable exp )
        {
            Log.w( LOG_TAG, Log.getStackTraceString( exp ) );
        }

        return defaultValue;
    }

    public static long getLong( Context context, Environment env, Scope scope, String key, long defaultValue )
    {
        String valueStr = getSetting( context, env, scope, key );
        try
        {
            return valueStr == null ? defaultValue : Long.parseLong( valueStr );
        }
        catch( Throwable exp )
        {
            Log.w( LOG_TAG, Log.getStackTraceString( exp ) );
        }

        return defaultValue;
    }

    public static JSONObject getJSON( Context context, Environment env, Scope scope, String key, JSONObject defaultValue )
    {
        String valueStr = getSetting( context, env, scope, key );
        try
        {
            return valueStr == null ? defaultValue : new JSONObject( valueStr );
        }
        catch( Throwable exp )
        {
            Log.w( LOG_TAG, Log.getStackTraceString( exp ) );
        }

        return defaultValue;
    }

    public static Environment currentEnvironment(Context ctx)
    {
        return Environment.valueOf(getString( ctx,
                Environment.ALL,
                Scope.COMMON,
                SettingConstants.GLOBAL_ENVIRONMENT,
                Environment.PRODUCTION.toString()));
    }


    public static String getSetting( Context context, Environment env, Scope scope, String key )
    {
        Cursor c = context.getContentResolver().query( Setting.CONTENT_URI,
                new String[]{ Setting.VALUE },
                Setting.KEY + "=? AND " + Setting.SCOPE + "=? AND " + Setting.ENV + "=?",
                new String[]{ key, scope.toString(), env.toString() },
                null );
        if( c != null )
        {
            try
            {
                if( c.moveToFirst() )
                    return c.getString( c.getColumnIndex( Setting.VALUE ) );
            }
            finally
            {
                c.close();
            }
        }
        return null;
    }

    public static void setString(Context ctx, Environment env, Scope scope, String key, String value, Origin origin, String source)
    {
        Setting.setSettings(ctx, new Setting(env, scope, key, value, source), origin);
    }

    public static Setting getSettingRecord( Context context, Environment env, Scope scope, String key )
    {
        Cursor c = context.getContentResolver().query( Setting.CONTENT_URI,
                null,
                Setting.KEY + "=? AND " + Setting.SCOPE + "=? AND " + Setting.ENV + "=?",
                new String[]{ key, scope.toString(), env.toString() },
                null );
        if( c != null )
        {
            try
            {
                if( c.moveToFirst() )
                    return new Setting( c );
            }
            finally
            {
                c.close();
            }
        }
        return null;
    }


    public static void setSettings( Context context, Setting record, Origin origin )
    {
        if( record.getKey().equals( SettingConstants.GLOBAL_ENVIRONMENT ) )
            record.setEnv( Environment.ALL.toString() );

        final String env = record.getEnv();
        final String scope = record.getScope();
        final String key = record.getKey();
        final String value = record.getValue();
        final String source = record.getSource();

        Setting existing = getSettingRecord( context, Environment.valueOf( env ), Scope.valueOf( scope ), key );
        if( existing != null && !existing.getValue().equals( value ) )
        {
            ContentValues values = new ContentValues();
            values.put( Setting.VALUE, record.getValue() );
            context.getContentResolver().update( Setting.CONTENT_URI, values, Setting.KEY + "=? AND " + Setting.SCOPE + "=? AND " + Setting.ENV + "=?",
                    new String[]{ key, scope, env } );
        }
        else
        {
            ContentValues values = new ContentValues();
            values.put( Setting.SCOPE, scope );
            values.put( Setting.KEY, key );
            values.put( Setting.VALUE, value );
            values.put( Setting.ENV, env );
            values.put( Setting.SOURCE, source );
            context.getContentResolver().insert( Setting.CONTENT_URI, values );
        }
    }
}

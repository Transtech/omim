package com.mapswithme.maps.settings;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import com.mapswithme.maps.R;

public class VehiclePrefsFragment extends BaseXmlSettingsFragment
{
    private static final String NOT_SET = "(not set)";


    @Override
    protected int getXmlResources()
    {
        return R.xml.prefs_vehicle;
    }

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        EditTextPreference pref = (EditTextPreference) findPreference( getString( R.string.pref_vehicle_length ) );
        updateSummary( pref, pref.getText(), "m" );
        pref.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange( Preference preference, Object newValue )
            {
                if( !isFloat( newValue ) )
                    return false;

                updateSummary( preference, newValue.toString(), "m" );
                return true;
            }
        } );

        pref = (EditTextPreference) findPreference( getString( R.string.pref_vehicle_width ) );
        updateSummary( pref, pref.getText(), "m" );
        pref.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange( Preference preference, Object newValue )
            {
                if( !isFloat( newValue ) )
                    return false;

                updateSummary( preference, newValue.toString(), "m" );
                return true;
            }
        } );

        pref = (EditTextPreference) findPreference( getString( R.string.pref_vehicle_height ) );
        updateSummary( pref, pref.getText(), "m" );
        pref.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange( Preference preference, Object newValue )
            {
                if( !isFloat( newValue ) )
                    return false;

                updateSummary( preference, newValue.toString(), "m" );
                return true;
            }
        } );

        pref = (EditTextPreference) findPreference( getString( R.string.pref_vehicle_weight ) );
        updateSummary( pref, pref.getText(), "t" );
        pref.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange( Preference preference, Object newValue )
            {
                if( !isFloat( newValue ) )
                    return false;

                updateSummary( preference, newValue.toString(), "t" );
                return true;
            }
        } );

    }

    private void updateSummary( Preference pref, String val, String suffix )
    {
        if( val == null || val.trim().length() == 0 || val.equals( NOT_SET ) )
            pref.setSummary( NOT_SET );
        else
            pref.setSummary( val + " " + suffix );
    }

    private boolean isFloat( Object v )
    {
        if( v == null )
            return false;

        try
        {
            Float.parseFloat( v.toString() );
        }
        catch( NumberFormatException nfe )
        {
            return false;
        }
        return true;
    }

}

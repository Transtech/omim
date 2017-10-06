package com.mapswithme.maps.settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.TwoStatePreference;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.util.Log;

import com.mapswithme.maps.Framework;
import com.mapswithme.maps.R;
import com.mapswithme.maps.routing.ComplianceController;
import com.mapswithme.maps.routing.GraphHopperRouter;
import com.mapswithme.maps.sound.LanguageData;
import com.mapswithme.maps.sound.TtsPlayer;
import com.mapswithme.util.Config;
import com.mapswithme.util.ThemeSwitcher;
import com.mapswithme.util.statistics.Statistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.net.transtech.geo.model.VehicleProfile;

public class RoutePrefsFragment extends PreferenceFragment
{
  private static final String TAG = "RoutePrefsFragment";
  private static final int REQUEST_INSTALL_DATA = 1;

  private TwoStatePreference mPrefEnabled;
//  private TwoStatePreference mDemoEnabled;
  private ListPreference mPrefLanguages;
  private ListPreference mNetwork;

  private final Map<String, LanguageData> mLanguages = new HashMap<>();
  private LanguageData mCurrentLanguage;
  private String mSelectedLanguage;
  private String mSelectedNetwork;
//  private TwoStatePreference mAdherenceMode;

  private final Preference.OnPreferenceChangeListener mEnabledListener = new Preference.OnPreferenceChangeListener()
  {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
      Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.VOICE_ENABLED, Statistics.params().add(Statistics.EventParam.ENABLED, newValue.toString()));
      boolean set = (Boolean)newValue;
      if (!set)
      {
        TtsPlayer.setEnabled(false);
        mPrefLanguages.setEnabled(false);
        return true;
      }

      if (mCurrentLanguage != null && mCurrentLanguage.downloaded)
      {
        setLanguage(mCurrentLanguage);
        return true;
      }

      mPrefLanguages.setEnabled(true);
      getPreferenceScreen().onItemClick(null, null, mPrefLanguages.getOrder(), 0);
      mPrefLanguages.setEnabled(false);
      return false;
    }
  };

  private final Preference.OnPreferenceChangeListener mLangListener = new Preference.OnPreferenceChangeListener()
  {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
      if (newValue == null)
        return false;

      mSelectedLanguage = (String)newValue;
      Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.VOICE_LANGUAGE, Statistics.params().add(Statistics.EventParam.LANGUAGE, mSelectedLanguage));
      LanguageData lang = mLanguages.get(mSelectedLanguage);
      if (lang == null)
        return false;

      if (lang.downloaded)
        setLanguage(lang);
      else
        startActivityForResult(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA), REQUEST_INSTALL_DATA);

      return false;
    }
  };

    private final Preference.OnPreferenceChangeListener mNetworkListener = new Preference.OnPreferenceChangeListener()
    {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
            if (newValue == null)
                return false;

            mSelectedNetwork = (String)newValue;
            mNetwork.setValue( mSelectedNetwork );
            boolean result = ComplianceController.get().setNetworkProfile( mSelectedNetwork );
            update();

            ThemeSwitcher.restart(true);

            return result;
        }
    };


    private void enableListeners(boolean enable)
  {
    mPrefEnabled.setOnPreferenceChangeListener( enable ? mEnabledListener : null );
    mPrefLanguages.setOnPreferenceChangeListener(enable ? mLangListener : null);
//      Log.i( "Maps_RoutePrefsFragment", (enable ? "Enabling" : "Disabling") + " useDemoGPS pref listener " );
//      mDemoEnabled.setOnPreferenceChangeListener( enable ? mDemoListener : null );
      mNetwork.setOnPreferenceChangeListener(enable ? mNetworkListener : null);
//      mAdherenceMode.setOnPreferenceChangeListener(enable ? mModeListener : null);
  }

  private void setLanguage(@NonNull LanguageData lang)
  {
    Config.setTtsEnabled(true);
    TtsPlayer.INSTANCE.setLanguage(lang);
    mPrefLanguages.setSummary(lang.name);

    update();
  }

  private void update()
  {
    enableListeners(false);

    List<LanguageData> languages = TtsPlayer.INSTANCE.refreshLanguages();
    mLanguages.clear();
    mCurrentLanguage = null;

    if (languages.isEmpty())
    {
      mPrefEnabled.setChecked(false);
      mPrefEnabled.setEnabled(false);
      mPrefEnabled.setSummary(R.string.pref_tts_unavailable);
      mPrefLanguages.setEnabled(false);
      mPrefLanguages.setSummary(null);

      enableListeners(true);
      return;
    }

    mPrefEnabled.setChecked(TtsPlayer.INSTANCE.isEnabled());
    mPrefEnabled.setSummary(null);

    final CharSequence[] entries = new CharSequence[languages.size()];
    final CharSequence[] values = new CharSequence[languages.size()];
    for (int i = 0; i < languages.size(); i++)
    {
      LanguageData lang = languages.get(i);
      entries[i] = lang.name;
      values[i] = lang.internalCode;

      mLanguages.put(lang.internalCode, lang);
    }

    mPrefLanguages.setEntries(entries);
    mPrefLanguages.setEntryValues(values);

    mCurrentLanguage = TtsPlayer.getSelectedLanguage(languages);
    boolean available = (mCurrentLanguage != null && mCurrentLanguage.downloaded);
    mPrefLanguages.setEnabled(available && TtsPlayer.INSTANCE.isEnabled());
    mPrefLanguages.setSummary(available ? mCurrentLanguage.name : null);
    mPrefLanguages.setValue(available ? mCurrentLanguage.internalCode : null);

    mPrefEnabled.setChecked(available && TtsPlayer.INSTANCE.isEnabled());

//      mAdherenceMode.setChecked( ComplianceController.get().getDefaultMode() == ComplianceController.ComplianceMode.NETWORK_ADHERENCE );

      GraphHopperRouter truckRouter = ComplianceController.get().getRouter( RoutePrefsFragment.this.getActivity() );

      try {
        List<VehicleProfile> profiles = truckRouter.getGeoEngine().getVehicleProfiles();
        if (profiles != null && profiles.size() > 0) {
          final CharSequence[] entries2 = new CharSequence[profiles.size()];
          final CharSequence[] values2 = new CharSequence[profiles.size()];

          int i = 0;
          for (VehicleProfile vp : profiles) {
            entries2[i] = vp.getDescription();
            values2[i] = vp.getCode();
            i++;
          }

          mNetwork.setEntries(entries2);
          mNetwork.setEntryValues(values2);

          VehicleProfile vp = truckRouter.getSelectedProfile();
          mNetwork.setSummary(vp == null ? null : vp.getDescription() + " (" + vp.getCode() + ")");
          mNetwork.setValue(vp == null ? null : vp.getCode());
        }
      }
      catch (Exception e) {
        new android.support.v7.app.AlertDialog.Builder(getActivity())
                .setMessage("Map version mismatched")
                .create()
                .show();

        Log.e(TAG, e.getMessage());
      }

      enableListeners( true );
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.prefs_route);

    mPrefEnabled = (TwoStatePreference) findPreference(getString(R.string.pref_tts_enabled));
//    mDemoEnabled = (TwoStatePreference) findPreference(getString(R.string.pref_demo_gps));
    mPrefLanguages = (ListPreference) findPreference(getString(R.string.pref_tts_language));
      mNetwork = (ListPreference) findPreference(getString(R.string.pref_route_network));
//      mAdherenceMode = (TwoStatePreference) findPreference(getString(R.string.pref_route_mode));

    final Framework.Params3dMode _3d = new Framework.Params3dMode();
    Framework.nativeGet3dMode(_3d);

    final TwoStatePreference pref3d = (TwoStatePreference)findPreference(getString(R.string.pref_3d));
    pref3d.setChecked(_3d.enabled);

    pref3d.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
    {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        Framework.nativeSet3dMode((Boolean) newValue, _3d.buildings);
        return true;
      }
    });

    boolean autozoomEnabled = Framework.nativeGetAutoZoomEnabled();
    final TwoStatePreference prefAutoZoom = (TwoStatePreference)findPreference(getString(R.string.pref_auto_zoom));
    prefAutoZoom.setChecked(autozoomEnabled);
    prefAutoZoom.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
    {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        Framework.nativeSetAutoZoomEnabled((Boolean)newValue);
        return true;
      }
    });

    update();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    // Do not check resultCode here as it is always RESULT_CANCELED
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_INSTALL_DATA)
    {
      update();

      LanguageData lang = mLanguages.get(mSelectedLanguage);
      if (lang != null && lang.downloaded)
        setLanguage(lang);
    }
  }
}

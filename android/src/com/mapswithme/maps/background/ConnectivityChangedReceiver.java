package com.mapswithme.maps.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;
import com.mapswithme.maps.MwmApplication;
import com.mapswithme.maps.downloader.MapManager;
import com.mapswithme.transtech.OtaMapdataUpdater;
import com.mapswithme.util.ConnectionState;

import static com.mapswithme.maps.MwmApplication.prefs;

public class ConnectivityChangedReceiver extends BroadcastReceiver
{
  private static final String DOWNLOAD_UPDATE_TIMESTAMP = "DownloadOrUpdateTimestamp";
  private static final long MIN_EVENT_DELTA_MILLIS = 60 * 1000; // 1 minute

  @Override
  public void onReceive(Context context, Intent intent)
  {
    if (!OtaMapdataUpdater.isDownloadAllowed() || MapManager.nativeNeedMigrate())
      return;

    MwmApplication.get().initNativeCore();
    OtaMapdataUpdater.init(context);

    /*
    final long lastEventTimestamp = prefs().getLong(DOWNLOAD_UPDATE_TIMESTAMP, 0);

    if (System.currentTimeMillis() - lastEventTimestamp > MIN_EVENT_DELTA_MILLIS)
    {
      prefs().edit()
             .putLong(DOWNLOAD_UPDATE_TIMESTAMP, System.currentTimeMillis())
             .apply();

      MwmApplication.get().initNativeCore();
    }
    */
  }
}

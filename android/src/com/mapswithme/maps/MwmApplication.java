package com.mapswithme.maps;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.UiThread;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.ndk.CrashlyticsNdk;
import com.mapswithme.maps.background.AppBackgroundTracker;
import com.mapswithme.maps.background.Notifier;
import com.mapswithme.maps.bookmarks.data.BookmarkManager;
import com.mapswithme.maps.downloader.CountryItem;
import com.mapswithme.maps.downloader.MapManager;
import com.mapswithme.maps.editor.Editor;
import com.mapswithme.maps.location.DemoLocationProvider;
import com.mapswithme.maps.location.TrackRecorder;
import com.mapswithme.maps.routing.RoutingController;
import com.mapswithme.maps.settings.StoragePathManager;
import com.mapswithme.maps.sound.TtsPlayer;
import com.mapswithme.maps.traffic.TrafficManager;
import com.mapswithme.transtech.OtaMapdataUpdater;
import com.mapswithme.util.*;
//import com.mapswithme.util.statistics.PushwooshHelper;
import com.mapswithme.util.statistics.Statistics;
//import com.my.tracker.MyTracker;
//import com.my.tracker.MyTrackerParams;
//import com.pushwoosh.PushManager;
import io.fabric.sdk.android.Fabric;
//import net.hockeyapp.android.CrashManager;

import java.io.File;
import java.util.List;

public class MwmApplication extends MultiDexApplication
{
  private final static String TAG = "MwmApplication";

  private static MwmApplication sSelf;
  private static MwmActivity sMvmActivity;
  private SharedPreferences mPrefs;
  private AppBackgroundTracker mBackgroundTracker;

  private boolean mAreCountersInitialized;
  private boolean mIsFrameworkInitialized;

  private Handler mMainLoopHandler;
  private final Object mMainQueueToken = new Object();

  public static boolean disclaimerAccepted = false;

  /*
  private final MapManager.StorageCallback mStorageCallbacks = new MapManager.StorageCallback()
  {
    @Override
    public void onStatusChanged(List<MapManager.StorageCallbackData> data)
    {
      for (MapManager.StorageCallbackData item : data)
        if (item.isLeafNode && item.newStatus == CountryItem.STATUS_FAILED)
        {
          if (MapManager.nativeIsAutoretryFailed())
          {
            Notifier.cancelDownloadSuggest();

            Notifier.notifyDownloadFailed(item.countryId, MapManager.nativeGetName(item.countryId));
            MapManager.sendErrorStat(Statistics.EventName.DOWNLOADER_ERROR, MapManager.nativeGetError(item.countryId));
          }

          return;
        }
    }

    @Override
    public void onProgress(String countryId, long localSize, long remoteSize) {}
  };
  */

  public MwmApplication()
  {
    super();
    sSelf = this;
  }

  public static MwmApplication get()
  {
    return sSelf;
  }

  public static AppBackgroundTracker backgroundTracker()
  {
    return sSelf.mBackgroundTracker;
  }

  public static SharedPreferences prefs()
  {
    return sSelf.mPrefs;
  }

  private static boolean isCrashlyticsEnabled()
  {
    return !BuildConfig.FABRIC_API_KEY.startsWith("0000");
  }

  public static MwmActivity getsMvmActivity() {
    return sMvmActivity;
  }

  public static void setsMvmActivity(MwmActivity sMvmActivity) {
    MwmApplication.sMvmActivity = sMvmActivity;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public void onCreate()
  {
    super.onCreate();
    mMainLoopHandler = new Handler(getMainLooper());

    //initHockeyApp();

    initCrashlytics();
//    final boolean isInstallationIdFound =
//      setInstallationIdToCrashlytics();

    //initPushWoosh();
    //initTracker();

    String settingsPath = getSettingsPath();
    new File(settingsPath).mkdirs();
    new File(getTempPath()).mkdirs();

    // First we need initialize paths and platform to have access to settings and other components.
    nativePreparePlatform(settingsPath);
    nativeInitPlatform(getApkPath(), getStoragePath(settingsPath), getTempPath(), getObbGooglePath(),
                       BuildConfig.FLAVOR, BuildConfig.BUILD_TYPE, UiUtils.isTablet());

    Statistics s = Statistics.INSTANCE;

//    if (!isInstallationIdFound)
//      setInstallationIdToCrashlytics();

    OtaMapdataUpdater.init(this);

    mPrefs = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
    mBackgroundTracker = new AppBackgroundTracker();
    TrackRecorder.init();
    Editor.init();

    DemoLocationProvider.stopNotification();
  }

  public void initNativeCore()
  {
    if (mIsFrameworkInitialized)
      return;

    nativeInitFramework();

    //MapManager.nativeSubscribe(mStorageCallbacks);

    initNativeStrings();
    BookmarkManager.nativeLoadBookmarks();
    TtsPlayer.INSTANCE.init(this);
    ThemeSwitcher.restart(false);
    RoutingController.get().initialize();
    TrafficManager.INSTANCE.initialize();
    mIsFrameworkInitialized = true;
  }

  private void initNativeStrings()
  {
    nativeAddLocalization("country_status_added_to_queue", getString(R.string.country_status_added_to_queue));
    nativeAddLocalization("country_status_downloading", getString(R.string.country_status_downloading));
    nativeAddLocalization("country_status_download", getString(R.string.country_status_download));
    nativeAddLocalization("country_status_download_without_routing", getString(R.string.country_status_download_without_routing));
    nativeAddLocalization("country_status_download_failed", getString(R.string.country_status_download_failed));
    nativeAddLocalization("try_again", getString(R.string.try_again));
    nativeAddLocalization("not_enough_free_space_on_sdcard", getString(R.string.not_enough_free_space_on_sdcard));
    nativeAddLocalization("placepage_unknown_place", getString(R.string.placepage_unknown_place));
    nativeAddLocalization("my_places", getString(R.string.my_places));
    nativeAddLocalization("my_position", getString(R.string.my_position));
    nativeAddLocalization("routes", getString(R.string.routes));
    nativeAddLocalization("cancel", getString(R.string.cancel));
    nativeAddLocalization("wifi", getString(R.string.wifi));

    nativeAddLocalization("routing_failed_unknown_my_position", getString(R.string.routing_failed_unknown_my_position));
    nativeAddLocalization("routing_failed_has_no_routing_file", getString(R.string.routing_failed_has_no_routing_file));
    nativeAddLocalization("routing_failed_start_point_not_found", getString(R.string.routing_failed_start_point_not_found));
    nativeAddLocalization("routing_failed_dst_point_not_found", getString(R.string.routing_failed_dst_point_not_found));
    nativeAddLocalization("routing_failed_cross_mwm_building", getString(R.string.routing_failed_cross_mwm_building));
    nativeAddLocalization("routing_failed_route_not_found", getString(R.string.routing_failed_route_not_found));
    nativeAddLocalization("routing_failed_internal_error", getString(R.string.routing_failed_internal_error));
    nativeAddLocalization("place_page_booking_rating", getString(R.string.place_page_booking_rating));
  }

  /*
  private void initHockeyApp()
  {
    String id = ("beta".equals(BuildConfig.BUILD_TYPE) ? PrivateVariables.hockeyAppBetaId()
                                                       : PrivateVariables.hockeyAppId());
    if (!TextUtils.isEmpty(id))
      CrashManager.register(this, id);
  }
  */

  private void initCrashlytics()
  {
    if (!isCrashlyticsEnabled())
      return;

    Fabric.with(this, new Crashlytics(), new CrashlyticsNdk());

    nativeInitCrashlytics();
  }

  /*
  private static boolean setInstallationIdToCrashlytics()
  {
    if (!isCrashlyticsEnabled())
      return false;

    final String installationId = Utils.getInstallationId();
    // If installation id is not found this means id was not
    // generated by alohalytics yet and it is a first run.
    if (TextUtils.isEmpty(installationId))
      return false;

    Crashlytics.setString("AlohalyticsInstallationId", installationId);
    return true;
  }
  */

  public boolean isFrameworkInitialized()
  {
    return mIsFrameworkInitialized;
  }

  public String getApkPath()
  {
    try
    {
      return getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0).sourceDir;
    } catch (final NameNotFoundException e)
    {
      Log.e(TAG, "Can't get apk path from PackageManager");
      return "";
    }
  }

  public static String getSettingsPath()
  {
    return Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.MWM_DIR_POSTFIX;
  }

  private static String getStoragePath(String settingsPath)
  {
    String path = Config.getStoragePath();
    if (!TextUtils.isEmpty(path))
    {
      File f = new File(path);
      if (f.exists() && f.isDirectory())
        return path;

      path = new StoragePathManager().findMapsMeStorage(settingsPath);
      Config.setStoragePath(path);
      return path;
    }

    return settingsPath;
  }

  public String getTempPath()
  {
    final File cacheDir = getExternalCacheDir();
    if (cacheDir != null)
      return cacheDir.getAbsolutePath();

    return Environment.getExternalStorageDirectory().getAbsolutePath() +
           String.format(Constants.STORAGE_PATH, BuildConfig.APPLICATION_ID, Constants.CACHE_DIR);
  }

  private static String getObbGooglePath()
  {
    final String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    return storagePath.concat(String.format(Constants.OBB_PATH, BuildConfig.APPLICATION_ID));
  }

  static
  {
    System.loadLibrary("mapswithme");
  }

  /*
  private void initPushWoosh()
  {
    try
    {
      if (BuildConfig.PW_APPID.equals(PW_EMPTY_APP_ID))
        return;

      PushManager pushManager = PushManager.getInstance(this);

      pushManager.onStartup(this);
      pushManager.registerForPushNotifications();

      PushwooshHelper.get().setContext(this);
      PushwooshHelper.get().synchronize();
    }
    catch(Exception e)
    {
      Log.e("Pushwoosh", e.getLocalizedMessage());
    }
  }
  */

  @SuppressWarnings("unused")
  void sendPushWooshTags(String tag, String[] values)
  {
    /*
    try
    {
      if (values.length == 1)
        PushwooshHelper.get().sendTag(tag, values[0]);
      else
        PushwooshHelper.get().sendTag(tag, values);
    }
    catch(Exception e)
    {
      Log.e("Pushwoosh", e.getLocalizedMessage());
    }
    */
  }

  /*
  private void initTracker()
  {
    MyTracker.setDebugMode(BuildConfig.DEBUG);
    MyTracker.createTracker(PrivateVariables.myTrackerKey(), this);
    final MyTrackerParams myParams = MyTracker.getTrackerParams();
    myParams.setDefaultVendorAppPackage();
    MyTracker.initTracker();
  }
  */

  public void initCounters()
  {
    if (!mAreCountersInitialized)
    {
      mAreCountersInitialized = true;
      Config.updateLaunchCounter();
      PreferenceManager.setDefaultValues(this, R.xml.prefs_misc, false);
    }
  }

  public static void onUpgrade()
  {
    Config.resetAppSessionCounters();
  }

  @SuppressWarnings("unused")
  void forwardToMainThread(final long functorPointer)
  {
    Message m = Message.obtain(mMainLoopHandler, new Runnable()
    {
      @Override
      public void run()
      {
        nativeProcessFunctor(functorPointer);
      }
    });
    m.obj = mMainQueueToken;
    mMainLoopHandler.sendMessage(m);
  }

  private static native void nativePreparePlatform(String settingsPath);
  private native void nativeInitPlatform(String apkPath, String storagePath, String tmpPath, String obbGooglePath,
                                         String flavorName, String buildType, boolean isTablet);

  private static native void nativeInitFramework();
  private static native void nativeProcessFunctor(long functorPointer);
  private static native void nativeAddLocalization(String name, String value);

  @UiThread
  private static native void nativeInitCrashlytics();
}

package com.mapswithme.maps;

import android.annotation.SuppressLint;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mapswithme.maps.MwmActivity.MapTask;
import com.mapswithme.maps.MwmActivity.OpenUrlTask;
import com.mapswithme.maps.api.Const;
import com.mapswithme.maps.api.ParsedMwmRequest;
import com.mapswithme.maps.base.BaseMwmFragmentActivity;
import com.mapswithme.maps.bookmarks.data.BookmarkManager;
import com.mapswithme.maps.search.SearchEngine;
import com.mapswithme.transtech.OtaMapdataUpdater;
import com.mapswithme.util.Constants;
import com.mapswithme.util.UiUtils;
import com.mapswithme.util.Utils;
import com.mapswithme.util.concurrency.ThreadPool;
import com.mapswithme.util.statistics.Statistics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressLint("StringFormatMatches")
public class DownloadResourcesActivity extends BaseMwmFragmentActivity
{
  private static final String TAG = DownloadResourcesActivity.class.getName();

  static final String EXTRA_COUNTRY = "country";
  static final String EXTRA_AUTODOWNLOAD = "autodownload";

  private TextView mTvMessage;
  private ProgressBar mProgress;
  private Button mBtnRetry;
  private MapTask mMapTaskToForward;

  private boolean mIsReadingAttachment;

  @SuppressWarnings("unused")
  private interface Listener
  {
    void onProgress(int percent);
    void onFinish(int errorCode);
  }

  private final IntentProcessor[] mIntentProcessors = {
      new GeoIntentProcessor(),
      new HttpGe0IntentProcessor(),
      new Ge0IntentProcessor(),
      new MapsWithMeIntentProcessor(),
      new GoogleMapsIntentProcessor(),
      new OpenCountryTaskProcessor(),
      new KmzKmlProcessor()
  };

  private BroadcastReceiver mOtaMapdataUpdaterReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(OtaMapdataUpdater.ACTION_UPDATE_PROGRESS_BROADCAST)) {
        String message = intent.getStringExtra("MESSAGE");
        int progress = intent.getIntExtra("PROGRESS", -1);

        UiUtils.hide(mBtnRetry);
        mTvMessage.setText(message);

        if (progress >= 0) {
          mProgress.setIndeterminate(false);
          mProgress.setMax(100);
          mProgress.setProgress(progress);

          UiUtils.show(mProgress);
        }
        else {
          mProgress.setIndeterminate(true);
          UiUtils.show(mProgress);
        }
      }
      else if (intent.getAction().equals(OtaMapdataUpdater.ACTION_UPDATE_STATUS_BROADCAST)) {
        int status = intent.getIntExtra("STATUS", 0);
        int code = intent.getIntExtra("CODE", 0);

        UiUtils.hide(mProgress);

        switch(status) {
          case OtaMapdataUpdater.STATUS_OK:
            UiUtils.hide(mBtnRetry);
            break;
          case OtaMapdataUpdater.STATUS_ERR_NO_WIFI:
            UiUtils.show(mBtnRetry);
            mTvMessage.setText("WiFi not connected.\nBefore you start using the app, allow us to download some required resources.\nPlease connect to WiFi to start the provisioning.");
            break;
          case OtaMapdataUpdater.STATUS_ERR_DOWNLOAD_FAILED:
            UiUtils.show(mBtnRetry);
            mTvMessage.setText("Download failed, code:" + Integer.toString(code) + ", \nPlease connect to WiFi and retry.");
            break;
          case OtaMapdataUpdater.STATUS_ERR_MAP_VERSION_NOT_CONFIGURED:
            UiUtils.show(mBtnRetry);
            mTvMessage.setText("Map data version not configured.\nPlease ensure proper map data version is configured in NextGen");
            break;
          case OtaMapdataUpdater.STATUS_ERR_MD5:
            UiUtils.show(mBtnRetry);
            mTvMessage.setText("Checksum validation failed.\nPlease connect to WiFi and retry");
            break;
        }
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download_resources_transtech);
    initViewsAndListeners();

    UiUtils.hide(mBtnRetry);

    if (!OtaMapdataUpdater.isProvisioned()) {
      if (OtaMapdataUpdater.isDownloadAllowed()) {
        UiUtils.show(mProgress);
        mTvMessage.setText("Before you start using the app, allow us to download some required resources.\nKindly keep the WiFi connected.");
      }
      else {
        UiUtils.hide(mProgress);
        mTvMessage.setText("Before you start using the app, allow us to download some required resources.\nPlease connect to WiFi to start the provisioning.");
      }

      // block starting of main ui
      return;
    }

    dispatchIntent();
    showMap();
  }

  @Override
  protected void onDestroy()
  {
    super.onDestroy();
    Utils.keepScreenOn(false, getWindow());
  }

  @Override
  protected void onResume()
  {
    super.onResume();

    IntentFilter intentFilter =  new IntentFilter();
    intentFilter.addAction(OtaMapdataUpdater.ACTION_UPDATE_PROGRESS_BROADCAST);
    intentFilter.addAction(OtaMapdataUpdater.ACTION_UPDATE_STATUS_BROADCAST);
    registerReceiver(mOtaMapdataUpdaterReceiver, intentFilter);
  }

  @Override
  protected void onPause()
  {
    super.onPause();
    unregisterReceiver(mOtaMapdataUpdaterReceiver);
  }

  private void initViewsAndListeners()
  {
    mTvMessage = (TextView) findViewById(R.id.tv__download_message);
    mProgress = (ProgressBar) findViewById(R.id.pb__download_resources);

    mBtnRetry = (Button) findViewById(R.id.btn__download_resources);

    mBtnRetry.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        OtaMapdataUpdater.init(MwmApplication.get());
      }
    });
  }

  private void showMap()
  {
    if (mIsReadingAttachment || !OtaMapdataUpdater.isProvisioned())
      return;

    final Intent intent = new Intent(this, MwmActivity.class);

    // Disable animation because MwmActivity should appear exactly over this one
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);

    // Add saved task to forward to map activity.
    if (mMapTaskToForward != null)
    {
      intent.putExtra(MwmActivity.EXTRA_TASK, mMapTaskToForward);
      mMapTaskToForward = null;
    }

    startActivity(intent);

    finish();
  }

  private boolean dispatchIntent()
  {
    final Intent intent = getIntent();
    if (intent == null)
      return false;

    for (final IntentProcessor ip : mIntentProcessors)
      if (ip.isSupported(intent) && ip.process(intent))
        return true;

    return false;
  }

  private class GeoIntentProcessor implements IntentProcessor
  {
    @Override
    public boolean isSupported(Intent intent)
    {
      return (intent.getData() != null && "geo".equals(intent.getScheme()));
    }

    @Override
    public boolean process(Intent intent)
    {
      final String url = intent.getData().toString();
      Log.i(TAG, "Query = " + url);
      mMapTaskToForward = new OpenUrlTask(url);
//      org.alohalytics.Statistics.logEvent("GeoIntentProcessor::process", url);
      return true;
    }
  }

  private class Ge0IntentProcessor implements IntentProcessor
  {
    @Override
    public boolean isSupported(Intent intent)
    {
      return (intent.getData() != null && "ge0".equals(intent.getScheme()));
    }

    @Override
    public boolean process(Intent intent)
    {
      final String url = intent.getData().toString();
      Log.i(TAG, "URL = " + url);
      mMapTaskToForward = new OpenUrlTask(url);
//      org.alohalytics.Statistics.logEvent("Ge0IntentProcessor::process", url);
      return true;
    }
  }

  private class HttpGe0IntentProcessor implements IntentProcessor
  {
    @Override
    public boolean isSupported(Intent intent)
    {
      if ("http".equalsIgnoreCase(intent.getScheme()))
      {
        final Uri data = intent.getData();
        if (data != null)
          return "ge0.me".equals(data.getHost());
      }

      return false;
    }

    @Override
    public boolean process(Intent intent)
    {
      final Uri data = intent.getData();
      Log.i(TAG, "URL = " + data.toString());

      final String ge0Url = "ge0:/" + data.getPath();
      mMapTaskToForward = new OpenUrlTask(ge0Url);
//      org.alohalytics.Statistics.logEvent("HttpGe0IntentProcessor::process", ge0Url);
      return true;
    }
  }

  //Use this to invoke API task.
  private class MapsWithMeIntentProcessor implements IntentProcessor
  {
    @Override
    public boolean isSupported(Intent intent)
    {
      return Const.ACTION_MWM_REQUEST.equals(intent.getAction());
    }

    @Override
    public boolean process(final Intent intent)
    {
      final String apiUrl = intent.getStringExtra(Const.EXTRA_URL);
//      org.alohalytics.Statistics.logEvent("MapsWithMeIntentProcessor::process", apiUrl == null ? "null" : apiUrl);
      if (apiUrl != null)
      {
        SearchEngine.nativeCancelInteractiveSearch();

        final ParsedMwmRequest request = ParsedMwmRequest.extractFromIntent(intent);
        ParsedMwmRequest.setCurrentRequest(request);
        Statistics.INSTANCE.trackApiCall(request);

        if (!ParsedMwmRequest.isPickPointMode())
          mMapTaskToForward = new OpenUrlTask(apiUrl);
        return true;
      }

      return false;
    }
  }

  private class GoogleMapsIntentProcessor implements IntentProcessor
  {
    @Override
    public boolean isSupported(Intent intent)
    {
      final Uri data = intent.getData();
      return (data != null && "maps.google.com".equals(data.getHost()));
    }

    @Override
    public boolean process(Intent intent)
    {
      final String url = intent.getData().toString();
      Log.i(TAG, "URL = " + url);
      mMapTaskToForward = new OpenUrlTask(url);
//      org.alohalytics.Statistics.logEvent("GoogleMapsIntentProcessor::process", url);
      return true;
    }
  }

  private class OpenCountryTaskProcessor implements IntentProcessor
  {
    @Override
    public boolean isSupported(Intent intent)
    {
      return intent.hasExtra(EXTRA_COUNTRY);
    }

    @Override
    public boolean process(Intent intent)
    {
      String countryId = intent.getStringExtra(EXTRA_COUNTRY);
      final boolean autoDownload = intent.getBooleanExtra(EXTRA_AUTODOWNLOAD, false);
      if (autoDownload)
        Statistics.INSTANCE.trackEvent(Statistics.EventName.DOWNLOAD_COUNTRY_NOTIFICATION_CLICKED);

      mMapTaskToForward = new MwmActivity.ShowCountryTask(countryId, autoDownload);
//      org.alohalytics.Statistics.logEvent("OpenCountryTaskProcessor::process",
//                                          new String[] { "autoDownload", String.valueOf(autoDownload) },
//                                          LocationHelper.INSTANCE.getSavedLocation());
      return true;
    }
  }

  private class KmzKmlProcessor implements IntentProcessor
  {
    private Uri mData;

    @Override
    public boolean isSupported(Intent intent)
    {
      mData = intent.getData();
      return mData != null;
    }

    @Override
    public boolean process(Intent intent)
    {
      mIsReadingAttachment = true;
      ThreadPool.getStorage().execute(new Runnable()
      {
        @Override
        public void run()
        {
          final boolean result = readKmzFromIntent();
          runOnUiThread(new Runnable()
          {
            @Override
            public void run()
            {
              Utils.toastShortcut(DownloadResourcesActivity.this, result ? R.string.load_kmz_successful : R.string.load_kmz_failed);
              mIsReadingAttachment = false;
              showMap();
            }
          });
        }
      });
      return true;
    }

    private boolean readKmzFromIntent()
    {
      String path = null;
      File tmpFile = null;
      final String scheme = mData.getScheme();
      if (scheme != null && !scheme.equalsIgnoreCase(ContentResolver.SCHEME_FILE))
      {
        // scheme is "content" or "http" - need to download or read file first
        InputStream input = null;
        OutputStream output = null;

        try
        {
          final ContentResolver resolver = getContentResolver();
          final String ext = getExtensionFromMime(resolver.getType(mData));
          if (ext != null)
          {
            final String filePath = MwmApplication.get().getTempPath() + "Attachment" + ext;

            tmpFile = new File(filePath);
            output = new FileOutputStream(tmpFile);
            input = resolver.openInputStream(mData);

            final byte buffer[] = new byte[Constants.MB / 2];
            int read;
            while ((read = input.read(buffer)) != -1)
              output.write(buffer, 0, read);
            output.flush();

            path = filePath;
          }
        } catch (final Exception ex)
        {
          Log.w(TAG, "Attachment not found or io error: " + ex);
        } finally
        {
          Utils.closeStream(input);
          Utils.closeStream(output);
        }
      }
      else
        path = mData.getPath();

      boolean result = false;
      if (path != null)
      {
        Log.d(TAG, "Loading bookmarks file from: " + path);
        result = BookmarkManager.nativeLoadKmzFile(path);
      }
      else
        Log.w(TAG, "Can't get bookmarks file from URI: " + mData);

      if (tmpFile != null)
        //noinspection ResultOfMethodCallIgnored
        tmpFile.delete();

      return result;
    }

    private String getExtensionFromMime(String mime)
    {
      final int i = mime.lastIndexOf('.');
      if (i == -1)
        return null;

      mime = mime.substring(i + 1);
      if (mime.equalsIgnoreCase("kmz"))
        return ".kmz";
      else if (mime.equalsIgnoreCase("kml+xml"))
        return ".kml";
      else
        return null;
    }
  }

  private static native int nativeGetBytesToDownload();
  private static native int nativeStartNextFileDownload(Listener listener);
  private static native void nativeCancelCurrentFile();
}
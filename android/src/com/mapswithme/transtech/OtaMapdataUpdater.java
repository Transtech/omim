package com.mapswithme.transtech;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.WindowManager;
import com.mapswithme.maps.Framework;
import com.mapswithme.maps.MwmApplication;
import com.mapswithme.maps.R;
import com.mapswithme.maps.background.Notifier;
import com.mapswithme.util.ConnectionState;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by warren on 21/07/2017.
 */
public class OtaMapdataUpdater extends Service {
    private static final String LOG_TAG = "OtaMapdataUpdater";
    public static final String ACTION_CHECK_FOR_UPDATES = "com.mapswithme.transtech.action.CHECK_FOR_UPDATES";
    public static final String EXTRA_IMMEDIATE_SCHEDULE = "extra_immediate_schedule";

    public static final String ACTION_UPDATE_STATUS_BROADCAST = "com.mapswithme.transtech.action.UPDATE_STATUS_BROADCAST";
    public static final String ACTION_UPDATE_PROGRESS_BROADCAST = "com.mapswithme.transtech.action.UPDATE_PROGRESS_BROADCAST";

    public static final int STATUS_OK = 0;
    public static final int STATUS_ERR_NO_WIFI = 1;
    public static final int STATUS_ERR_DOWNLOAD_FAILED = 2;
    public static final int STATUS_ERR_MAP_VERSION_NOT_CONFIGURED = 3;
    public static final int STATUS_ERR_MD5= 4;


    private static final int BUFFER_SIZE = 4096;
    private static UpdateTask updateTask;
    private static long threadStartTime;

    private static final MwmApplication APP = MwmApplication.get();
    private static final String DATA_PATH = Framework.nativeGetWritableDir();

    public static void init(Context context) {
        context.startService(new Intent(context, OtaMapdataUpdater.class).setAction(OtaMapdataUpdater.ACTION_CHECK_FOR_UPDATES));
    }

    public static boolean isProvisioned() {
        if ("0".equals(getCurrentMapdataVersion())) return false;
        else return true;
    }

    public static void showProvisioningAlertDialog() {
        AlertDialog dlg = new AlertDialog.Builder(MwmApplication.get())
                .setCancelable(true)
                .setTitle("Initial Map Data Download")
                .setMessage("Provisioning, please keep Wifi connected")
                .setPositiveButton(R.string.ok, null)
                .create();

        dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dlg.show();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        scheduleCheck();
    }

    /**
     * When sent an "ACTION_CHECK_FOR_UPDATES" intent, the service starts off a background thread (via the executorService)
     * to update the device.  This all happens without user intervention.  Whenever this method is claled, it will also schedule
     * a roughly twice daily timer to call this with the "ACTION_CHECK_FOR_UPDATES" timer, meaning that it will check for
     * updates twice a day.
     *
     * It is assumed that this device will be within communication range when this happens.  If comms fails, it will wait another
     * 12 hours before trying again.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.i(LOG_TAG, "OtaMapdataUpdater Service starting");

        long delay = AlarmManager.INTERVAL_HALF_DAY;

        String action = intent != null ? intent.getAction() : null;

        if (action == null || ACTION_CHECK_FOR_UPDATES.equals(action)) {
            // if Extra is present it means we don't want to go ahead right away, but after a short delay
            if(ACTION_CHECK_FOR_UPDATES.equals(action) && intent.getIntExtra(EXTRA_IMMEDIATE_SCHEDULE , 0) != 0){
                delay = intent.getIntExtra(EXTRA_IMMEDIATE_SCHEDULE , 0) * 1000;
                if(delay <= 0)
                    delay = AlarmManager.INTERVAL_HALF_DAY;
            }
            else{
                startUpdateCheck();
            }
        }

        scheduleCheck(delay);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (updateTask != null) {
            updateTask.cancel(true);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Sets up an alarmManager repeating timer to fire roughly twice per day, causing an update.
     */
    private void scheduleCheck() {
        scheduleCheck(AlarmManager.INTERVAL_HALF_DAY);
    }

    /**
     * Sets up an alarmManager repeating timer to fire roughly twice per day, causing an update.
     */
    private void scheduleCheck(long delay) {
        AlarmManager am = (AlarmManager) getSystemService(Activity.ALARM_SERVICE);
        Log.d(LOG_TAG, "Setting alarm. delay is " + delay + " Miliseconds");
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, getPendingIntent());
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(ACTION_CHECK_FOR_UPDATES);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void startUpdateCheck() {

        if (updateTask != null && updateTask.getStatus() != AsyncTask.Status.FINISHED) {
            Log.d(LOG_TAG, "OTA Map Update Thread status = " + String.valueOf(updateTask.getStatus()));
            // Cancel if been running not finished for more than 3 hours
            if (System.currentTimeMillis() - threadStartTime > 3 * 60 * 60 * 1000) {
                Log.d(LOG_TAG, "OTA Map Update Thread has been running too long - cancelling");
                updateTask.cancel(true);
            }
            else {
                return;
            }
        }

        updateTask = new UpdateTask();
        updateTask.execute();
        threadStartTime = System.currentTimeMillis();
    }

    public class UpdateTask extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String targetVersion = Setting.getMapVersion(APP);
                String currentVersion = getCurrentMapdataVersion();

                Log.d(LOG_TAG,"Current Version:" + currentVersion + " Target Version:" + targetVersion);

                if (targetVersion == null || "".equals(targetVersion)) {
                    // not configured, no update
                    Log.d(LOG_TAG, "Map version not configured, no update necessary");
                    broadcastStatus(STATUS_ERR_MAP_VERSION_NOT_CONFIGURED);
                    return false;
                }
                else if (currentVersion.equalsIgnoreCase(targetVersion)) {
                    // version is current, no update
                    Log.d(LOG_TAG, "Map version up to date, no update necessary");
                    return false;
                }

                if (!isDownloadAllowed()) {
                    // not connected to wifi or not data pack
                    Notifier.notifyOtaMapdataUpdatePending();
                    broadcastStatus(STATUS_ERR_NO_WIFI);
                    return false;
                }

                // start the process
                Setting.setOtaLastUpdateDate(APP, new SimpleDateFormat("MMM dd, yyyy h:mm:ss aa", Locale.ENGLISH).format(new Date()));
                Setting.setOtaUpdateStatus(APP, TranstechConstants.UPDATE_STATUS.IN_PROGRESS);

                String baseUrl = Setting.getOtaUpdateUrl(APP) + targetVersion;
                JSONArray jsonArray = getIndex(baseUrl);

                List<String> unchangedFiles = new ArrayList<>();

                // download and check
                for (int i=0; i<jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String filename = jsonObject.optString("name");
                    String checksum = jsonObject.optString("md5");

                    if (isFileUnchanged(DATA_PATH, filename, checksum)) {
                        unchangedFiles.add(filename);
                    }
                    else if (!downloadFile(baseUrl, filename, checksum)) {
                        throw new IOException("Error downloading " + filename);
                    }
                }

                // all download are ok, deploy

                boolean deployOk = true;
                for (int i=0; i<jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String filename = jsonObject.optString("name");
                    String compressType = jsonObject.optString("compressType");

                    if (unchangedFiles.contains(filename)) {
                        Log.i(LOG_TAG, "Skipping unchanged file: " + filename);
                    }
                    else if ("zip".equalsIgnoreCase(compressType)) {
                        Notifier.notifyOtaMapdataUpdate("Unpacking " + filename);
                        broadcastProgress("Unpacking " + filename, -1);

                        if (unpackZip(filename, DATA_PATH)) {
                            // keep the zip file in cache, left it to the system to delete when necessary
                            // deleteFileFromCache(filename);
                        }
                        else deployOk = false;
                    }
                    else {
                        Notifier.notifyOtaMapdataUpdate("Copying " + filename);
                        broadcastProgress("Copying " + filename, -1);

                        if (copyFile(filename, DATA_PATH)) deleteFileFromCache(filename);
                        else deployOk = false;
                    }
                }

                // done, write version
                if (deployOk) {
                    setCurrentMapdataVersion(targetVersion);

                    Setting.setOtaLastUpdateDate(APP, new SimpleDateFormat("MMM dd, yyyy h:mm:ss aa", Locale.ENGLISH).format(new Date()));
                    Setting.setOtaUpdateStatus(APP, TranstechConstants.UPDATE_STATUS.FINISHED);

                    broadcastStatus(STATUS_OK);

                    return true;
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, "Error checking for updates, will try again in half an hour.", e);
                Setting.setOtaUpdateStatus(APP, TranstechConstants.UPDATE_STATUS.FAILED);

                if (isDownloadAllowed()) broadcastStatus(STATUS_ERR_DOWNLOAD_FAILED);
                else broadcastStatus(STATUS_ERR_NO_WIFI);

                // if it fails, we keep trying every 30 minutes
                scheduleCheck(AlarmManager.INTERVAL_HALF_HOUR);
            } finally {
                Notifier.cancelOtaMapdataUpdate();
            }

            return false;
        }

        @Override
        protected void onPreExecute() {

            Notifier.notifyOtaMapdataUpdate("Updating map data...");
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result) {
                AlertDialog dlg = new AlertDialog.Builder(OtaMapdataUpdater.this)
                        .setCancelable(false)
                        .setTitle("Map Data Update")
                        .setMessage("Map data updated.\nPlease restart for the update to take effect.")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.exit(0);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create();

                dlg.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                dlg.show();
            }
        }

        private JSONArray getIndex(String baseUrl) throws IOException {
            String url = baseUrl + "/index.json";

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");

            Log.i(LOG_TAG, "GET " + url + " response: " + connection.getResponseCode());

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.w(LOG_TAG, "GET " + url + " invalid response: " + connection.getResponseCode());
                return null;
            }

            InputStream in = new BufferedInputStream(connection.getInputStream());
            String response = IOUtils.toString(in);

            try {
                return new JSONArray(response);
            } catch (JSONException e) {
                Log.w(LOG_TAG, "Unable to parse json: " + response);
                return null;
            }
        }

        private boolean isFileUnchanged(String path, String filename, String md5) throws IOException {
            File file = new File(path, filename);

            if (file.exists() && fileMD5(file).equalsIgnoreCase(md5)) {
                Log.i(LOG_TAG, filename + " exist and md5 matched");
                return true;
            }

            return false;
        }

        private boolean downloadFile(String baseUrl, String filename, String md5) throws IOException {
            File outputFile = new File(APP.getCacheDir(), filename);
            String url = baseUrl + "/" + URLEncoder.encode(filename, "UTF-8").replace("+", "%20");;

            // check if existing and matched md5
            if (outputFile.exists() && fileMD5(outputFile).equalsIgnoreCase(md5)) {
                Log.i(LOG_TAG, filename + " exist and md5 matched, download skipped");
                return true;
            }

            if (!isDownloadAllowed()) {
                Log.w(LOG_TAG, "Download blocked, not connected to wifi and no data pack");
                broadcastStatus(STATUS_ERR_NO_WIFI);
                return false;
            }

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");

            Log.i(LOG_TAG, "GET " + url + " response: " + connection.getResponseCode());

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.w(LOG_TAG, "GET " + url + " invalid response: " + connection.getResponseCode());
                broadcastStatus(STATUS_ERR_DOWNLOAD_FAILED, connection.getResponseCode());
                return false;
            }

            int fileLength = connection.getContentLength();
            InputStream in = connection.getInputStream();
            OutputStream out = new FileOutputStream(outputFile);

            String notifyMsg = "Downloading " + filename;
            Notifier.notifyOtaMapdataUpdate(notifyMsg);
            broadcastProgress(notifyMsg + "\nKindly keep the WiFi connected.", 0);

            byte[] buf = new byte[BUFFER_SIZE];
            int count, total = 0;
            long lastProgressReport = System.currentTimeMillis();

            while ((count = in.read(buf)) != -1) {
                out.write(buf, 0, count);
                total += count;

                long now = System.currentTimeMillis();
                if (now - lastProgressReport > 10 * 1000) {
                    Log.d(LOG_TAG, "Downloaded " + total + " bytes of " + filename);

                    String progressNotifyMsg = notifyMsg;
                    int progress = -1;
                    if (fileLength > 0) {
                        progress = (int) (total * 100.0 /fileLength);
                        progressNotifyMsg = progressNotifyMsg + " " + Integer.toString(progress) + "%";
                    }

                    Notifier.notifyOtaMapdataUpdate(progressNotifyMsg);
                    broadcastProgress(notifyMsg + "\nKindly keep the WiFi connected.", progress);
                    lastProgressReport = now;

                    // safety check if wifi is still connected
                    if (!isDownloadAllowed()) {
                        broadcastStatus(STATUS_ERR_NO_WIFI);
                        return false;
                    }
                }
            }

            out.close();
            in.close();

            Log.i(LOG_TAG, "Downloaded " + total + " bytes of " + filename + " expect: " + Integer.toString(fileLength));
            Notifier.notifyOtaMapdataUpdate(notifyMsg + ": completed");
            broadcastProgress(notifyMsg, 100);

            if (outputFile.exists() && fileMD5(outputFile).equalsIgnoreCase(md5)) {
                Log.i(LOG_TAG, "md5 Ok");
                return true;
            }

            Log.w(LOG_TAG, "GET " + url + "checksum failed");
            broadcastStatus(STATUS_ERR_MD5);

            return false;
        }
    }

    /**
     * Calculates the MD5 hash of the supplied file (relative to app file context)
     * @param file
     * @return
     * @throws IOException
     */
    private String fileMD5(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");

            byte[] buf = new byte[BUFFER_SIZE];
            int count;

            while ((count = in.read(buf)) != -1){
                digest.update(buf, 0, count);
            }

            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(String.format("%02x", 0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.e(LOG_TAG, "No MD5?!?!", e);
        } finally {
            in.close();
        }
        return "";
    }

    private boolean unpackZip(String zipname, String outputPath)
    {
        File zipfile = new File(APP.getCacheDir(), zipname);

        InputStream is;
        ZipInputStream zis;

        try {
            String filename;
            is = new FileInputStream(zipfile);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry zipEntry;
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;

            while ((zipEntry = zis.getNextEntry()) != null) {
                filename = zipEntry.getName();

                // Need to create directories if not exists
                if (zipEntry.isDirectory()) {
                    String outputFile = outputPath + filename;
                    File fmd = new File(outputFile);
                    fmd.mkdirs();

                    Log.i(LOG_TAG, "Created directory " + outputFile);
                    continue;
                }

                String outputFile = outputPath + filename;
                FileOutputStream fout = new FileOutputStream(outputFile);

                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }

                fout.close();
                zis.closeEntry();

                Log.i(LOG_TAG, "Unzipped " + outputFile);
            }

            zis.close();
        }
        catch(IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean copyFile(String inputFile, String outputPath) {

        InputStream in;
        OutputStream out;

        try {
            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists()) dir.mkdirs();

            in = new FileInputStream(new File(APP.getCacheDir(), inputFile));
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[BUFFER_SIZE];
            int count;

            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }

            in.close();

            out.flush();
            out.close();

            Log.i(LOG_TAG, "Copied " + inputFile + " to " + outputPath);

            return true;

        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        return false;
    }

    private boolean deleteFileFromCache(String filename) {
        try {
            if (new File(APP.getCacheDir(), filename).delete()) {
                Log.i(LOG_TAG, "deleted " + filename + " from cache");
                return true;
            }
        }
        catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        return false;
    }

    public static boolean isDownloadAllowed() {
        /* removed checking for data pack
        boolean wifiCOnnected = ConnectionState.isWifiConnected();
        boolean dataPackEnabled = Setting.isDataPackEnabled(APP);

        Log.d(LOG_TAG, "isWifiConnected=" + Boolean.toString(wifiCOnnected) + " isDataPackEnabled=" + Boolean.toString(dataPackEnabled) );
        return wifiCOnnected || dataPackEnabled;
        */

        return ConnectionState.isWifiConnected();
    }

    public static String getCurrentMapdataVersion() {
        final File file = new File(DATA_PATH, "VERSION.TXT");

        try {
            if (file.exists()) {
                final InputStream inputStream = new FileInputStream(file);
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String line = reader.readLine();

                reader.close();
                inputStream.close();

                return line.trim();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        return "0";
    }

    private void setCurrentMapdataVersion(String version) {
        final File file = new File(DATA_PATH, "VERSION.TXT");

        try {
            final OutputStream outputStream = new FileOutputStream(file);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            outputStreamWriter.write(version);
            outputStreamWriter.close();

        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private void broadcastProgress(String message, int progress) {
        Intent i = new Intent(ACTION_UPDATE_PROGRESS_BROADCAST);
        i.putExtra("MESSAGE", message);
        i.putExtra("PROGRESS", progress);
        sendBroadcast(i);
    }

    private void broadcastStatus(int status) {
        broadcastStatus(status, 0);
    }

    private void broadcastStatus(int status, int code) {
        Intent i = new Intent(ACTION_UPDATE_STATUS_BROADCAST);
        i.putExtra("STATUS", status);
        i.putExtra("CODE", code);
        sendBroadcast(i);
    }
}

package com.mapswithme.transtech;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.os.AsyncTask;
import android.util.Log;
import com.mapswithme.maps.Framework;
import com.mapswithme.maps.MwmApplication;
import com.mapswithme.maps.R;

import java.io.*;

/**
 * Created by warren on 25/05/2017.
 */
public class MapdataUpdater extends BroadcastReceiver {

    static final String TAG = MapdataUpdater.class.getName();

    @Override
    public void onReceive(Context context, final Intent intent) {
        Log.e(TAG, "sdcard");

        String externalPath = intent.getData().getPath() + "/smartnav2";
        final File source = new File(externalPath);

        if (source.exists() && source.isDirectory() && source.canRead()) {
            String curVersion = getVersion(Framework.nativeGetWritableDir());
            String extVersion = getVersion(externalPath);

            new AlertDialog.Builder(MwmApplication.getsMvmActivity())
                    .setCancelable(false)
                    .setTitle("Map Data Update")
                    .setMessage("Do you want to update map data to version: " + extVersion + "?\nCurrent Version: " + curVersion)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dlg, int which) {
                            new CopyAsyncTask().execute(source);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dlg, int which) {
                            dlg.dismiss();
                        }
                    }).create().show();

        }
        else {
            Log.i(TAG, "/smartnav2 directory not found on external device, ignored.");
        }
    }

    private String getVersion(String path) {
        final File file = new File(path + "/VERSION.TXT");

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
            Log.e(TAG, e.getMessage());
        }

        return "0";
    }

    private class CopyAsyncTask extends AsyncTask<File, String, Boolean> {

        private int numCopied = 0;
        ProgressDialog dlg;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            dlg = new ProgressDialog(MwmApplication.getsMvmActivity());
            dlg.setTitle("Updating Maps");
            dlg.setMessage("Please wait...");
            dlg.setCancelable(false);
            dlg.setIndeterminate(true);
            dlg.show();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            dlg.dismiss();

            if (success) {
                new AlertDialog.Builder(MwmApplication.getsMvmActivity())
                        .setCancelable(false)
                        .setTitle("Map Data Update")
                        .setMessage(numCopied + " files updated.\nPlease restart for the update to take effect.")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.exit(0);
                            }
                        })
                        .create().show();
            }
            else {
                new AlertDialog.Builder(MwmApplication.getsMvmActivity())
                        .setCancelable(false)
                        .setTitle("Map Data Update")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage("Error updating map, please retry.")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dlg, int which) { dlg.dismiss(); }
                        })
                        .create().show();
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            dlg.setMessage(values[0]);
        }

        @Override
        protected Boolean doInBackground(File... source) {
            final String internalPath = Framework.nativeGetWritableDir();
            File destination = new File(internalPath);

            try {
                copyDirectory(source[0], destination);
                return true;

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                return false;
            }
        }

        private void copyDirectory(File sourceLocation , File targetLocation)
                throws IOException {

            if (sourceLocation.isDirectory()) {
                if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                    throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
                }

                String[] children = sourceLocation.list();
                for (int i=0; i<children.length; i++) {
                    copyDirectory(new File(sourceLocation, children[i]),
                            new File(targetLocation, children[i]));
                }
            } else {
                File directory = targetLocation.getParentFile();
                if (directory != null && !directory.exists() && !directory.mkdirs()) {
                    throw new IOException("Cannot create dir " + directory.getAbsolutePath());
                }

                publishProgress("Transferring " + sourceLocation.getName());
                Log.i(TAG, "Copying " + sourceLocation.getName());

                InputStream in = new FileInputStream(sourceLocation);
                OutputStream out = new FileOutputStream(targetLocation);

                // Copy
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                numCopied++;
            }
        }
    }

}

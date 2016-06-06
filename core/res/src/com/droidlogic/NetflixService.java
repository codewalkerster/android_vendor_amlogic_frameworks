package com.droidlogic;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.Scanner;

public class NetflixService extends Service {
    private static final String TAG = "NetflixService";

    /**
     * Feature for {@link #getSystemAvailableFeatures} and {@link #hasSystemFeature}:
     * This device supports netflix
     */
    public static final String FEATURE_SOFTWARE_NETFLIX     = "droidlogic.software.netflix";

    public static final String NETFLIX_PKG_NAME             = "com.netflix.ninja";
    public static final String NETFLIX_STATUS_CHANGE        = "com.netflix.action.STATUS_CHANGE";
    public static final String NETFLIX_DIAL_STOP            = "com.netflix.action.DIAL_STOP";
    private static final String VIDEO_SIZE_DEVICE           = "/sys/class/video/device_resolution";
    private static boolean mLaunchDialService               = true;

    private boolean mIsNetflixFg = false;
    private Context mContext;

    private BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "action:" + action);

            if (action.equals(NETFLIX_DIAL_STOP)) {
                int pid = getNetflixPid();
                if (pid > 0) {
                    Log.i (TAG, "Killing active Netflix Service PID: " + pid);
                    android.os.Process.killProcess (pid);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;

        IntentFilter filter = new IntentFilter(NETFLIX_DIAL_STOP);
        filter.setPriority (IntentFilter.SYSTEM_HIGH_PRIORITY);
        mContext.registerReceiver (mReceiver, filter);

        new ObserverThread ("NetflixObserver").start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int getNetflixPid() {
        int retry = 10;
        ActivityManager manager = (ActivityManager) mContext.getSystemService (Context.ACTIVITY_SERVICE);

        do {
            List<RunningAppProcessInfo> services = manager.getRunningAppProcesses();
            for (int i = 0; i < services.size(); i++) {
                String servicename = services.get (i).processName;
                if (servicename.contains (NETFLIX_PKG_NAME)) {
                    Log.i (TAG, "find process: " + servicename + " pid: " + services.get (i).pid);
                    return services.get (i).pid;
                }
            }
        } while (--retry > 0);

        return -1;
    }

    public boolean netflixActivityRunning() {
        ActivityManager am = (ActivityManager) mContext.getSystemService (Context.ACTIVITY_SERVICE);
        List< ActivityManager.RunningTaskInfo > task = am.getRunningTasks (1);

        if (task.size() != 0) {
            if (mLaunchDialService) {
                mLaunchDialService = false;

                try {
                    Intent intent = new Intent();
                    intent.setClassName ("com.netflix.dial", "com.netflix.dial.NetflixDialService");
                    mContext.startService (intent);
                } catch (SecurityException e) {
                    Log.i (TAG, "Initial launching dial Service failed");
                }
            }

            ComponentName componentInfo = task.get (0).topActivity;
            if (componentInfo.getPackageName().equals (NETFLIX_PKG_NAME) ) {
                //Log.i (TAG, "netflix running as top activity");
                return true;
            }
        }
        return false;
    }

    class ObserverThread extends Thread {
        public ObserverThread (String name) {
            super (name);
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep (1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                boolean fg = netflixActivityRunning();
                if (fg ^ mIsNetflixFg) {
                    Log.i (TAG, "Netflix status changed from " + (mIsNetflixFg?"fg":"bg")+ " -> " + (fg?"fg":"bg"));

                    mIsNetflixFg = fg;
                    Intent intent = new Intent();
                    intent.setAction (NETFLIX_STATUS_CHANGE);
                    intent.putExtra ("status", fg ? 1 : 0);
                    intent.putExtra ("pid", fg?getNetflixPid():-1);
                    mContext.sendBroadcast (intent);
                }

                if (SystemProperties.getBoolean ("sys.display-size.check", true)) {
                    try {
                        Scanner sc = new Scanner (new File(VIDEO_SIZE_DEVICE));
                        if (sc.hasNext("\\d+x\\d+")) {
                            String[] parts = sc.nextLine().split ("x");
                            int w = Integer.parseInt (parts[0]);
                            int h = Integer.parseInt (parts[1]);
                            //Log.i(TAG, "Video resolution: " + w + "x" + h);

                            String prop = SystemProperties.get ("sys.display-size", "0x0");
                            String[] parts_prop = prop.split ("x");
                            int wd = Integer.parseInt (parts_prop[0]);
                            int wh = Integer.parseInt (parts_prop[1]);

                            if ((w != wd) || (h != wh)) {
                                SystemProperties.set ("sys.display-size", String.format("%dx%d", w, h));
                                //Log.i(TAG, "set sys.display-size property to " + String.format("%dx$d", w, h));
                            }
                        } else {
                            //Log.i(TAG, "Video resolution no pattern found" + sc.nextLine());
                        }
                        sc.close();

                    } catch (Exception e) {
                        Log.i(TAG, "Error parsing video size device node");
                    }
                }
            }
        }
    }
}


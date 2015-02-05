package com.droidlogic.app;

import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.util.Log;
import java.lang.Integer;
import java.lang.Thread;
import java.io.IOException;

import com.droidlogic.SubTitleService.ISubTitleService;

public class SubtitleManager{
    private String TAG = "SubtitleManager";
    private MediaPlayer mMediaPlayer = null;
    private ISubTitleService mService = null;
    private EventHandler mEventHandler = null;
    private boolean mInvokeFromMp = false;
    private boolean mThreadStop = false;
    private String mPath;
    private Thread mThread = null;

    public SubtitleManager(MediaPlayer mp) {
        /*Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(looper);
        } else {
            mEventHandler = null;
        }*/
        mMediaPlayer = mp;
    }

    public void setInvokeFromMp(boolean fromMediaPlayer) {
        mInvokeFromMp = fromMediaPlayer;
    }

    public boolean getInvokeFromMp() {
        return mInvokeFromMp;
    }

    public void setSource(Context context, Uri uri) {
        if (context == null) {
            return;
        }

        if (uri == null) {
            return;
        }

        getService();

        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals("file")) {
            mPath = uri.getPath();
            return;
        }

        try {
            ContentResolver resolver = context.getContentResolver();
            //add for subtitle service
            String mediaStorePath = uri.getPath();
            String[] cols = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA
            };

            if (scheme.equals("content")) {
                int idx_check = (uri.toString()).indexOf("media/external/video/media");
                if (idx_check > -1) {
                    int idx = mediaStorePath.lastIndexOf("/");
                    String idStr = mediaStorePath.substring(idx+1);
                    int id = Integer.parseInt(idStr);
                    if (debug()) Log.i(TAG,"[setDataSource]id:"+id);
                    String where = MediaStore.Video.Media._ID + "=" + id;
                    Cursor cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,cols, where , null, null);
                    if (cursor != null && cursor.getCount() == 1) {
                        int colidx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                        cursor.moveToFirst();
                        mPath = cursor.getString(colidx);
                        if (debug()) Log.i(TAG,"[setDataSource]mediaStorePath:"+mediaStorePath+",mPath:"+mPath);
                    }
                }
                else {
                    mPath = null;
                }
            }
            else {
                mPath = null;
            }
        }catch (SecurityException ex) {
        } finally {
        }
    }

    public void setSource(String path) {
        if (path == null) {
            return;
        }

        getService();

        final Uri uri = Uri.parse(path);
        if ("file".equals(uri.getScheme())) {
            path = uri.getPath();
        }

        mPath = path;
    }

    private int open(String path) {
        if (disable()) {
            return -1;
        }
        if (debug()) Log.i(TAG,"[open] path:"+path);

        if (path.startsWith("/data/") || path.equals("")) {
            return -1;
        }

        try {
            if (mService != null) {
                mService.open(path);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    public void openIdx(int idx) {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[openIdx] idx:"+idx);

        if (idx < 0) {
            return;
        }

        try {
            if (mService != null) {
                mService.openIdx(idx);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[start] mEventHandler:"+mEventHandler+",mPath:"+mPath);

        /*
        if (mEventHandler != null) {
            mThreadStop = false;
            mEventHandler.removeMessages(AML_SUBTITLE_START);
            Message m = mEventHandler.obtainMessage(AML_SUBTITLE_START);
            mEventHandler.sendMessageDelayed(m, 500);
        }*/

        mThreadStop = false;
        if (mPath != null) {
            int ret = open(mPath);
            if (ret == 0) {
                show();
                if (optionEnable()) {
                    option();//show subtitle select option add for debug
                }
            }
        }
    }

    public void close() {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[close]");

        /*
        if (mEventHandler != null) {
            mEventHandler.removeMessages(AML_SUBTITLE_START);
        }*/

        if (mThread != null) {
            mThreadStop = true;
            mThread = null;
        }

        try {
            if (mService != null ) {
                mService.close();
                mService = null;
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void show() {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[show] total:"+total());

        if (total() > 0) {
            if (debug()) Log.i(TAG,"[start show]mThread:"+mThread);
            if (mThread == null) {
                mThread = new Thread(runnable);
                mThread.start();
            }
        }
    }

    public void option() {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[option] ");

        try {
            if (mService != null) {
                mService.option();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public int total() {
        if (disable()) {
            return 0;
        }
        if (debug()) Log.i(TAG,"[total] ");

        int ret = 0;
        try {
            if (mService != null) {
                ret = mService.getSubTotal();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if (debug()) Log.i(TAG,"[total] ret:"+ret);
        return ret;
    }

    public void next() {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[next] ");

        try {
            if (mService != null) {
                mService.nextSub();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void previous() {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[previous] ");

        try {
            if (mService != null) {
                mService.preSub();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void hide() {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[hide]");

        try {
            if (mService != null) {
                mService.hide();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void display() {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[display]");

        try {
            if (mService != null) {
                mService.display();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void clear() {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[clear]");

        try {
            if (mService != null) {
                mService.clear();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetForSeek() {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[resetForSeek]");

        try {
            if (mService != null) {
                mService.resetForSeek();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public int getSubType() {
        if (disable()) {
            return -1;
        }
        if (debug()) Log.i(TAG,"[getSubType] ");

        int ret = 0;
        try {
            if (mService != null) {
                ret = mService.getSubType();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if (debug()) Log.i(TAG,"[getSubType] ret:"+ret);
        return ret;
    }

    public String getSubName(int idx) {
        if (disable()) {
            return null;
        }
        if (debug()) Log.i(TAG,"[getSubName]");

        String name = null;
        try {
            if (mService != null) {
                name = mService.getSubName(idx);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if (debug()) Log.i(TAG,"[getSubName] name["+idx+"]:"+name);
        return name;
    }

    public String getSubLanguage(int idx) {
        if (disable()) {
            return null;
        }
        if (debug()) Log.i(TAG,"[getSubLanguage]");

        String language = null;
        try {
            if (mService != null) {
                language = mService.getSubLanguage(idx);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if (debug()) Log.i(TAG,"[getSubLanguage] language["+idx+"]:"+language);
        return language;
    }

    public String getCurName() {
        if (disable()) {
            return null;
        }
        if (debug()) Log.i(TAG,"[getCurName]");

        String name = null;
        try {
            if (mService != null) {
                name = mService.getCurName();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if (debug()) Log.i(TAG,"[getCurName] name:"+name);
        return name;
    }

    private void getService() {
        if (disable()) {
            return;
        }

        if (mService == null) {
            IBinder b = ServiceManager.getService("subtitle_service"/*Context.SUBTITLE_SERVICE*/);
            mService = ISubTitleService.Stub.asInterface(b);
        }
        if (debug()) Log.i(TAG,"[getService] mService:"+mService);
    }

    public void release() {
        if (disable()) {
            return;
        }

        if (debug()) Log.i(TAG,"[release] ");
        close();
    }

    public void setTextColor(int color) {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[setTextColor] color:"+color);

        try {
            if (mService != null) {
                mService.setTextColor(color);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setTextSize(int size) {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[setTextSize] size:"+size);

        try {
            if (mService != null) {
                mService.setTextSize(size);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setGravity(int gravity) {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[setGravity] gravity:"+gravity);

        try {
            if (mService != null) {
                mService.setGravity(gravity);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setTextStyle(int style) {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[setTextStyle] style:"+style);

        try {
            if (mService != null) {
                mService.setTextStyle(style);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPosHeight(int height) {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[setPosHeight] height:"+height);

        try {
            if (mService != null) {
                mService.setPosHeight(height);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setImgSubRatio(float ratioW, float ratioH, int maxW, int maxH) {
        if (disable()) {
            return;
        }
        if (debug()) Log.i(TAG,"[setImgSubRatio] ratioW:" + ratioW + ", ratioH:" + ratioH + ",maxW:" + maxW + ",maxH:" + maxH);

        try {
            if (mService != null) {
                mService.setImgSubRatio(ratioW, ratioH, maxW, maxH);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean debug() {
        boolean ret = false;
        if (SystemProperties.getBoolean("sys.subtitle.debug", false)) {
            ret = true;
        }
        return ret;
    }

    private boolean disable() {
        boolean ret = false;
        if (SystemProperties.getBoolean("sys.subtitle.disable", false)) {
            ret = true;
        }
        return ret;
    }

    private boolean optionEnable() {
        boolean ret = false;
        if (SystemProperties.getBoolean("sys.subtitleOption.enable", false)) {
            ret = true;
        }
        return ret;
    }

    private static final int AML_SUBTITLE_START = 800; // random value
    private class EventHandler extends Handler {
        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case AML_SUBTITLE_START:
                    if (debug()) Log.i(TAG,"[handleMessage]AML_SUBTITLE_START mPath:"+mPath);
                    if (mPath != null) {
                        int ret = open(mPath);
                        if (ret == 0) {
                            show();
                            if (optionEnable()) {
                                option();//show subtitle select option add for debug
                            }
                        }
                    }
                break;
            }
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            int pos = 0;
            while (!mThreadStop) {
                if (disable()) {
                    mThreadStop = true;
                    break;
                }

                if (debug()) Log.i(TAG,"[runnable]showSub mService:"+mService);

                //show subtitle
                try {
                    if (mService != null) {
                        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                            pos = mMediaPlayer.getCurrentPosition();
                            if (debug()) Log.i(TAG,"[runnable]showSub:"+pos);
                        }
                        mService.showSub(pos);
                    }
                    else {
                        mThreadStop = true;
                        break;
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }

                try {
                    Thread.sleep(1000 - (pos % 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    };
}

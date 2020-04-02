package com.android.internal.custom.longshot.injector;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import android.view.InputDevice;

public class ScreenshotHelperInjector {
    private static final String OP_SCREENSHOT_PACKAGE = "com.oneplus.screenshot";
    private static final String OP_SCREENSHOT_SERVICE = "com.oneplus.screenshot.TakeScreenshotService";
    private static final int SCREENSHOT_TIMEOUT_MS = 10000;
    private static final String SYSUI_PACKAGE = "com.android.systemui";
    private static final String SYSUI_SCREENSHOT_ERROR_RECEIVER = "com.android.systemui.screenshot.ScreenshotServiceErrorReceiver";
    private static final String TAG = "OPScreenshotHelper";
    public Context mContext;
    private Handler mHandler;
    final Runnable mLongshotTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    ServiceConnection unused = mScreenshotConnection = null;
                    notifyScreenshotError();
                }
            }
        }
    };
    public ServiceConnection mScreenshotConnection;
    public final Object mScreenshotLock = new Object();

    public ScreenshotHelperInjector(Context context) {
        mContext = context;
    }

    public void takeScreenshot(final int screenshotType, final boolean hasStatus,
            final boolean hasNav, @NonNull Handler handler,
            final boolean isLongshot, final Bundle screenshotBundle) {
        if (screenshotType != WindowManager.TAKE_SCREENSHOT_FULLSCREEN){
            takeScreenshot(screenshotType, hasStatus, hasNav, SCREENSHOT_TIMEOUT_MS, handler,
                null);
            return;
        }
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }
            mHandler = handler;
            final Intent serviceIntent = new Intent();
            serviceIntent.setComponent(LongScreenshotManagerService.COMPONENT_LONGSHOT);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, screenshotType);
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(handler.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (msg.what == 2) {
                                        handler.removeCallbacks(mLongshotTimeout);
                                    } else if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        handler.removeCallbacks(mLongshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = hasStatus ? 1: 0;
                        msg.arg2 = hasNav ? 1: 0;
                        msg.obj = screenshotBundle;
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Couldn't take screenshot: " + e);
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != null) {
                            mContext.unbindService(mScreenshotConnection);
                            mScreenshotConnection = null;
                            handler.removeCallbacks(mLongshotTimeout);
                            notifyScreenshotError();
                        }
                    }
                }
            };
            if (mContext.bindServiceAsUser(serviceIntent, conn,
                    Context.BIND_AUTO_CREATE | Context.BIND_FOREGROUND_SERVICE_WHILE_AWAKE,
                    UserHandle.CURRENT)) {
                mScreenshotConnection = conn;
                if (isLongshot) {
                    handler.postDelayed(mLongshotTimeout, 120000);
                } else {
                    handler.postDelayed(mLongshotTimeout, SCREENSHOT_TIMEOUT_MS);
                }
            }
        }
    }

    public void stopLongshotConnection() {
        synchronized (mScreenshotLock) {
            Slog.d(TAG, "OPLongshot stopLongshotConnection" + mScreenshotLock);
            if (mScreenshotConnection != null) {
                mContext.unbindService(mScreenshotConnection);
                mScreenshotConnection = null;
                mHandler.removeCallbacks(mLongshotTimeout);
            }
        }
    }

    public void notifyScreenshotError() {
        ComponentName errorComponent = new ComponentName("com.android.systemui", SYSUI_SCREENSHOT_ERROR_RECEIVER);
        Intent errorIntent = new Intent(Intent.ACTION_USER_PRESENT);
        errorIntent.setComponent(errorComponent);
        errorIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT |
                Intent.FLAG_RECEIVER_FOREGROUND);
        mContext.sendBroadcastAsUser(errorIntent, UserHandle.CURRENT);
    }
}

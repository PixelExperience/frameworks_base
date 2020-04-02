package com.android.internal.custom.longshot;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import java.util.ArrayList;
import java.util.List;

public class OpLongScreenshotManagerService extends ILongScreenshotManager.Stub {
    public static final String OP_SCREENSHOT_PACKAGE = "com.oneplus.screenshot";
    public static final ComponentName COMPONENT_LONGSHOT = new ComponentName(OP_SCREENSHOT_PACKAGE, OP_SCREENSHOT_PACKAGE + ".LongshotService");
    private static final String TAG = "Longshot.ManagerService";
    private static OpLongScreenshotManagerService sInstance = null;
    private Context mContext = null;
    private LongshotConnection mLongshot = new LongshotConnection();

    private class LongshotConnection extends ILongScreenshotCallback.Stub implements ServiceConnection {
        private List<ILongScreenshotListener> mListeners;
        private ILongScreenshot mService;

        private LongshotConnection() {
            mService = null;
            mListeners = new ArrayList();
        }

        @Override
        public void notifyMove() {
            Slog.d(TAG, "notifyMove");
            synchronized (mListeners) {
                for (ILongScreenshotListener onMove : mListeners) {
                    try {
                        onMove.onMove();
                    } catch (RemoteException e) {
                        Slog.e(TAG, e.toString());
                    }
                }
            }
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Slog.d(TAG, "onServiceConnected : " + componentName);
            mService = ILongScreenshot.Stub.asInterface(iBinder);
            try {
                mService.start(this);
                IWindowManager windowManagerService = WindowManagerGlobal.getWindowManagerService();
                if (windowManagerService != null) {
                    windowManagerService.onLongshotStart();
                }
            } catch (NullPointerException unused) {
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Slog.d(TAG, "onServiceDisconnected");
            stop();
        }

        public void registerListener(ILongScreenshotListener iLongScreenshotListener) {
            synchronized (mListeners) {
                mListeners.add(iLongScreenshotListener);
            }
        }

        @Override
        public void stop() {
            Slog.d(TAG, "stop");
            mContext.unbindService(this);
            mService = null;
            try {
                WindowManagerGlobal.getWindowManagerService().stopLongshotConnection();
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }

        public void unregisterListener(ILongScreenshotListener iLongScreenshotListener) {
            synchronized (mListeners) {
                mListeners.remove(iLongScreenshotListener);
            }
        }
    }

    private OpLongScreenshotManagerService(Context context) {
        mContext = context;
    }

    private boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        if (intent != null && serviceConnection != null) {
            return mContext.bindServiceAsUser(intent, serviceConnection, i, UserHandle.CURRENT);
        }
        Slog.e(TAG, "--- bind failed: service = " + intent + ", conn = " + serviceConnection);
        return false;
    }

    private Intent createIntent(ComponentName componentName) {
        return new Intent().setComponent(componentName);
    }

    private Intent createLongshotIntent(boolean z, boolean z2) {
        return createIntent(COMPONENT_LONGSHOT).putExtra("statusbar_visible", z).putExtra("navigationbar_visible", z2);
    }

    public static OpLongScreenshotManagerService getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new OpLongScreenshotManagerService(context);
        }
        return sInstance;
    }

    @Override
    public boolean isLongshotHandleState() {
        try {
            return mLongshot.mService.isHandleState();
        } catch (NullPointerException unused) {
            return false;
        } catch (RemoteException e) {
            Slog.e(TAG, e.toString());
            return false;
        }
    }

    @Override
    public boolean isLongshotMode() {
        return mLongshot.mService != null;
    }

    @Override
    public boolean isLongshotMoveState() {
        try {
            return mLongshot.mService.isMoveState();
        } catch (NullPointerException unused) {
            return false;
        } catch (RemoteException e) {
            Slog.e(TAG, e.toString());
            return false;
        }
    }

    @Override
    public void notifyLongshotScroll(boolean z) {
        try {
            mLongshot.mService.notifyScroll(z);
        } catch (NullPointerException unused) {
        } catch (RemoteException e) {
            Slog.e(TAG, e.toString());
        }
    }

    @Override
    public void notifyLongshotScrollChanged(int i, int i2, int i3, int i4) {
        try {
            Slog.d(TAG, "notifyLongshotScrollChanged y:" + i2 + " oldy:" + i4);
            if (mLongshot != null && mLongshot.mService != null) {
                mLongshot.mService.notifyLongshotScrollChanged(i, i2, i3, i4);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, e.toString());
        }
    }

    @Override
    public void notifyScrollViewSearchComplete(int i, int i2, int i3) {
        try {
            if (mLongshot != null && mLongshot.mService != null) {
                mLongshot.mService.notifyScrollViewSearchComplete(i, i2, i3);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, e.toString());
        }
    }

    @Override
    public void notifyScrollViewTop(int i) {
        try {
            mLongshot.mService.notifyScrollViewTop(i);
        } catch (NullPointerException unused) {
        } catch (RemoteException e) {
            Slog.e(TAG, e.toString());
        }
    }

    @Override
    public void notifyWindowLayerChange(IBinder iBinder) {
        try {
            Slog.d(TAG, "notifyWindowLayerChange windowToken:" + iBinder);
            if (mLongshot != null && mLongshot.mService != null) {
                mLongshot.mService.notifyWindowLayerChange(iBinder);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUnscrollableView() {
        try {
            Slog.d(TAG, "onUnscrollableView");
            mLongshot.mService.onUnscrollableView();
        } catch (NullPointerException unused) {
        } catch (RemoteException e) {
            Slog.e(TAG, e.toString());
        }
    }

    @Override
    public void registerLongshotListener(ILongScreenshotListener iLongScreenshotListener) {
        mLongshot.registerListener(iLongScreenshotListener);
    }

    @Override
    public void stopLongshot(int i) {
        if (mLongshot.mService != null) {
            Slog.d(TAG, "stopLongshot");
            try {
                mLongshot.mService.stopLongshot(i);
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }
    }

    @Override
    public void takeLongshot(boolean z, boolean z2) {
        stopLongshot(1);
        boolean bindService = bindService(createLongshotIntent(z, z2), mLongshot, 1);
        Slog.i(TAG, "start : bindService=" + bindService + ", " + UserHandle.CURRENT);
    }

    @Override
    public void unregisterLongshotListener(ILongScreenshotListener iLongScreenshotListener) {
        mLongshot.unregisterListener(iLongScreenshotListener);
    }
}

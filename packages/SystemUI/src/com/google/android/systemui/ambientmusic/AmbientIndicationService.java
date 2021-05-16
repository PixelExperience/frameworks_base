package com.google.android.systemui.ambientmusic;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dependency;
import java.util.Objects;

public class AmbientIndicationService extends BroadcastReceiver {
    private final static String TAG = "AmbientIndicationService";
    private final AlarmManager mAlarmManager;
    private final AmbientIndicationContainer mAmbientIndicationContainer;
    private final KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onUserSwitchComplete(int i) {
            onUserSwitched();
        }
    };
    private final Context mContext;
    private final AlarmManager.OnAlarmListener mHideIndicationListener;

    public AmbientIndicationService(Context context, AmbientIndicationContainer ambientIndicationContainer) {
        mContext = context;
        mAmbientIndicationContainer = ambientIndicationContainer;
        Objects.requireNonNull(mAmbientIndicationContainer);
        mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        mHideIndicationListener = new AlarmManager.OnAlarmListener() {
            @Override
            public final void onAlarm() {
                mAmbientIndicationContainer.hideAmbientMusic();
            }
        };
        start();
    }

    private void start() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.google.android.ambientindication.action.AMBIENT_INDICATION_SHOW");
        intentFilter.addAction("com.google.android.ambientindication.action.AMBIENT_INDICATION_HIDE");
        mContext.registerReceiverAsUser(this, UserHandle.ALL, intentFilter, "com.google.android.ambientindication.permission.AMBIENT_INDICATION", null);
        ((KeyguardUpdateMonitor) Dependency.get(KeyguardUpdateMonitor.class)).registerCallback(mCallback);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isForCurrentUser()) {
            Log.i(TAG, "Suppressing ambient, not for this user.");
        } else if (verifyAmbientApiVersion(intent)) {
            if (mAmbientIndicationContainer.isMediaPlaying()) {
                Log.i(TAG, "Suppressing ambient intent due to media playback.");
                return;
            }
            boolean shouldShow = false;
            String action = intent.getAction();
            int hashCode = action.hashCode();
            if (hashCode != -1032272569) {
                if (hashCode == -1031945470 && action.equals("com.google.android.ambientindication.action.AMBIENT_INDICATION_SHOW")) {
                    shouldShow = true;
                }
            } else if (action.equals("com.google.android.ambientindication.action.AMBIENT_INDICATION_HIDE")) {
                shouldShow = false;
            }
            if (shouldShow) {
                long min = Math.min(Math.max(intent.getLongExtra("com.google.android.ambientindication.extra.TTL_MILLIS", 180000), 0L), 180000L);
                mAmbientIndicationContainer.setAmbientMusic(intent.getCharSequenceExtra("com.google.android.ambientindication.extra.TEXT"), (PendingIntent) intent.getParcelableExtra("com.google.android.ambientindication.extra.OPEN_INTENT"), intent.getBooleanExtra("com.google.android.ambientindication.extra.SKIP_UNLOCK", false));
                mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + min, TAG, mHideIndicationListener, null);
                Log.i(TAG, "Showing ambient indication.");
            } else {
                mAlarmManager.cancel(mHideIndicationListener);
                mAmbientIndicationContainer.hideAmbientMusic();
                Log.i(TAG, "Hiding ambient indication.");
            }
        }
    }

    private boolean verifyAmbientApiVersion(Intent intent) {
        int intExtra = intent.getIntExtra("com.google.android.ambientindication.extra.VERSION", 0);
        if (intExtra == 1) {
            return true;
        }
        Log.e(TAG, "AmbientIndicationApi.EXTRA_VERSION is " + 1 + ", but received an intent with version " + intExtra + ", dropping intent.");
        return false;
    }

    private boolean isForCurrentUser() {
        return getSendingUserId() == getCurrentUser() || getSendingUserId() == -1;
    }

    private int getCurrentUser() {
        return KeyguardUpdateMonitor.getCurrentUser();
    }

    private void onUserSwitched() {
        mAmbientIndicationContainer.hideAmbientMusic();
    }
}

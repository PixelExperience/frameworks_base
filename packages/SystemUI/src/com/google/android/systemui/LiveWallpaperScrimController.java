package com.google.android.systemui;

import android.annotation.Nullable;
import android.app.AlarmManager;
import android.app.IWallpaperManager;
import android.os.Handler;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.dock.DockManager;
import com.android.systemui.statusbar.BlurUtils;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.LockscreenWallpaper;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.util.wakelock.DelayedWakeLock;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LiveWallpaperScrimController extends ScrimController {
    @Inject
    public LiveWallpaperScrimController(LightBarController lightBarController, DozeParameters dozeParameters, AlarmManager alarmManager, KeyguardStateController keyguardStateController, DelayedWakeLock.Builder builder, Handler handler, @Nullable IWallpaperManager iWallpaperManager, LockscreenWallpaper lockscreenWallpaper, KeyguardUpdateMonitor keyguardUpdateMonitor, SysuiColorExtractor sysuiColorExtractor, DockManager dockManager, BlurUtils blurUtils) {
        super(lightBarController, dozeParameters, alarmManager, keyguardStateController, builder, handler, keyguardUpdateMonitor, sysuiColorExtractor, dockManager, blurUtils);
    }
}

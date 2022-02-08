package org.pixelexperience.systemui;

import static com.android.systemui.DejankUtils.whitelistIpcs;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.IWallpaperManager;
import android.app.WallpaperInfo;
import android.content.ComponentName;
import android.os.Handler;
import android.os.RemoteException;
import android.util.ArraySet;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dock.DockManager;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.LockscreenWallpaper;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.ScrimState;
import com.android.systemui.statusbar.phone.UnlockedScreenOffAnimationController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.util.wakelock.DelayedWakeLock;
import com.google.android.collect.Sets;

import java.util.concurrent.Executor;

import javax.inject.Inject;

@SysUISingleton
public class LiveWallpaperScrimController extends ScrimController {
    private static final ArraySet<ComponentName> REDUCED_SCRIM_WALLPAPERS = Sets.newArraySet(new ComponentName("com.breel.geswallpapers", "com.breel.geswallpapers.wallpapers.EarthWallpaperService"), new ComponentName("com.breel.wallpapers18", "com.breel.wallpapers18.delight.wallpapers.DelightWallpaperV1"), new ComponentName("com.breel.wallpapers18", "com.breel.wallpapers18.delight.wallpapers.DelightWallpaperV2"), new ComponentName("com.breel.wallpapers18", "com.breel.wallpapers18.delight.wallpapers.DelightWallpaperV3"), new ComponentName("com.breel.wallpapers18", "com.breel.wallpapers18.surfandturf.wallpapers.variations.SurfAndTurfWallpaperV2"), new ComponentName("com.breel.wallpapers18", "com.breel.wallpapers18.cities.wallpapers.variations.SanFranciscoWallpaper"), new ComponentName("com.breel.wallpapers18", "com.breel.wallpapers18.cities.wallpapers.variations.NewYorkWallpaper"));
    private final LockscreenWallpaper mLockscreenWallpaper;
    private final IWallpaperManager mWallpaperManager;
    private int mCurrentUser = ActivityManager.getCurrentUser();

    @Inject
    public LiveWallpaperScrimController(LightBarController lightBarController, DozeParameters dozeParameters,
            AlarmManager alarmManager, KeyguardStateController keyguardStateController,
            DelayedWakeLock.Builder delayedWakeLockBuilder, Handler handler,
            KeyguardUpdateMonitor keyguardUpdateMonitor, DockManager dockManager,
            ConfigurationController configurationController, @Main Executor mainExecutor,
            UnlockedScreenOffAnimationController unlockedScreenOffAnimationController,
            @Nullable IWallpaperManager wallpaperManager, LockscreenWallpaper lockscreenWallpaper) {
        super(lightBarController, dozeParameters, alarmManager, keyguardStateController, delayedWakeLockBuilder,
            handler, keyguardUpdateMonitor, dockManager, configurationController, mainExecutor,
            unlockedScreenOffAnimationController);
        mWallpaperManager = wallpaperManager;
        mLockscreenWallpaper = lockscreenWallpaper;
    }

    @Override
    public void transitionTo(ScrimState scrimState) {
        if (scrimState == ScrimState.KEYGUARD) {
            updateScrimValues();
        }
        super.transitionTo(scrimState);
    }

    private void updateScrimValues() {
        boolean reducedScrim = whitelistIpcs(() -> isReducedScrimWallpaperSet());
        if (isReducedScrimWallpaperSet()) {
            setScrimBehindValues(0.25f);
        } else {
            setScrimBehindValues(0.2f);
        }
    }

    @Override
    public void setCurrentUser(int i) {
        mCurrentUser = i;
        updateScrimValues();
    }

    private boolean isReducedScrimWallpaperSet() {
        try {
            WallpaperInfo wallpaperInfo = mWallpaperManager.getWallpaperInfo(mCurrentUser);
            if (wallpaperInfo != null && REDUCED_SCRIM_WALLPAPERS.contains(wallpaperInfo.getComponent())) {
                return mLockscreenWallpaper.getBitmap() == null;
            }
        } catch (RemoteException unused) {
        }
        return false;
    }
}
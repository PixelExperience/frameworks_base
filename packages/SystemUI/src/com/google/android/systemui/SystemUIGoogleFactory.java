package com.google.android.systemui;

import android.app.AlarmManager;
import android.content.Context;
import android.util.ArrayMap;

import com.android.internal.colorextraction.ColorExtractor.GradientColors;
import com.android.internal.util.function.TriConsumer;

import com.android.systemui.SystemUIFactory;
import com.android.systemui.Dependency.DependencyProvider;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.ScrimState;
import com.android.systemui.statusbar.phone.LockscreenWallpaper;

import java.util.function.Consumer;

import com.android.systemui.statusbar.NotificationEntryManager;

import com.google.android.systemui.statusbar.NotificationEntryManagerGoogle;

public class SystemUIGoogleFactory extends SystemUIFactory {

    @Override
    public void injectDependencies(ArrayMap<Object, DependencyProvider> providers,
            Context context) {
        super.injectDependencies(providers, context);
        providers.put(NotificationEntryManager.class,
                () -> new NotificationEntryManagerGoogle(context));
        providers.put(NotificationLockscreenUserManager.class,
                () -> new NotificationLockscreenUserManagerGoogle(context));
    }

    @Override
    public ScrimController createScrimController(ScrimView scrimBehind, ScrimView scrimInFront,
            LockscreenWallpaper lockscreenWallpaper,
            TriConsumer<ScrimState, Float, GradientColors> scrimStateListener,
            Consumer<Integer> scrimVisibleListener, DozeParameters dozeParameters,
            AlarmManager alarmManager) {
        return new LiveWallpaperScrimController(scrimBehind, scrimInFront, lockscreenWallpaper,
                scrimStateListener, scrimVisibleListener, dozeParameters, alarmManager);
    }
}

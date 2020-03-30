package com.google.android.systemui;

import com.android.systemui.power.EnhancedEstimates;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;

import com.google.android.systemui.NotificationLockscreenUserManagerGoogle;
import com.google.android.systemui.power.EnhancedEstimatesGoogleImpl;

import dagger.Binds;
import dagger.Module;

/**
 * A dagger module for injecting default implementations of components of System UI that may be
 * overridden by the System UI implementation.
 */
@Module
public abstract class SystemUIGoogleModule {

    @Binds
    abstract EnhancedEstimates bindEnhancedEstimates(
        EnhancedEstimatesGoogleImpl enhancedEstimates);

    @Binds
    abstract NotificationLockscreenUserManager bindNotificationLockscreenUserManager(
        NotificationLockscreenUserManagerGoogle notificationLockscreenUserManager);
}

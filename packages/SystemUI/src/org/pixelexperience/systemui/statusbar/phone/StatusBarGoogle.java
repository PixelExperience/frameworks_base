/*
 * Copyright (C) 2021 The Pixel Experience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pixelexperience.systemui.statusbar.phone;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.InitController;
import com.android.systemui.R;
import com.android.systemui.accessibility.floatingmenu.AccessibilityFloatingMenuController;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.classifier.FalsingCollector;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.demomode.DemoModeController;
import com.android.systemui.dock.DockManager;
import com.android.systemui.keyguard.DismissCallbackRegistry;
import com.android.systemui.keyguard.KeyguardUnlockAnimationController;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.navigationbar.NavigationBarController;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.PluginDependencyProvider;
import com.android.systemui.recents.ScreenPinningRequest;
import com.android.systemui.settings.brightness.BrightnessSlider;
import com.android.systemui.shared.plugins.PluginManager;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.FeatureFlags;
import com.android.systemui.statusbar.LockscreenShadeTransitionController;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.NotificationShadeDepthController;
import com.android.systemui.statusbar.NotificationShadeWindowController;
import com.android.systemui.statusbar.NotificationViewHierarchyManager;
import com.android.systemui.statusbar.PulseExpansionHandler;
import com.android.systemui.statusbar.SuperStatusBarViewFactory;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.VibratorHelper;
import com.android.systemui.statusbar.charging.WiredChargingRippleController;
import com.android.systemui.statusbar.events.SystemStatusAnimationScheduler;
import com.android.systemui.statusbar.notification.DynamicPrivacyController;
import com.android.systemui.statusbar.notification.NotificationWakeUpCoordinator;
import com.android.systemui.statusbar.notification.collection.legacy.VisualStabilityManager;
import com.android.systemui.statusbar.notification.init.NotificationsController;
import com.android.systemui.statusbar.notification.interruption.BypassHeadsUpNotifier;
import com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProvider;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.notification.row.NotificationGutsManager;
import com.android.systemui.statusbar.phone.AutoHideController;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.phone.DozeScrimController;
import com.android.systemui.statusbar.phone.DozeServiceHost;
import com.android.systemui.statusbar.phone.HeadsUpManagerPhone;
import com.android.systemui.statusbar.phone.KeyguardBypassController;
import com.android.systemui.statusbar.phone.KeyguardDismissUtil;
import com.android.systemui.statusbar.phone.KeyguardLiftController;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.LightsOutNotifController;
import com.android.systemui.statusbar.phone.LockscreenWallpaper;
import com.android.systemui.statusbar.phone.NotificationIconAreaController;
import com.android.systemui.statusbar.phone.PhoneStatusBarPolicy;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.StatusBarLocationPublisher;
import com.android.systemui.statusbar.phone.StatusBarNotificationActivityStarter;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy;
import com.android.systemui.statusbar.phone.StatusBarTouchableRegionManager;
import com.android.systemui.statusbar.phone.UnlockedScreenOffAnimationController;
import com.android.systemui.statusbar.phone.dagger.StatusBarComponent;
import com.android.systemui.statusbar.phone.ongoingcall.OngoingCallController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.RemoteInputQuickSettingsDisabler;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.volume.VolumeComponent;
import com.android.systemui.wmshell.BubblesManager;
import com.android.wm.shell.bubbles.Bubbles;
import com.android.wm.shell.legacysplitscreen.LegacySplitScreen;
import com.android.wm.shell.startingsurface.StartingSurface;
import com.google.android.systemui.LiveWallpaperScrimController;
import com.google.android.systemui.NotificationLockscreenUserManagerGoogle;
import com.google.android.systemui.dreamliner.DockIndicationController;
import com.google.android.systemui.dreamliner.DockObserver;
import com.google.android.systemui.reversecharging.ReverseChargingViewController;
import com.google.android.systemui.smartspace.SmartSpaceController;
import com.google.android.systemui.statusbar.KeyguardIndicationControllerGoogle;
import com.google.android.systemui.statusbar.notification.voicereplies.NotificationVoiceReplyClient;
import com.google.android.systemui.statusbar.phone.WallpaperNotifier;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.concurrent.Executor;

import javax.inject.Provider;

import dagger.Lazy;

public class StatusBarGoogle extends StatusBar {
    public static final boolean DEBUG = Log.isLoggable("StatusBarGoogle", Log.DEBUG);
    private final KeyguardIndicationControllerGoogle mKeyguardIndicationController;
    private final SmartSpaceController mSmartSpaceController;
    private final SysuiStatusBarStateController mStatusBarStateController;
    private final Lazy<Optional<NotificationVoiceReplyClient>> mVoiceReplyClient;
    private final Optional<ReverseChargingViewController> mReverseChargingViewController;
    private final WallpaperNotifier mWallpaperNotifier;
    private long mAnimStartTime;
    private boolean mChargingAnimShown;
    private int mReceivingBatteryLevel;
    private boolean mReverseChargingAnimShown;

    public StatusBarGoogle(Context context,
                           NotificationsController notificationsController,
                           LightBarController lightBarController,
                           AutoHideController autoHideController,
                           KeyguardUpdateMonitor keyguardUpdateMonitor,
                           StatusBarSignalPolicy signalPolicy,
                           PulseExpansionHandler pulseExpansionHandler,
                           NotificationWakeUpCoordinator notificationWakeUpCoordinator,
                           KeyguardBypassController keyguardBypassController,
                           KeyguardStateController keyguardStateController,
                           HeadsUpManagerPhone headsUpManagerPhone,
                           DynamicPrivacyController dynamicPrivacyController,
                           BypassHeadsUpNotifier bypassHeadsUpNotifier,
                           FalsingManager falsingManager,
                           FalsingCollector falsingCollector,
                           BroadcastDispatcher broadcastDispatcher,
                           RemoteInputQuickSettingsDisabler remoteInputQuickSettingsDisabler,
                           NotificationGutsManager notificationGutsManager,
                           NotificationLogger notificationLogger,
                           NotificationInterruptStateProvider notificationInterruptStateProvider,
                           NotificationViewHierarchyManager notificationViewHierarchyManager,
                           KeyguardViewMediator keyguardViewMediator,
                           DisplayMetrics displayMetrics,
                           MetricsLogger metricsLogger,
                           Executor uiBgExecutor,
                           NotificationMediaManager notificationMediaManager,
                           NotificationLockscreenUserManager lockScreenUserManager,
                           NotificationRemoteInputManager remoteInputManager,
                           UserSwitcherController userSwitcherController,
                           NetworkController networkController,
                           BatteryController batteryController,
                           SysuiColorExtractor colorExtractor,
                           ScreenLifecycle screenLifecycle,
                           WakefulnessLifecycle wakefulnessLifecycle,
                           SysuiStatusBarStateController statusBarStateController,
                           VibratorHelper vibratorHelper,
                           Optional<BubblesManager> bubblesManagerOptional,
                           Optional<Bubbles> bubblesOptional,
                           VisualStabilityManager visualStabilityManager,
                           DeviceProvisionedController deviceProvisionedController,
                           NavigationBarController navigationBarController,
                           AccessibilityFloatingMenuController accessibilityFloatingMenuController,
                           Lazy<AssistManager> assistManagerLazy,
                           ConfigurationController configurationController,
                           NotificationShadeWindowController notificationShadeWindowController,
                           DozeParameters dozeParameters,
                           LiveWallpaperScrimController scrimController,
                           KeyguardLiftController keyguardLiftController,
                           Lazy<LockscreenWallpaper> lockscreenWallpaperLazy,
                           Lazy<BiometricUnlockController> biometricUnlockControllerLazy,
                           DozeServiceHost dozeServiceHost,
                           PowerManager powerManager,
                           ScreenPinningRequest screenPinningRequest,
                           DozeScrimController dozeScrimController,
                           VolumeComponent volumeComponent,
                           CommandQueue commandQueue,
                           Provider<StatusBarComponent.Builder> statusBarComponentBuilder,
                           PluginManager pluginManager,
                           Optional<LegacySplitScreen> splitScreenOptional,
                           LightsOutNotifController lightsOutNotifController,
                           StatusBarNotificationActivityStarter.Builder
                                   statusBarNotificationActivityStarterBuilder,
                           ShadeController shadeController,
                           SuperStatusBarViewFactory superStatusBarViewFactory,
                           StatusBarKeyguardViewManager statusBarKeyguardViewManager,
                           ViewMediatorCallback viewMediatorCallback,
                           InitController initController,
                           Handler timeTickHandler,
                           PluginDependencyProvider pluginDependencyProvider,
                           KeyguardDismissUtil keyguardDismissUtil,
                           ExtensionController extensionController,
                           UserInfoControllerImpl userInfoControllerImpl,
                           PhoneStatusBarPolicy phoneStatusBarPolicy,
                           KeyguardIndicationControllerGoogle keyguardIndicationController,
                           DismissCallbackRegistry dismissCallbackRegistry,
                           DemoModeController demoModeController,
                           Lazy<NotificationShadeDepthController> notificationShadeDepthControllerLazy,
                           StatusBarTouchableRegionManager statusBarTouchableRegionManager,
                           NotificationIconAreaController notificationIconAreaController,
                           BrightnessSlider.Factory brightnessSliderFactory,
                           WiredChargingRippleController chargingRippleAnimationController,
                           OngoingCallController ongoingCallController,
                           SystemStatusAnimationScheduler animationScheduler,
                           StatusBarLocationPublisher locationPublisher,
                           StatusBarIconController statusBarIconController,
                           LockscreenShadeTransitionController lockscreenShadeTransitionController,
                           FeatureFlags featureFlags,
                           KeyguardUnlockAnimationController keyguardUnlockAnimationController,
                           UnlockedScreenOffAnimationController unlockedScreenOffAnimationController,
                           Optional<StartingSurface> startingSurfaceOptional,
                           SmartSpaceController smartSpaceController,
                           WallpaperNotifier wallpaperNotifier,
                           Optional<ReverseChargingViewController> reverseChargingViewController,
                           Lazy<Optional<NotificationVoiceReplyClient>> notificationVoiceReplyClient) {
        super(context, notificationsController, lightBarController, autoHideController, keyguardUpdateMonitor,
                signalPolicy, pulseExpansionHandler, notificationWakeUpCoordinator, keyguardBypassController,
                keyguardStateController, headsUpManagerPhone, dynamicPrivacyController, bypassHeadsUpNotifier,
                falsingManager, falsingCollector, broadcastDispatcher, remoteInputQuickSettingsDisabler,
                notificationGutsManager, notificationLogger, notificationInterruptStateProvider,
                notificationViewHierarchyManager, keyguardViewMediator, displayMetrics, metricsLogger,
                uiBgExecutor, notificationMediaManager, lockScreenUserManager, remoteInputManager,
                userSwitcherController, networkController, batteryController, colorExtractor, screenLifecycle,
                wakefulnessLifecycle, statusBarStateController, vibratorHelper, bubblesManagerOptional,
                bubblesOptional, visualStabilityManager, deviceProvisionedController, navigationBarController,
                accessibilityFloatingMenuController, assistManagerLazy, configurationController,
                notificationShadeWindowController, dozeParameters, scrimController, keyguardLiftController,
                lockscreenWallpaperLazy, biometricUnlockControllerLazy, dozeServiceHost, powerManager,
                screenPinningRequest, dozeScrimController, volumeComponent, commandQueue, statusBarComponentBuilder,
                pluginManager, splitScreenOptional, lightsOutNotifController, statusBarNotificationActivityStarterBuilder,
                shadeController, superStatusBarViewFactory, statusBarKeyguardViewManager, viewMediatorCallback, initController,
                timeTickHandler, pluginDependencyProvider, keyguardDismissUtil, extensionController, userInfoControllerImpl,
                phoneStatusBarPolicy, keyguardIndicationController, dismissCallbackRegistry, demoModeController,
                notificationShadeDepthControllerLazy, statusBarTouchableRegionManager, notificationIconAreaController,
                brightnessSliderFactory, chargingRippleAnimationController, ongoingCallController, animationScheduler,
                locationPublisher, statusBarIconController, lockscreenShadeTransitionController, featureFlags,
                keyguardUnlockAnimationController, unlockedScreenOffAnimationController, startingSurfaceOptional);
        mSmartSpaceController = smartSpaceController;
        mWallpaperNotifier = wallpaperNotifier;
        mReverseChargingViewController = reverseChargingViewController;
        mVoiceReplyClient = notificationVoiceReplyClient;
        mKeyguardIndicationController = keyguardIndicationController;
        mKeyguardIndicationController.setStatusBar(this);
        mStatusBarStateController = statusBarStateController;
    }

    @Override
    public void start() {
        super.start();
        ((NotificationLockscreenUserManagerGoogle) Dependency.get(NotificationLockscreenUserManager.class)).updateSmartSpaceVisibilitySettings();
        DockObserver dockObserver = (DockObserver) Dependency.get(DockManager.class);
        dockObserver.setDreamlinerGear(mNotificationShadeWindowView.findViewById(R.id.dreamliner_gear));
        dockObserver.setPhotoPreview(mNotificationShadeWindowView.findViewById(R.id.photo_preview));
        dockObserver.setIndicationController(new DockIndicationController(mContext, mKeyguardIndicationController, mStatusBarStateController, this));
        dockObserver.registerDockAlignInfo();
        mReverseChargingViewController.ifPresent(ReverseChargingViewController::initialize);
        mWallpaperNotifier.attach();
        mVoiceReplyClient.get().ifPresent(NotificationVoiceReplyClient::startClient);
    }

    @Override
    public void setLockscreenUser(int i) {
        super.setLockscreenUser(i);
        mSmartSpaceController.reloadData();
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(fileDescriptor, printWriter, strArr);
        mSmartSpaceController.dump(fileDescriptor, printWriter, strArr);
    }

    @Override
    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        super.onBatteryLevelChanged(i, z, z2);
        mReceivingBatteryLevel = i;
        if (!mBatteryController.isWirelessCharging()) {
            if (SystemClock.uptimeMillis() - mAnimStartTime > 1500) {
                mChargingAnimShown = false;
            }
            mReverseChargingAnimShown = false;
        }
        if (DEBUG) {
            Log.d("StatusBarGoogle", "onBatteryLevelChanged(): level=" + i + ",wlc=" + (mBatteryController.isWirelessCharging() ? 1 : 0) + ",wlcs=" + mChargingAnimShown + ",rtxs=" + mReverseChargingAnimShown + ",this=" + this);
        }
    }

    @Override
    public void onReverseChanged(boolean z, int i, String str) {
        super.onReverseChanged(z, i, str);
        if (!z && i >= 0 && !TextUtils.isEmpty(str) && mBatteryController.isWirelessCharging() && mChargingAnimShown && !mReverseChargingAnimShown) {
            mReverseChargingAnimShown = true;
            long uptimeMillis = SystemClock.uptimeMillis() - mAnimStartTime;
            showChargingAnimation(mReceivingBatteryLevel, i, uptimeMillis > 1500 ? 0 : 1500 - uptimeMillis);
        }
        if (DEBUG) {
            Log.d("StatusBarGoogle", "onReverseChanged(): rtx=" + (z ? 1 : 0) + ",rxlevel=" + mReceivingBatteryLevel + ",level=" + i + ",name=" + str + ",wlc=" + (mBatteryController.isWirelessCharging() ? 1 : 0) + ",wlcs=" + mChargingAnimShown + ",rtxs=" + mReverseChargingAnimShown + ",this=" + this);
        }
    }

    @Override
    public void showWirelessChargingAnimation(int i) {
        if (DEBUG) {
            Log.d("StatusBarGoogle", "showWirelessChargingAnimation()");
        }
        mChargingAnimShown = true;
        super.showWirelessChargingAnimation(i);
        mAnimStartTime = SystemClock.uptimeMillis();
    }
}
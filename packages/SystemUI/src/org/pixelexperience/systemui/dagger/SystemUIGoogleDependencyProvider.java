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

package org.pixelexperience.systemui.dagger;

import static com.android.systemui.Dependency.TIME_TICK_HANDLER_NAME;
import static org.pixelexperience.systemui.Dependency.*;

import android.app.AlarmManager;
import android.app.IActivityManager;
import android.app.IWallpaperManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.StatsManager;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.om.OverlayManager;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IThermalService;
import android.os.Looper;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.UserManager;
import android.os.Vibrator;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.Nullable;

import com.android.internal.app.AssistUtils;
import com.android.internal.app.IBatteryStats;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.BootCompleteCache;
import com.android.systemui.InitController;
import com.android.systemui.R;
import com.android.systemui.accessibility.floatingmenu.AccessibilityFloatingMenuController;
import com.android.systemui.assist.AssistLogger;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.assist.PhoneStateMonitor;
import com.android.systemui.assist.ui.DefaultUiController;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.classifier.FalsingCollector;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dagger.qualifiers.UiBackground;
import com.android.systemui.demomode.DemoModeController;
import com.android.systemui.dock.DockManager;
import com.android.systemui.dump.DumpManager;
import com.android.systemui.keyguard.DismissCallbackRegistry;
import com.android.systemui.keyguard.KeyguardUnlockAnimationController;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.log.LogBuffer;
import com.android.systemui.log.LogBufferFactory;
import com.android.systemui.model.SysUiState;
import com.android.systemui.navigationbar.NavigationBarController;
import com.android.systemui.navigationbar.NavigationModeController;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.PluginDependencyProvider;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.ScreenPinningRequest;
import com.android.systemui.screenrecord.RecordingController;
import com.android.systemui.settings.UserContextProvider;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.settings.brightness.BrightnessSlider;
import com.android.systemui.shared.plugins.PluginManager;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.FeatureFlags;
import com.android.systemui.statusbar.LockscreenShadeTransitionController;
import com.android.systemui.statusbar.NotificationClickNotifier;
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
import com.android.systemui.statusbar.notification.NotificationEntryManager;
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
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.RemoteInputQuickSettingsDisabler;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.telephony.TelephonyListenerManager;
import com.android.systemui.theme.ThemeOverlayApplier;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.DeviceConfigProxy;
import com.android.systemui.util.concurrency.DelayableExecutor;
import com.android.systemui.util.sensors.ProximitySensor;
import com.android.systemui.util.settings.SecureSettings;
import com.android.systemui.util.wakelock.DelayedWakeLock;
import com.android.systemui.util.wakelock.WakeLock;
import com.android.systemui.volume.VolumeComponent;
import com.android.systemui.wmshell.BubblesManager;
import com.android.wm.shell.bubbles.Bubbles;
import com.android.wm.shell.legacysplitscreen.LegacySplitScreen;
import com.android.wm.shell.startingsurface.StartingSurface;
import com.android.wm.shell.tasksurfacehelper.TaskSurfaceHelper;
import com.google.android.systemui.LiveWallpaperScrimController;
import com.google.android.systemui.NotificationLockscreenUserManagerGoogle;
import com.google.android.systemui.assist.GoogleAssistLogger;
import com.google.android.systemui.assist.OpaEnabledDispatcher;
import com.google.android.systemui.assist.OpaEnabledReceiver;
import com.google.android.systemui.assist.OpaEnabledSettings;
import com.google.android.systemui.assist.uihints.AssistantPresenceHandler;
import com.google.android.systemui.assist.uihints.AssistantWarmer;
import com.google.android.systemui.assist.uihints.ColorChangeHandler;
import com.google.android.systemui.assist.uihints.ConfigurationHandler;
import com.google.android.systemui.assist.uihints.FlingVelocityWrapper;
import com.google.android.systemui.assist.uihints.GlowController;
import com.google.android.systemui.assist.uihints.GoBackHandler;
import com.google.android.systemui.assist.uihints.GoogleDefaultUiController;
import com.google.android.systemui.assist.uihints.IconController;
import com.google.android.systemui.assist.uihints.KeyboardMonitor;
import com.google.android.systemui.assist.uihints.LightnessProvider;
import com.google.android.systemui.assist.uihints.NavBarFader;
import com.google.android.systemui.assist.uihints.NgaMessageHandler;
import com.google.android.systemui.assist.uihints.NgaUiController;
import com.google.android.systemui.assist.uihints.OverlappedElementController;
import com.google.android.systemui.assist.uihints.OverlayUiHost;
import com.google.android.systemui.assist.uihints.ScrimController;
import com.google.android.systemui.assist.uihints.TakeScreenshotHandler;
import com.google.android.systemui.assist.uihints.TaskStackNotifier;
import com.google.android.systemui.assist.uihints.TimeoutManager;
import com.google.android.systemui.assist.uihints.TouchInsideHandler;
import com.google.android.systemui.assist.uihints.TouchOutsideHandler;
import com.google.android.systemui.assist.uihints.TranscriptionController;
import com.google.android.systemui.assist.uihints.edgelights.EdgeLightsController;
import com.google.android.systemui.assist.uihints.input.NgaInputHandler;
import com.google.android.systemui.assist.uihints.input.TouchActionRegion;
import com.google.android.systemui.assist.uihints.input.TouchInsideRegion;
import com.google.android.systemui.autorotate.AutorotateDataService;
import com.google.android.systemui.autorotate.DataLogger;
import com.google.android.systemui.columbus.ColumbusContentObserver;
import com.google.android.systemui.columbus.ColumbusService;
import com.google.android.systemui.columbus.ColumbusServiceWrapper;
import com.google.android.systemui.columbus.ColumbusSettings;
import com.google.android.systemui.columbus.ContentResolverWrapper;
import com.google.android.systemui.columbus.PowerManagerWrapper;
import com.google.android.systemui.columbus.actions.Action;
import com.google.android.systemui.columbus.actions.DismissTimer;
import com.google.android.systemui.columbus.actions.LaunchApp;
import com.google.android.systemui.columbus.actions.LaunchOpa;
import com.google.android.systemui.columbus.actions.LaunchOverview;
import com.google.android.systemui.columbus.actions.ManageMedia;
import com.google.android.systemui.columbus.actions.OpenNotificationShade;
import com.google.android.systemui.columbus.actions.SettingsAction;
import com.google.android.systemui.columbus.actions.SilenceCall;
import com.google.android.systemui.columbus.actions.SnoozeAlarm;
import com.google.android.systemui.columbus.actions.TakeScreenshot;
import com.google.android.systemui.columbus.actions.UnpinNotifications;
import com.google.android.systemui.columbus.actions.UserAction;
import com.google.android.systemui.columbus.actions.UserSelectedAction;
import com.google.android.systemui.columbus.feedback.FeedbackEffect;
import com.google.android.systemui.columbus.feedback.HapticClick;
import com.google.android.systemui.columbus.feedback.UserActivity;
import com.google.android.systemui.columbus.gates.CameraVisibility;
import com.google.android.systemui.columbus.gates.ChargingState;
import com.google.android.systemui.columbus.gates.FlagEnabled;
import com.google.android.systemui.columbus.gates.Gate;
import com.google.android.systemui.columbus.gates.KeyguardProximity;
import com.google.android.systemui.columbus.gates.KeyguardVisibility;
import com.google.android.systemui.columbus.gates.PowerSaveState;
import com.google.android.systemui.columbus.gates.PowerState;
import com.google.android.systemui.columbus.gates.Proximity;
import com.google.android.systemui.columbus.gates.ScreenTouch;
import com.google.android.systemui.columbus.gates.SetupWizard;
import com.google.android.systemui.columbus.gates.SilenceAlertsDisabled;
import com.google.android.systemui.columbus.gates.SystemKeyPress;
import com.google.android.systemui.columbus.gates.TelephonyActivity;
import com.google.android.systemui.columbus.gates.UsbState;
import com.google.android.systemui.columbus.gates.VrMode;
import com.google.android.systemui.columbus.sensors.CHREGestureSensor;
import com.google.android.systemui.columbus.sensors.GestureController;
import com.google.android.systemui.columbus.sensors.GestureSensor;
import com.google.android.systemui.columbus.sensors.GestureSensorImpl;
import com.google.android.systemui.columbus.sensors.config.Adjustment;
import com.google.android.systemui.columbus.sensors.config.GestureConfiguration;
import com.google.android.systemui.columbus.sensors.config.LowSensitivitySettingAdjustment;
import com.google.android.systemui.columbus.sensors.config.SensorConfiguration;
import com.google.android.systemui.dreamliner.DockObserver;
import com.google.android.systemui.dreamliner.DreamlinerUtils;
import com.google.android.systemui.elmyra.ServiceConfigurationGoogle;
import com.google.android.systemui.elmyra.actions.CameraAction;
import com.google.android.systemui.elmyra.actions.SetupWizardAction;
import com.google.android.systemui.elmyra.feedback.OpaHomeButton;
import com.google.android.systemui.elmyra.feedback.OpaLockscreen;
import com.google.android.systemui.elmyra.feedback.SquishyNavigationButtons;
import com.google.android.systemui.gamedashboard.EntryPointController;
import com.google.android.systemui.gamedashboard.FpsController;
import com.google.android.systemui.gamedashboard.GameDashboardUiEventLogger;
import com.google.android.systemui.gamedashboard.GameModeDndController;
import com.google.android.systemui.gamedashboard.ScreenRecordController;
import com.google.android.systemui.gamedashboard.ShortcutBarController;
import com.google.android.systemui.gamedashboard.ToastController;
import com.google.android.systemui.power.EnhancedEstimatesGoogleImpl;
import com.google.android.systemui.power.PowerNotificationWarningsGoogleImpl;
import com.google.android.systemui.qs.tiles.BatterySaverTileGoogle;
import com.google.android.systemui.qs.tiles.OverlayToggleTile;
import com.google.android.systemui.qs.tiles.ReverseChargingTile;
import com.google.android.systemui.reversecharging.ReverseChargingController;
import com.google.android.systemui.reversecharging.ReverseChargingViewController;
import com.google.android.systemui.reversecharging.ReverseWirelessCharger;
import com.google.android.systemui.smartspace.BcSmartspaceDataProvider;
import com.google.android.systemui.smartspace.KeyguardMediaViewController;
import com.google.android.systemui.smartspace.KeyguardSmartspaceController;
import com.google.android.systemui.smartspace.KeyguardZenAlarmViewController;
import com.google.android.systemui.smartspace.SmartSpaceController;
import com.google.android.systemui.statusbar.KeyguardIndicationControllerGoogle;
import com.google.android.systemui.statusbar.NotificationVoiceReplyManagerService;
import com.google.android.systemui.statusbar.notification.voicereplies.DebugNotificationVoiceReplyClient;
import com.google.android.systemui.statusbar.notification.voicereplies.NotificationVoiceReplyClient;
import com.google.android.systemui.statusbar.notification.voicereplies.NotificationVoiceReplyController;
import com.google.android.systemui.statusbar.notification.voicereplies.NotificationVoiceReplyLogger;
import com.google.android.systemui.statusbar.notification.voicereplies.NotificationVoiceReplyManager;
import com.google.android.systemui.statusbar.phone.WallpaperNotifier;
import com.google.android.systemui.theme.ThemeOverlayControllerGoogle;

import org.pixelexperience.systemui.GoogleServices;
import org.pixelexperience.systemui.assist.AssistManagerGoogle;
import org.pixelexperience.systemui.log.dagger.NotifVoiceReplyLog;
import org.pixelexperience.systemui.statusbar.phone.StatusBarGoogle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.inject.Named;
import javax.inject.Provider;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;

@Module
public class SystemUIGoogleDependencyProvider {
    @Provides
    @SysUISingleton
    static NotificationLockscreenUserManagerGoogle provideNotificationLockscreenUserManagerGoogle(Context context, BroadcastDispatcher broadcastDispatcher, DevicePolicyManager devicePolicyManager, UserManager userManager, NotificationClickNotifier notificationClickNotifier, KeyguardManager keyguardManager, StatusBarStateController statusBarStateController, @Main Handler handler, DeviceProvisionedController deviceProvisionedController, KeyguardStateController keyguardStateController, Lazy<KeyguardBypassController> lazy, SmartSpaceController smartSpaceController) {
        return new NotificationLockscreenUserManagerGoogle(context, broadcastDispatcher, devicePolicyManager, userManager, notificationClickNotifier, keyguardManager, statusBarStateController, handler, deviceProvisionedController, keyguardStateController, lazy, smartSpaceController);
    }

    @Provides
    @SysUISingleton
    static LiveWallpaperScrimController provideLiveWallpaperScrimController(LightBarController lightBarController, DozeParameters dozeParameters, AlarmManager alarmManager, KeyguardStateController keyguardStateController, DelayedWakeLock.Builder builder, Handler handler, @Nullable IWallpaperManager iWallpaperManager, LockscreenWallpaper lockscreenWallpaper, KeyguardUpdateMonitor keyguardUpdateMonitor, ConfigurationController configurationController, DockManager dockManager, @Main Executor executor, UnlockedScreenOffAnimationController unlockedScreenOffAnimationController) {
        return new LiveWallpaperScrimController(lightBarController, dozeParameters, alarmManager, keyguardStateController, builder, handler, iWallpaperManager, lockscreenWallpaper, keyguardUpdateMonitor, configurationController, dockManager, executor, unlockedScreenOffAnimationController);
    }

    @Provides
    @SysUISingleton
    static GoogleServices provideGoogleServices(Context context, Lazy<ServiceConfigurationGoogle> lazy, StatusBar statusBar, UiEventLogger uiEventLogger, Lazy<ColumbusServiceWrapper> lazyB, FeatureFlags featureFlags, KeyguardIndicationControllerGoogle keyguardIndicationControllerGoogle, AlarmManager alarmManager, AutorotateDataService autorotateDataService) {
        return new GoogleServices(context, lazy, statusBar, uiEventLogger, lazyB, featureFlags, keyguardIndicationControllerGoogle, alarmManager, autorotateDataService);
    }

    @Provides
    @SysUISingleton
    static AutorotateDataService provideAutorotateDataService(Context context, SensorManager sensorManager, DataLogger dataLogger, BroadcastDispatcher broadcastDispatcher, DeviceConfigProxy deviceConfigProxy, @Main DelayableExecutor delayableExecutor) {
        return new AutorotateDataService(context, sensorManager, dataLogger, broadcastDispatcher, deviceConfigProxy, delayableExecutor);
    }

    @Provides
    @SysUISingleton
    static DataLogger provideDataLogger(StatsManager statsManager) {
        return new DataLogger(statsManager);
    }

    @Provides
    @SysUISingleton
    static EnhancedEstimatesGoogleImpl provideEnhancedEstimatesGoogleImpl(Context context) {
        return new EnhancedEstimatesGoogleImpl(context);
    }

    @Provides
    @SysUISingleton
    static ReverseChargingTile provideReverseChargingTile(QSHost qSHost, @Background Looper looper, @Main Handler handler, FalsingManager falsingManager, MetricsLogger metricsLogger, StatusBarStateController statusBarStateController, ActivityStarter activityStarter, QSLogger qSLogger, BatteryController batteryController, IThermalService iThermalService) {
        return new ReverseChargingTile(qSHost, looper, handler, falsingManager, metricsLogger, statusBarStateController, activityStarter, qSLogger, batteryController, iThermalService);
    }

    @Provides
    @SysUISingleton
    static BatterySaverTileGoogle provideBatterySaverTileGoogle(QSHost qSHost, @Background Looper looper, @Main Handler handler, FalsingManager falsingManager, MetricsLogger metricsLogger, StatusBarStateController statusBarStateController, ActivityStarter activityStarter, QSLogger qSLogger, BatteryController batteryController, SecureSettings secureSettings) {
        return new BatterySaverTileGoogle(qSHost, looper, handler, falsingManager, metricsLogger, statusBarStateController, activityStarter, qSLogger, batteryController, secureSettings);
    }

    @Provides
    @SysUISingleton
    static OverlayToggleTile provideOverlayToggleTile(QSHost qSHost, @Background Looper looper, @Main Handler handler, FalsingManager falsingManager, MetricsLogger metricsLogger, StatusBarStateController statusBarStateController, ActivityStarter activityStarter, QSLogger qSLogger, OverlayManager overlayManager) {
        return new OverlayToggleTile(qSHost, looper, handler, falsingManager, metricsLogger, statusBarStateController, activityStarter, qSLogger, overlayManager);
    }

    @Provides
    @SysUISingleton
    static ServiceConfigurationGoogle provideServiceConfigurationGoogle(Context context, com.google.android.systemui.elmyra.feedback.AssistInvocationEffect assistInvocationEffect, com.google.android.systemui.elmyra.actions.LaunchOpa.Builder builder, com.google.android.systemui.elmyra.actions.SettingsAction.Builder builderB, CameraAction.Builder builderC, SetupWizardAction.Builder builderD, SquishyNavigationButtons squishyNavigationButtons, com.google.android.systemui.elmyra.actions.UnpinNotifications unpinNotifications, com.google.android.systemui.elmyra.actions.SilenceCall silenceCall, com.google.android.systemui.elmyra.gates.TelephonyActivity telephonyActivity) {
        return new ServiceConfigurationGoogle(context, assistInvocationEffect, builder, builderB, builderC, builderD, squishyNavigationButtons, unpinNotifications, silenceCall, telephonyActivity);
    }

    @Provides
    @SysUISingleton
    static com.google.android.systemui.elmyra.feedback.AssistInvocationEffect provideAssistInvocationEffectElmyra(AssistManagerGoogle assistManagerGoogle, OpaHomeButton opaHomeButton, OpaLockscreen opaLockscreen) {
        return new com.google.android.systemui.elmyra.feedback.AssistInvocationEffect(assistManagerGoogle, opaHomeButton, opaLockscreen);
    }

    @Provides
    @SysUISingleton
    static OpaHomeButton provideOpaHomeButton(KeyguardViewMediator keyguardViewMediator, StatusBar statusBar, NavigationModeController navigationModeController) {
        return new OpaHomeButton(keyguardViewMediator, statusBar, navigationModeController);
    }

    @Provides
    @SysUISingleton
    static OpaLockscreen provideOpaLockscreen(StatusBar statusBar, KeyguardStateController keyguardStateController) {
        return new OpaLockscreen(statusBar, keyguardStateController);
    }

    @Provides
    @SysUISingleton
    static SquishyNavigationButtons provideSquishyNavigationButtons(Context context, KeyguardViewMediator keyguardViewMediator, StatusBar statusBar, NavigationModeController navigationModeController) {
        return new SquishyNavigationButtons(context, keyguardViewMediator, statusBar, navigationModeController);
    }

    @Provides
    @SysUISingleton
    static com.google.android.systemui.elmyra.gates.TelephonyActivity provideTelephonyActivityElmyra(Context context, TelephonyListenerManager telephonyListenerManager) {
        return new com.google.android.systemui.elmyra.gates.TelephonyActivity(context, telephonyListenerManager);
    }

    @Provides
    @SysUISingleton
    static SetupWizardAction.Builder provideSetupWizardAction(Context context, StatusBar statusBar) {
        return new SetupWizardAction.Builder(context, statusBar);
    }

    @Provides
    @SysUISingleton
    static com.google.android.systemui.elmyra.actions.UnpinNotifications provideUnpinNotificationsElmyra(Context context, Optional<HeadsUpManager> optional) {
        return new com.google.android.systemui.elmyra.actions.UnpinNotifications(context, optional);
    }

    @Provides
    @SysUISingleton
    static com.google.android.systemui.elmyra.actions.LaunchOpa.Builder provideLaunchOpaElmyra(Context context, StatusBar statusBar) {
        return new com.google.android.systemui.elmyra.actions.LaunchOpa.Builder(context, statusBar);
    }

    @Provides
    @SysUISingleton
    static com.google.android.systemui.elmyra.actions.SilenceCall provideSilenceCallElmyra(Context context, TelephonyListenerManager telephonyListenerManager) {
        return new com.google.android.systemui.elmyra.actions.SilenceCall(context, telephonyListenerManager);
    }

    @Provides
    @SysUISingleton
    static com.google.android.systemui.elmyra.actions.SettingsAction.Builder provideSettingsActionElmyra(Context context, StatusBar statusBar) {
        return new com.google.android.systemui.elmyra.actions.SettingsAction.Builder(context, statusBar);
    }

    @Provides
    @SysUISingleton
    static CameraAction.Builder provideCameraAction(Context context, StatusBar statusBar) {
        return new CameraAction.Builder(context, statusBar);
    }

    @Provides
    @SysUISingleton
    static EntryPointController provideEntryPointController(Context context, AccessibilityManager accessibilityManager, BroadcastDispatcher broadcastDispatcher, CommandQueue commandQueue, GameModeDndController gameModeDndController, Handler handler, NavigationModeController navigationModeController, OverviewProxyService overviewProxyService, PackageManager packageManager, ShortcutBarController shortcutBarController, ToastController toastController, GameDashboardUiEventLogger gameDashboardUiEventLogger, Optional<TaskSurfaceHelper> optional) {
        return new EntryPointController(context, accessibilityManager, broadcastDispatcher, commandQueue, gameModeDndController, handler, navigationModeController, overviewProxyService, packageManager, shortcutBarController, toastController, gameDashboardUiEventLogger, optional);
    }

    @Provides
    @SysUISingleton
    static ShortcutBarController provideShortcutBarController(Context context, WindowManager windowManager, FpsController fpsController, ConfigurationController configurationController, Handler handler, ScreenRecordController screenRecordController, Optional<TaskSurfaceHelper> optional, GameDashboardUiEventLogger gameDashboardUiEventLogger, ToastController toastController) {
        return new ShortcutBarController(context, windowManager, fpsController, configurationController, handler, screenRecordController, optional, gameDashboardUiEventLogger, toastController);
    }

    @Provides
    @SysUISingleton
    static FpsController provideFpsController(@Main Executor executor) {
        return new FpsController(executor);
    }

    @Provides
    @SysUISingleton
    static GameDashboardUiEventLogger provideGameDashboardUiEventLogger(UiEventLogger uiEventLogger) {
        return new GameDashboardUiEventLogger(uiEventLogger);
    }

    @Provides
    @SysUISingleton
    static GameModeDndController provideGameModeDndController(Context context, NotificationManager notificationManager, BroadcastDispatcher broadcastDispatcher) {
        return new GameModeDndController(context, notificationManager, broadcastDispatcher);
    }

    @Provides
    @SysUISingleton
    static ScreenRecordController provideScreenRecordController(RecordingController recordingController, Handler handler, KeyguardDismissUtil keyguardDismissUtil, UserContextProvider userContextProvider, ToastController toastController) {
        return new ScreenRecordController(recordingController, handler, keyguardDismissUtil, userContextProvider, toastController);
    }

    @Provides
    @SysUISingleton
    static ToastController provideToastController(Context context, ConfigurationController configurationController, WindowManager windowManager, UiEventLogger uiEventLogger, NavigationModeController navigationModeController) {
        return new ToastController(context, configurationController, windowManager, uiEventLogger, navigationModeController);
    }

    @Provides
    @SysUISingleton
    static ThemeOverlayControllerGoogle provideThemeOverlayControllerGoogle(Context context, BroadcastDispatcher broadcastDispatcher, @Background Handler handler, @Main Executor executor, @Background Executor executorB, ThemeOverlayApplier themeOverlayApplier, SecureSettings secureSettings, WallpaperManager wallpaperManager, UserManager userManager, DumpManager dumpManager, DeviceProvisionedController deviceProvisionedController, UserTracker userTracker, FeatureFlags featureFlags, WakefulnessLifecycle wakefulnessLifecycle) {
        return new ThemeOverlayControllerGoogle(context, broadcastDispatcher, handler, executor, executorB, themeOverlayApplier, secureSettings, wallpaperManager, userManager, dumpManager, deviceProvisionedController, userTracker, featureFlags, wakefulnessLifecycle);
    }

    @Provides
    @SysUISingleton
    static KeyguardIndicationControllerGoogle provideKeyguardIndicationControllerGoogle(Context context, WakeLock.Builder builder, KeyguardStateController keyguardStateController, StatusBarStateController statusBarStateController, KeyguardUpdateMonitor keyguardUpdateMonitor, DockManager dockManager, BroadcastDispatcher broadcastDispatcher, DevicePolicyManager devicePolicyManager, IBatteryStats iBatteryStats, UserManager userManager, TunerService tunerService, DeviceConfigProxy deviceConfigProxy, @Main DelayableExecutor delayableExecutor, FalsingManager falsingManager, LockPatternUtils lockPatternUtils, IActivityManager iActivityManager, KeyguardBypassController keyguardBypassController) {
        return new KeyguardIndicationControllerGoogle(context, builder, keyguardStateController, statusBarStateController, keyguardUpdateMonitor, dockManager, broadcastDispatcher, devicePolicyManager, iBatteryStats, userManager, tunerService, deviceConfigProxy, delayableExecutor, falsingManager, lockPatternUtils, iActivityManager, keyguardBypassController);
    }

    @Provides
    @SysUISingleton
    static NotificationVoiceReplyManagerService provideNotificationVoiceReplyManagerService(NotificationVoiceReplyManager.Initializer initializer, NotificationVoiceReplyLogger notificationVoiceReplyLogger) {
        return new NotificationVoiceReplyManagerService(initializer, notificationVoiceReplyLogger);
    }

    @Provides
    @SysUISingleton
    static WallpaperNotifier provideWallpaperNotifier(Context context, NotificationEntryManager notificationEntryManager, BroadcastDispatcher broadcastDispatcher) {
        return new WallpaperNotifier(context, notificationEntryManager, broadcastDispatcher);
    }

    @Provides
    @SysUISingleton
    static NotificationVoiceReplyManager.Initializer provideNotificationVoiceReplyController(NotificationEntryManager notificationEntryManager, NotificationLockscreenUserManager notificationLockscreenUserManager, NotificationRemoteInputManager notificationRemoteInputManager, @Named(VOICE_REPLY_CTA_LAYOUT) int i, @Named(VOICE_REPLY_CTA_CONTAINER_ID) int iB, @Named(VOICE_REPLY_CTA_TEXT_ID) int iC, @Named(VOICE_REPLY_CTA_ICON_ID) int iD, LockscreenShadeTransitionController lockscreenShadeTransitionController, NotificationShadeWindowController notificationShadeWindowController, StatusBarKeyguardViewManager statusBarKeyguardViewManager, StatusBar statusBar, SysuiStatusBarStateController sysuiStatusBarStateController, HeadsUpManager headsUpManager, Context context, NotificationVoiceReplyLogger notificationVoiceReplyLogger) {
        return new NotificationVoiceReplyController(notificationEntryManager, notificationLockscreenUserManager, notificationRemoteInputManager, i, iB, iC, iD, lockscreenShadeTransitionController, notificationShadeWindowController, statusBarKeyguardViewManager, statusBar, sysuiStatusBarStateController, headsUpManager, context, notificationVoiceReplyLogger);
    }

    @Provides
    @SysUISingleton
    static Optional<NotificationVoiceReplyClient> provideNotificationVoiceReplyClient(BroadcastDispatcher broadcastDispatcher, NotificationLockscreenUserManager notificationLockscreenUserManager, NotificationVoiceReplyManager.Initializer initializer) {
        return Optional.of(new DebugNotificationVoiceReplyClient(broadcastDispatcher, notificationLockscreenUserManager, initializer));
    }

    @Provides
    @SysUISingleton
    static NotificationVoiceReplyLogger provideNotificationVoiceReplyLogger(@NotifVoiceReplyLog LogBuffer logBuffer, UiEventLogger uiEventLogger) {
        return new NotificationVoiceReplyLogger(logBuffer, uiEventLogger);
    }

    @Provides
    @SysUISingleton
    static ContentResolverWrapper provideContentResolverWrapper(Context context) {
        return new ContentResolverWrapper(context);
    }

    @Provides
    @SysUISingleton
    static ColumbusServiceWrapper provideColumbusServiceWrapper(ColumbusSettings columbusSettings, Lazy<ColumbusService> lazy, Lazy<SettingsAction> lazyB) {
        return new ColumbusServiceWrapper(columbusSettings, lazy, lazyB);
    }

    @Provides
    @SysUISingleton
    static ColumbusService provideColumbusService(List<Action> list, Set<FeedbackEffect> set, Set<Gate> setB, GestureController gestureController, PowerManagerWrapper powerManagerWrapper) {
        return new ColumbusService(list, set, setB, gestureController, powerManagerWrapper);
    }

    @Provides
    @SysUISingleton
    static PowerManagerWrapper providePowerManagerWrapper(Context context) {
        return new PowerManagerWrapper(context);
    }

    @Provides
    @SysUISingleton
    static ColumbusSettings provideColumbusSettings(Context context, ColumbusContentObserver.Factory factory) {
        return new ColumbusSettings(context, factory);
    }

    @Provides
    @SysUISingleton
    static ColumbusContentObserver.Factory provideColumbusContentObserver(ContentResolverWrapper contentResolverWrapper, IActivityManager iActivityManager, Handler handler) {
        return new ColumbusContentObserver.Factory(contentResolverWrapper, iActivityManager, handler);
    }

    @Provides
    @SysUISingleton
    static com.google.android.systemui.columbus.feedback.AssistInvocationEffect provideAssistInvocationEffectColumbus(AssistManager assistManager) {
        return new com.google.android.systemui.columbus.feedback.AssistInvocationEffect(assistManager);
    }

    @Provides
    @SysUISingleton
    static UserActivity provideUserActivity(Lazy<PowerManager> lazy) {
        return new UserActivity(lazy);
    }

    @Provides
    @SysUISingleton
    static HapticClick provideHapticClick(Lazy<Vibrator> lazy) {
        return new HapticClick(lazy);
    }

    @Provides
    @SysUISingleton
    static KeyguardProximity provideKeyguardProximity(Context context, KeyguardVisibility keyguardVisibility, Proximity proximity) {
        return new KeyguardProximity(context, keyguardVisibility, proximity);
    }

    @Provides
    @SysUISingleton
    static KeyguardVisibility provideKeyguardVisibility(Context context, Lazy<KeyguardStateController> lazy) {
        return new KeyguardVisibility(context, lazy);
    }

    @Provides
    @SysUISingleton
    static ChargingState provideChargingState(Context context, Handler handler, @Named(COLUMBUS_TRANSIENT_GATE_DURATION) long j) {
        return new ChargingState(context, handler, j);
    }

    @Provides
    @SysUISingleton
    static UsbState provideUsbState(Context context, Handler handler, @Named(COLUMBUS_TRANSIENT_GATE_DURATION) long j) {
        return new UsbState(context, handler, j);
    }

    @Provides
    @SysUISingleton
    static PowerSaveState providePowerSaveState(Context context) {
        return new PowerSaveState(context);
    }

    @Provides
    @SysUISingleton
    static SilenceAlertsDisabled provideSilenceAlertsDisabled(Context context, ColumbusSettings columbusSettings) {
        return new SilenceAlertsDisabled(context, columbusSettings);
    }

    @Provides
    @SysUISingleton
    static FlagEnabled provideFlagEnabled(Context context, ColumbusSettings columbusSettings, Handler handler) {
        return new FlagEnabled(context, columbusSettings, handler);
    }

    @Provides
    @SysUISingleton
    static CameraVisibility provideCameraVisibility(Context context, List<Action> list, KeyguardVisibility keyguardVisibility, PowerState powerState, IActivityManager iActivityManager, Handler handler) {
        return new CameraVisibility(context, list, keyguardVisibility, powerState, iActivityManager, handler);
    }

    @Provides
    @SysUISingleton
    static SetupWizard provideSetupWizard(Context context, @Named(COLUMBUS_SETUP_WIZARD_ACTIONS) Set<Action> set, Lazy<DeviceProvisionedController> lazy) {
        return new SetupWizard(context, set, lazy);
    }

    @Provides
    @SysUISingleton
    static PowerState providePowerState(Context context, Lazy<WakefulnessLifecycle> lazy) {
        return new PowerState(context, lazy);
    }

    @Provides
    @SysUISingleton
    static SystemKeyPress provideSystemKeyPress(Context context, Handler handler, CommandQueue commandQueue, @Named(COLUMBUS_TRANSIENT_GATE_DURATION) long j, @Named(COLUMBUS_BLOCKING_SYSTEM_KEYS) Set<Integer> set) {
        return new SystemKeyPress(context, handler, commandQueue, j, set);
    }

    @Provides
    @SysUISingleton
    static ScreenTouch provideScreenTouch(Context context, PowerState powerState, Handler handler) {
        return new ScreenTouch(context, powerState, handler);
    }

    @Provides
    @SysUISingleton
    static com.google.android.systemui.columbus.gates.TelephonyActivity provideTelephonyActivityColumbus(Context context, Lazy<TelephonyManager> lazy, Lazy<TelephonyListenerManager> lazyB) {
        return new com.google.android.systemui.columbus.gates.TelephonyActivity(context, lazy, lazyB);
    }

    @Provides
    @SysUISingleton
    static Proximity provideProximity(Context context, ProximitySensor proximitySensor) {
        return new Proximity(context, proximitySensor);
    }

    @Provides
    @SysUISingleton
    static VrMode provideVrMode(Context context) {
        return new VrMode(context);
    }

    @Provides
    @SysUISingleton
    static GestureSensorImpl provideGestureSensorImpl(Context context, UiEventLogger uiEventLogger, Handler handler) {
        return new GestureSensorImpl(context, uiEventLogger, handler);
    }

    @Provides
    @SysUISingleton
    static GestureController provideGestureController(GestureSensor gestureSensor, UiEventLogger uiEventLogger) {
        return new GestureController(gestureSensor, uiEventLogger);
    }

    @Provides
    @SysUISingleton
    static CHREGestureSensor provideCHREGestureSensor(Context context, UiEventLogger uiEventLogger, GestureConfiguration gestureConfiguration, StatusBarStateController statusBarStateController, WakefulnessLifecycle wakefulnessLifecycle, Handler handler) {
        return new CHREGestureSensor(context, uiEventLogger, gestureConfiguration, statusBarStateController, wakefulnessLifecycle, handler);
    }

    @Provides
    @SysUISingleton
    static GestureConfiguration provideGestureConfiguration(List<Adjustment> list, SensorConfiguration sensorConfiguration) {
        return new GestureConfiguration(list, sensorConfiguration);
    }

    @Provides
    @SysUISingleton
    static SensorConfiguration provideSensorConfiguration(Context context) {
        return new SensorConfiguration(context);
    }

    @Provides
    @SysUISingleton
    static LowSensitivitySettingAdjustment provideLowSensitivitySettingAdjustment(Context context, ColumbusSettings columbusSettings, SensorConfiguration sensorConfiguration) {
        return new LowSensitivitySettingAdjustment(context, columbusSettings, sensorConfiguration);
    }

    @Provides
    @SysUISingleton
    static SettingsAction provideSettingsActionColumbus(Context context, StatusBar statusBar, UiEventLogger uiEventLogger) {
        return new SettingsAction(context, statusBar, uiEventLogger);
    }

    @Provides
    @SysUISingleton
    static UserSelectedAction provideUserSelectedAction(Context context, ColumbusSettings columbusSettings, Map<String, UserAction> map, TakeScreenshot takeScreenshot, KeyguardStateController keyguardStateController, PowerManagerWrapper powerManagerWrapper, WakefulnessLifecycle wakefulnessLifecycle) {
        return new UserSelectedAction(context, columbusSettings, map, takeScreenshot, keyguardStateController, powerManagerWrapper, wakefulnessLifecycle);
    }

    @Provides
    @SysUISingleton
    static DismissTimer provideDismissTimer(Context context, SilenceAlertsDisabled silenceAlertsDisabled, IActivityManager iActivityManager) {
        return new DismissTimer(context, silenceAlertsDisabled, iActivityManager);
    }

    @Provides
    @SysUISingleton
    static com.google.android.systemui.columbus.actions.UnpinNotifications provideUnpinNotificationsColumbus(Context context, SilenceAlertsDisabled silenceAlertsDisabled, Optional<HeadsUpManager> optional) {
        return new com.google.android.systemui.columbus.actions.UnpinNotifications(context, silenceAlertsDisabled, optional);
    }

    @Provides
    @SysUISingleton
    static ManageMedia provideManageMedia(Context context, AudioManager audioManager, UiEventLogger uiEventLogger) {
        return new ManageMedia(context, audioManager, uiEventLogger);
    }

    @Provides
    @SysUISingleton
    static LaunchApp provideLaunchApp(Context context, LauncherApps launcherApps, IActivityManager iActivityManager, UserManager userManager, ColumbusSettings columbusSettings, KeyguardVisibility keyguardVisibility, @Main Handler handler, @Background Handler handlerB, UiEventLogger uiEventLogger) {
        return new LaunchApp(context, launcherApps, iActivityManager, userManager, columbusSettings, keyguardVisibility, handler, handlerB, uiEventLogger);
    }

    @Provides
    @SysUISingleton
    static com.google.android.systemui.columbus.actions.SilenceCall provideSilenceCallColumbus(Context context, SilenceAlertsDisabled silenceAlertsDisabled, Lazy<TelecomManager> lazy, Lazy<TelephonyManager> lazyB, Lazy<TelephonyListenerManager> lazyC) {
        return new com.google.android.systemui.columbus.actions.SilenceCall(context, silenceAlertsDisabled, lazy, lazyB, lazyC);
    }

    @Provides
    @SysUISingleton
    static LaunchOverview provideLaunchOverview(Context context, Recents recents, UiEventLogger uiEventLogger) {
        return new LaunchOverview(context, recents, uiEventLogger);
    }

    @Provides
    @SysUISingleton
    static com.google.android.systemui.columbus.actions.LaunchOpa provideLaunchOpaColumbus(Context context, StatusBar statusBar, Set<FeedbackEffect> set, AssistManager assistManager, Lazy<KeyguardManager> lazy, TunerService tunerService, ColumbusContentObserver.Factory factory, UiEventLogger uiEventLogger) {
        return new com.google.android.systemui.columbus.actions.LaunchOpa(context, statusBar, set, assistManager, lazy, tunerService, factory, uiEventLogger);
    }

    @Provides
    @SysUISingleton
    static com.google.android.systemui.columbus.actions.SnoozeAlarm provideSnoozeAlarmColumbus(Context context, SilenceAlertsDisabled silenceAlertsDisabled, IActivityManager iActivityManager) {
        return new com.google.android.systemui.columbus.actions.SnoozeAlarm(context, silenceAlertsDisabled, iActivityManager);
    }

    @Provides
    @SysUISingleton
    static OpenNotificationShade provideOpenNotificationShade(Context context, Lazy<NotificationShadeWindowController> lazy, Lazy<StatusBar> lazyB, UiEventLogger uiEventLogger) {
        return new OpenNotificationShade(context, lazy, lazyB, uiEventLogger);
    }

    @Provides
    @SysUISingleton
    static TakeScreenshot provideTakeScreenshot(Context context, Handler handler, UiEventLogger uiEventLogger) {
        return new TakeScreenshot(context, handler, uiEventLogger);
    }

    @Provides
    @SysUISingleton
    static ReverseChargingController provideReverseChargingController(Context context, BroadcastDispatcher broadcastDispatcher, Optional<ReverseWirelessCharger> optional, AlarmManager alarmManager, Optional<UsbManager> optionalB, @Main Executor executor, @Background Executor executorB, BootCompleteCache bootCompleteCache, IThermalService iThermalService) {
        return new ReverseChargingController(context, broadcastDispatcher, optional, alarmManager, optionalB, executor, executorB, bootCompleteCache, iThermalService);
    }

    @Provides
    @SysUISingleton
    static Optional<ReverseChargingViewController> provideReverseChargingViewController(Context context, BatteryController batteryController, Lazy<StatusBar> lazy, StatusBarIconController statusBarIconController, BroadcastDispatcher broadcastDispatcher, @Main Executor executor, KeyguardIndicationControllerGoogle keyguardIndicationControllerGoogle) {
        return Optional.of(new ReverseChargingViewController(context, batteryController, lazy, statusBarIconController, broadcastDispatcher, executor, keyguardIndicationControllerGoogle));
    }

    @Provides
    @SysUISingleton
    static KeyguardMediaViewController provideKeyguardMediaViewController(Context context, BcSmartspaceDataPlugin bcSmartspaceDataPlugin, @Main DelayableExecutor delayableExecutor, NotificationMediaManager notificationMediaManager, BroadcastDispatcher broadcastDispatcher) {
        return new KeyguardMediaViewController(context, bcSmartspaceDataPlugin, delayableExecutor, notificationMediaManager, broadcastDispatcher);
    }

    @Provides
    @SysUISingleton
    static KeyguardZenAlarmViewController provideKeyguardZenAlarmViewController(Context context, BcSmartspaceDataPlugin bcSmartspaceDataPlugin, ZenModeController zenModeController, AlarmManager alarmManager, NextAlarmController nextAlarmController, Handler handler) {
        return new KeyguardZenAlarmViewController(context, bcSmartspaceDataPlugin, zenModeController, alarmManager, nextAlarmController, handler);
    }

    @Provides
    @SysUISingleton
    static PowerNotificationWarningsGoogleImpl providePowerNotificationWarningsGoogleImpl(Context context, ActivityStarter activityStarter, UiEventLogger uiEventLogger) {
        return new PowerNotificationWarningsGoogleImpl(context, activityStarter, uiEventLogger);
    }

    @Provides
    @SysUISingleton
    static SmartSpaceController provideSmartSpaceController(Context context, KeyguardUpdateMonitor keyguardUpdateMonitor, Handler handler, AlarmManager alarmManager, DumpManager dumpManager) {
        return new SmartSpaceController(context, keyguardUpdateMonitor, handler, alarmManager, dumpManager);
    }

    @Provides
    @SysUISingleton
    static BcSmartspaceDataPlugin provideBcSmartspaceDataPlugin() {
        return new BcSmartspaceDataProvider();
    }

    @Provides
    @SysUISingleton
    static KeyguardSmartspaceController provideKeyguardSmartspaceController(Context context, FeatureFlags featureFlags, KeyguardZenAlarmViewController keyguardZenAlarmViewController, KeyguardMediaViewController keyguardMediaViewController) {
        return new KeyguardSmartspaceController(context, featureFlags, keyguardZenAlarmViewController, keyguardMediaViewController);
    }

    @Provides
    @SysUISingleton
    static OpaEnabledDispatcher provideOpaEnabledDispatcher(Lazy<StatusBar> lazy) {
        return new OpaEnabledDispatcher(lazy);
    }

    @Provides
    @SysUISingleton
    static GoogleAssistLogger provideGoogleAssistLogger(Context context, UiEventLogger uiEventLogger, AssistUtils assistUtils, PhoneStateMonitor phoneStateMonitor, AssistantPresenceHandler assistantPresenceHandler) {
        return new GoogleAssistLogger(context, uiEventLogger, assistUtils, phoneStateMonitor, assistantPresenceHandler);
    }

    @Provides
    @SysUISingleton
    static OpaEnabledReceiver provideOpaEnabledReceiver(Context context, BroadcastDispatcher broadcastDispatcher, @Main Executor executor, @Background Executor executorB, OpaEnabledSettings opaEnabledSettings) {
        return new OpaEnabledReceiver(context, broadcastDispatcher, executor, executorB, opaEnabledSettings);
    }

    @Provides
    @SysUISingleton
    static AssistManagerGoogle provideAssistManagerGoogle(DeviceProvisionedController deviceProvisionedController, Context context, AssistUtils assistUtils, NgaUiController ngaUiController, CommandQueue commandQueue, OpaEnabledReceiver opaEnabledReceiver, PhoneStateMonitor phoneStateMonitor, OverviewProxyService overviewProxyService, OpaEnabledDispatcher opaEnabledDispatcher, KeyguardUpdateMonitor keyguardUpdateMonitor, NavigationModeController navigationModeController, ConfigurationController configurationController, AssistantPresenceHandler assistantPresenceHandler, NgaMessageHandler ngaMessageHandler, Lazy<SysUiState> lazy, Handler handler, DefaultUiController defaultUiController, GoogleDefaultUiController googleDefaultUiController, IWindowManager iWindowManager, AssistLogger assistLogger) {
        return new AssistManagerGoogle(deviceProvisionedController, context, assistUtils, ngaUiController, commandQueue, opaEnabledReceiver, phoneStateMonitor, overviewProxyService, opaEnabledDispatcher, keyguardUpdateMonitor, navigationModeController, configurationController, assistantPresenceHandler, ngaMessageHandler, lazy, handler, defaultUiController, googleDefaultUiController, iWindowManager, assistLogger);
    }

    @Provides
    @SysUISingleton
    static OpaEnabledSettings provideOpaEnabledSettings(Context context) {
        return new OpaEnabledSettings(context);
    }

    @Provides
    @SysUISingleton
    static NavBarFader provideNavBarFader(Lazy<NavigationBarController> lazy, Handler handler) {
        return new NavBarFader(lazy, handler);
    }

    @Provides
    @SysUISingleton
    static FlingVelocityWrapper provideFlingVelocityWrapper() {
        return new FlingVelocityWrapper();
    }

    @Provides
    @SysUISingleton
    static TouchInsideHandler provideTouchInsideHandler(Lazy<AssistManager> lazy, NavigationModeController navigationModeController, AssistLogger assistLogger) {
        return new TouchInsideHandler(lazy, navigationModeController, assistLogger);
    }

    @Provides
    @SysUISingleton
    static OverlappedElementController provideOverlappedElementController(Lazy<StatusBar> lazy) {
        return new OverlappedElementController(lazy);
    }

    @Provides
    @SysUISingleton
    static AssistantPresenceHandler provideAssistantPresenceHandler(Context context, AssistUtils assistUtils) {
        return new AssistantPresenceHandler(context, assistUtils);
    }

    @Provides
    @SysUISingleton
    static ColorChangeHandler provideColorChangeHandler(Context context) {
        return new ColorChangeHandler(context);
    }

    @Provides
    @SysUISingleton
    static IconController provideIconController(LayoutInflater layoutInflater, @Named(OVERLAY_UI_HOST_PARENT_VIEW_GROUP) ViewGroup viewGroup, ConfigurationController configurationController) {
        return new IconController(layoutInflater, viewGroup, configurationController);
    }

    @Provides
    @SysUISingleton
    static AssistantWarmer provideAssistantWarmer(Context context) {
        return new AssistantWarmer(context);
    }

    @Provides
    @SysUISingleton
    static TranscriptionController provideTranscriptionController(@Named(OVERLAY_UI_HOST_PARENT_VIEW_GROUP) ViewGroup viewGroup, TouchInsideHandler touchInsideHandler, FlingVelocityWrapper flingVelocityWrapper, ConfigurationController configurationController) {
        return new TranscriptionController(viewGroup, touchInsideHandler, flingVelocityWrapper, configurationController);
    }

    @Provides
    @SysUISingleton
    static TouchOutsideHandler provideTouchOutsideHandler() {
        return new TouchOutsideHandler();
    }

    @Provides
    @SysUISingleton
    static ConfigurationHandler provideConfigurationHandler(Context context) {
        return new ConfigurationHandler(context);
    }

    @Provides
    @SysUISingleton
    static KeyboardMonitor provideKeyboardMonitor(Context context, Optional<CommandQueue> optional) {
        return new KeyboardMonitor(context, optional);
    }

    @Provides
    @SysUISingleton
    static TaskStackNotifier provideTaskStackNotifier() {
        return new TaskStackNotifier();
    }

    @Provides
    @SysUISingleton
    static TakeScreenshotHandler provideTakeScreenshotHandler(Context context) {
        return new TakeScreenshotHandler(context);
    }

    @Provides
    @SysUISingleton
    static GoBackHandler provideGoBackHandler() {
        return new GoBackHandler();
    }

    @Provides
    @SysUISingleton
    static NgaUiController provideNgaUiController(Context context, TimeoutManager timeoutManager, AssistantPresenceHandler assistantPresenceHandler, TouchInsideHandler touchInsideHandler, ColorChangeHandler colorChangeHandler, OverlayUiHost overlayUiHost, EdgeLightsController edgeLightsController, GlowController glowController, ScrimController scrimController, TranscriptionController transcriptionController, IconController iconController, LightnessProvider lightnessProvider, StatusBarStateController statusBarStateController, Lazy<AssistManager> lazy, FlingVelocityWrapper flingVelocityWrapper, AssistantWarmer assistantWarmer, NavBarFader navBarFader, AssistLogger assistLogger) {
        return new NgaUiController(context, timeoutManager, assistantPresenceHandler, touchInsideHandler, colorChangeHandler, overlayUiHost, edgeLightsController, glowController, scrimController, transcriptionController, iconController, lightnessProvider, statusBarStateController, lazy, flingVelocityWrapper, assistantWarmer, navBarFader, assistLogger);
    }

    @Provides
    @SysUISingleton
    static GlowController provideGlowController(Context context, @Named(OVERLAY_UI_HOST_PARENT_VIEW_GROUP) ViewGroup viewGroup, TouchInsideHandler touchInsideHandler) {
        return new GlowController(context, viewGroup, touchInsideHandler);
    }

    @Provides
    @SysUISingleton
    static GoogleDefaultUiController provideGoogleDefaultUiController(Context context, GoogleAssistLogger googleAssistLogger) {
        return new GoogleDefaultUiController(context, googleAssistLogger);
    }

    @Provides
    @SysUISingleton
    static NgaMessageHandler provideNgaMessageHandler(NgaUiController ngaUiController, AssistantPresenceHandler assistantPresenceHandler, NavigationModeController navigationModeController, Set<NgaMessageHandler.KeepAliveListener> set, Set<NgaMessageHandler.AudioInfoListener> setB, Set<NgaMessageHandler.CardInfoListener> setC, Set<NgaMessageHandler.ConfigInfoListener> setD, Set<NgaMessageHandler.EdgeLightsInfoListener> setE, Set<NgaMessageHandler.TranscriptionInfoListener> setF, Set<NgaMessageHandler.GreetingInfoListener> setG, Set<NgaMessageHandler.ChipsInfoListener> setH, Set<NgaMessageHandler.ClearListener> setI, Set<NgaMessageHandler.StartActivityInfoListener> setJ, Set<NgaMessageHandler.KeyboardInfoListener> setK, Set<NgaMessageHandler.ZerostateInfoListener> set1B, Set<NgaMessageHandler.GoBackListener> set1C, Set<NgaMessageHandler.TakeScreenshotListener> set1D, Set<NgaMessageHandler.WarmingListener> set1E, Set<NgaMessageHandler.NavBarVisibilityListener> set1F, Handler handler) {
        return new NgaMessageHandler(ngaUiController, assistantPresenceHandler, navigationModeController, set, setB, setC, setD, setE, setF, setG, setH, setI, setJ, setK, set1B, set1C, set1D, set1E, set1F, handler);
    }

    @Provides
    @SysUISingleton
    static ScrimController provideScrimController(@Named(OVERLAY_UI_HOST_PARENT_VIEW_GROUP) ViewGroup viewGroup, OverlappedElementController overlappedElementController, LightnessProvider lightnessProvider, TouchInsideHandler touchInsideHandler) {
        return new ScrimController(viewGroup, overlappedElementController, lightnessProvider, touchInsideHandler);
    }

    @Provides
    @SysUISingleton
    static TimeoutManager provideTimeoutManager(Lazy<AssistManager> lazy) {
        return new TimeoutManager(lazy);
    }

    @Provides
    @SysUISingleton
    static OverlayUiHost provideOverlayUiHost(Context context, TouchOutsideHandler touchOutsideHandler) {
        return new OverlayUiHost(context, touchOutsideHandler);
    }

    @Provides
    @SysUISingleton
    static LightnessProvider provideLightnessProvider() {
        return new LightnessProvider();
    }

    @Provides
    @SysUISingleton
    static EdgeLightsController provideEdgeLightsController(Context context, @Named(OVERLAY_UI_HOST_PARENT_VIEW_GROUP) ViewGroup viewGroup, AssistLogger assistLogger) {
        return new EdgeLightsController(context, viewGroup, assistLogger);
    }

    @Provides
    @SysUISingleton
    static NgaInputHandler provideNgaInputHandler(TouchInsideHandler touchInsideHandler, Set<TouchActionRegion> set, Set<TouchInsideRegion> setB) {
        return new NgaInputHandler(touchInsideHandler, set, setB);
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.AudioInfoListener> provideAudioInfoListeners(EdgeLightsController edgeLightsController, GlowController glowController) {
        return new HashSet(Arrays.asList(edgeLightsController, glowController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.CardInfoListener> provideCardInfoListeners(GlowController glowController, ScrimController scrimController, TranscriptionController transcriptionController, LightnessProvider lightnessProvider) {
        return new HashSet(Arrays.asList(glowController, scrimController, transcriptionController, lightnessProvider));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.TranscriptionInfoListener> provideTranscriptionInfoListener(TranscriptionController transcriptionController) {
        return new HashSet(Arrays.asList(transcriptionController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.GreetingInfoListener> provideGreetingInfoListener(TranscriptionController transcriptionController) {
        return new HashSet(Arrays.asList(transcriptionController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.ChipsInfoListener> provideChipsInfoListener(TranscriptionController transcriptionController) {
        return new HashSet(Arrays.asList(transcriptionController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.ClearListener> provideClearListener(TranscriptionController transcriptionController) {
        return new HashSet(Arrays.asList(transcriptionController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.KeyboardInfoListener> provideKeyboardInfoListener(IconController iconController) {
        return new HashSet(Arrays.asList(iconController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.ZerostateInfoListener> provideZerostateInfoListener(IconController iconController) {
        return new HashSet(Arrays.asList(iconController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.GoBackListener> provideGoBackListener(GoBackHandler goBackHandler) {
        return new HashSet(Arrays.asList(goBackHandler));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.TakeScreenshotListener> provideTakeScreenshotListener(TakeScreenshotHandler takeScreenshotHandler) {
        return new HashSet(Arrays.asList(takeScreenshotHandler));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.WarmingListener> provideWarmingListener(AssistantWarmer assistantWarmer) {
        return new HashSet(Arrays.asList(assistantWarmer));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.NavBarVisibilityListener> provideNavBarVisibilityListener(NavBarFader navBarFader) {
        return new HashSet(Arrays.asList(navBarFader));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.ConfigInfoListener> provideConfigInfoListeners(AssistantPresenceHandler assistantPresenceHandler, TouchInsideHandler touchInsideHandler, TouchOutsideHandler touchOutsideHandler, TaskStackNotifier taskStackNotifier, KeyboardMonitor keyboardMonitor, ColorChangeHandler colorChangeHandler, ConfigurationHandler configurationHandler) {
        return new HashSet(Arrays.asList(assistantPresenceHandler, touchInsideHandler, touchOutsideHandler, taskStackNotifier, keyboardMonitor, colorChangeHandler, configurationHandler));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.EdgeLightsInfoListener> provideEdgeLightsInfoListeners(EdgeLightsController edgeLightsController, NgaInputHandler ngaInputHandler) {
        return new HashSet(Arrays.asList(edgeLightsController, ngaInputHandler));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.KeepAliveListener> provideKeepAliveListener(TimeoutManager timeoutManager) {
        return new HashSet(Arrays.asList(timeoutManager));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.StartActivityInfoListener> provideActivityStarter(final Lazy<StatusBar> lazy) {
        return new HashSet(Collections.singletonList((NgaMessageHandler.StartActivityInfoListener) (intent, z) -> {
            if (intent == null) {
                Log.e("ActivityStarter", "Null intent; cannot start activity");
            } else {
                lazy.get().startActivity(intent, z);
            }
        }));
    }

    @Provides
    @ElementsIntoSet
    static Set<TouchActionRegion> provideTouchActionRegions(IconController iconController, TranscriptionController transcriptionController) {
        return new HashSet(Arrays.asList(iconController, transcriptionController));
    }

    @Provides
    @ElementsIntoSet
    static Set<TouchInsideRegion> provideTouchInsideRegions(GlowController glowController, ScrimController scrimController, TranscriptionController transcriptionController) {
        return new HashSet(Arrays.asList(glowController, scrimController, transcriptionController));
    }

    @Provides
    @SysUISingleton
    @Named(OVERLAY_UI_HOST_PARENT_VIEW_GROUP)
    static ViewGroup provideParentViewGroup(OverlayUiHost overlayUiHost) {
        return overlayUiHost.getParent();
    }

    @Provides
    @SysUISingleton
    static Optional<UsbManager> provideUsbManager(Context context) {
        return Optional.ofNullable(context.getSystemService(UsbManager.class));
    }

    @Provides
    @SysUISingleton
    static IThermalService provideIThermalService() {
        return IThermalService.Stub.asInterface(ServiceManager.getService("thermalservice"));
    }

    @Provides
    @SysUISingleton
    static Optional<ReverseWirelessCharger> provideReverseWirelessCharger(Context context) {
        return context.getResources().getBoolean(R.bool.config_wlc_support_enabled) ? Optional.of(new ReverseWirelessCharger(context)) : Optional.empty();
    }

    @Provides
    @SysUISingleton
    static DockObserver provideDockObserver(Context context, BroadcastDispatcher broadcastDispatcher, StatusBarStateController statusBarStateController, NotificationInterruptStateProvider notificationInterruptStateProvider, ConfigurationController configurationController, @Main DelayableExecutor delayableExecutor) {
        return new DockObserver(context, DreamlinerUtils.getInstance(context), broadcastDispatcher, statusBarStateController, notificationInterruptStateProvider, configurationController, delayableExecutor);
    }

    @Provides
    @SysUISingleton
    static StatusBarGoogle provideStatusBarGoogle(
            Context context,
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
            @UiBackground Executor uiBgExecutor,
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
            @Nullable KeyguardLiftController keyguardLiftController,
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
            @Named(TIME_TICK_HANDLER_NAME) Handler timeTickHandler,
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
            Optional<ReverseChargingViewController> reverseChargingController,
            Lazy<Optional<NotificationVoiceReplyClient>> notificationVoiceReplyClient) {
        return new StatusBarGoogle(
                context, notificationsController, lightBarController, autoHideController, keyguardUpdateMonitor,
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
                keyguardUnlockAnimationController, unlockedScreenOffAnimationController, startingSurfaceOptional,
                smartSpaceController, wallpaperNotifier, reverseChargingController, notificationVoiceReplyClient);
    }

    @SysUISingleton
    @Provides
    @Named(VOICE_REPLY_CTA_LAYOUT)
    static int provideVoiceReplyCtaLayout() {
        return R.layout.assist_voice_reply_cta;
    }

    @SysUISingleton
    @Provides
    @Named(VOICE_REPLY_CTA_CONTAINER_ID)
    static int provideVoiceReplyCtaContainerId() {
        return R.id.voice_reply_cta_container;
    }

    @SysUISingleton
    @Provides
    @Named(VOICE_REPLY_CTA_TEXT_ID)
    static int provideVoiceReplyCtaTextId() {
        return R.id.voice_reply_cta_text;
    }

    @SysUISingleton
    @Provides
    @Named(VOICE_REPLY_CTA_ICON_ID)
    static int provideVoiceReplyCtaIconId() {
        return R.id.voice_reply_cta_icon;
    }

    @Provides
    @SysUISingleton
    @NotifVoiceReplyLog
    static LogBuffer provideNotifVoiceReplyLogBuffer(LogBufferFactory factory) {
        return factory.create("NotifVoiceReplyLog", 500);
    }

    @Provides
    @SysUISingleton
    @Named(COLUMBUS_TRANSIENT_GATE_DURATION)
    static long provideTransientGateDuration() {
        return 500;
    }

    @Provides
    @SysUISingleton
    @ElementsIntoSet
    static Set<Gate> provideColumbusGates(FlagEnabled flagEnabled, ChargingState chargingState, UsbState usbState, KeyguardProximity keyguardProximity, SetupWizard setupWizard, SystemKeyPress systemKeyPress, TelephonyActivity telephonyActivity, VrMode vrMode, CameraVisibility cameraVisibility, PowerSaveState powerSaveState, PowerState powerState, ScreenTouch screenTouch) {
        return new HashSet(Arrays.asList(flagEnabled, chargingState, usbState, keyguardProximity, setupWizard, systemKeyPress, telephonyActivity, vrMode, cameraVisibility, powerSaveState, powerState, screenTouch));
    }

    @Provides
    @SysUISingleton
    static Map<String, UserAction> provideUserSelectedActions(LaunchOpa launchOpa, ManageMedia manageMedia, TakeScreenshot takeScreenshot, LaunchOverview launchOverview, OpenNotificationShade openNotificationShade, LaunchApp launchApp) {
        Map<String, UserAction> result = new HashMap<>();
        result.put("assistant", launchOpa);
        result.put("media", manageMedia);
        result.put("screenshot", takeScreenshot);
        result.put("overview", launchOverview);
        result.put("notifications", openNotificationShade);
        result.put("launch", launchApp);
        return result;
    }

    @Provides
    @SysUISingleton
    static GestureSensor provideGestureSensor(Context context, ColumbusSettings columbusSettings, Lazy<CHREGestureSensor> gestureSensor, Lazy<GestureSensorImpl> apSensor) {
        if (columbusSettings.useApSensor() || !context.getPackageManager().hasSystemFeature("android.hardware.context_hub")) {
            Log.i("Columbus/Module", "Creating AP sensor");
            return apSensor.get();
        }
        Log.i("Columbus/Module", "Creating CHRE sensor");
        return gestureSensor.get();
    }

    @Provides
    @SysUISingleton
    static List<Action> provideColumbusActions(@Named(COLUMBUS_FULL_SCREEN_ACTIONS) List<Action> fullScreenActions, UnpinNotifications unpinNotifications, UserSelectedAction userSelectedAction) {
        List<Action> result = new ArrayList<>(fullScreenActions);
        result.add(unpinNotifications);
        result.add(userSelectedAction);
        return result;
    }

    @Provides
    @SysUISingleton
    @Named(COLUMBUS_FULL_SCREEN_ACTIONS)
    static List<Action> provideFullscreenActions(DismissTimer dismissTimer, SnoozeAlarm snoozeAlarm, SilenceCall silenceCall, SettingsAction settingsAction) {
        return Arrays.asList(dismissTimer, snoozeAlarm, silenceCall, settingsAction);
    }

    @Provides
    @SysUISingleton
    @ElementsIntoSet
    static Set<FeedbackEffect> provideColumbusEffects(HapticClick hapticClick, UserActivity userActivity) {
        return new HashSet(Arrays.asList(hapticClick, userActivity));
    }

    @Provides
    @SysUISingleton
    static List<Adjustment> provideGestureAdjustments(LowSensitivitySettingAdjustment lowSensitivitySettingAdjustment) {
        return Collections.singletonList(lowSensitivitySettingAdjustment);
    }

    @Provides
    @SysUISingleton
    @Named(COLUMBUS_BLOCKING_SYSTEM_KEYS)
    @ElementsIntoSet
    static Set<Integer> provideBlockingSystemKeys() {
        return new HashSet(Arrays.asList(24, 25, 26));
    }

    @Provides
    @SysUISingleton
    @Named(COLUMBUS_SETUP_WIZARD_ACTIONS)
    @ElementsIntoSet
    static Set<Action> provideSetupWizardActions(SettingsAction settingsAction) {
        return new HashSet(Arrays.asList(settingsAction));
    }
}
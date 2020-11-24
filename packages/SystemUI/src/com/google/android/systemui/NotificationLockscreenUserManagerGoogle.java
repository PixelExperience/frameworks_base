package com.google.android.systemui;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Handler;
import android.os.UserManager;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationClickNotifier;
import com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl;
import com.android.systemui.statusbar.phone.KeyguardBypassController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.KeyguardStateController;

import com.google.android.systemui.smartspace.SmartSpaceController;

import dagger.Lazy;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NotificationLockscreenUserManagerGoogle extends NotificationLockscreenUserManagerImpl {
    @Inject
    public Lazy<KeyguardBypassController> mKeyguardBypassControllerLazy;
    private final KeyguardStateController mKeyguardStateController;
    @Inject
    public SmartSpaceController mSmartSpaceController;

    @Inject
    public NotificationLockscreenUserManagerGoogle(Context context,
            BroadcastDispatcher broadcastDispatcher,
            DevicePolicyManager devicePolicyManager,
            UserManager userManager,
            NotificationClickNotifier clickNotifier,
            KeyguardManager keyguardManager,
            StatusBarStateController statusBarStateController,
            @Main Handler mainHandler,
            DeviceProvisionedController deviceProvisionedController,
            KeyguardStateController keyguardStateController) {
        super(context, broadcastDispatcher, devicePolicyManager, userManager,
            clickNotifier, keyguardManager, statusBarStateController, mainHandler,
            deviceProvisionedController, keyguardStateController);
        mKeyguardStateController = keyguardStateController;
        mKeyguardStateController.addCallback(new KeyguardStateController.Callback() {
            @Override 
            public void onKeyguardShowingChanged() {
                updateSmartSpaceVisibilitySettings();
            }
        });
    }

    @Override
    public void updateLockscreenNotificationSetting() {
        super.updateLockscreenNotificationSetting();
        updateSmartSpaceVisibilitySettings();
    }

    public void updateSmartSpaceVisibilitySettings() {
        boolean hideSensitiveData = !userAllowsPrivateNotificationsInPublic(mCurrentUserId) && (isAnyProfilePublicMode() || !mKeyguardStateController.isShowing());
        boolean hideWorkData = false;
        if (!mKeyguardBypassControllerLazy.get().getBypassEnabled()) {
            if (!allowsManagedPrivateNotificationsInPublic() && (isAnyManagedProfilePublicMode() || !mKeyguardStateController.isShowing())) {
                hideWorkData = true;
            }
        }
        mSmartSpaceController.setHideSensitiveData(hideSensitiveData, hideWorkData);
    }

    @Override
    public void updatePublicMode() {
        super.updatePublicMode();
        updateLockscreenNotificationSetting();
    }
}

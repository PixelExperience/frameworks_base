package com.google.android.systemui;

import android.content.Context;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.google.android.systemui.smartspace.SmartSpaceController;

public class NotificationLockscreenUserManagerGoogle extends NotificationLockscreenUserManager {
    public NotificationLockscreenUserManagerGoogle(Context context) {
        super(context);
    }

    @Override
    public void updateLockscreenNotificationSetting() {
        super.updateLockscreenNotificationSetting();
        updateAodVisibilitySettings();
    }

    public void updateAodVisibilitySettings() {
        SmartSpaceController.get(this.mContext).setHideSensitiveData(!userAllowsPrivateNotificationsInPublic(this.mCurrentUserId));
    }
}

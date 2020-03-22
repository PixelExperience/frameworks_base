package com.google.android.systemui;

import android.content.ContentResolver;
import android.provider.Settings;
import com.android.keyguard.KeyguardUpdateMonitor;

public class UserSettingsUtils {
    public static void save(ContentResolver contentResolver, boolean z) {
        Settings.Secure.putIntForUser(contentResolver, "systemui.google.opa_enabled", z ? 1 : 0, KeyguardUpdateMonitor.getCurrentUser());
    }

    public static boolean load(ContentResolver contentResolver) {
        if (Settings.Secure.getIntForUser(contentResolver, "systemui.google.opa_enabled", 0, KeyguardUpdateMonitor.getCurrentUser()) != 0) {
            return true;
        }
        return false;
    }
}

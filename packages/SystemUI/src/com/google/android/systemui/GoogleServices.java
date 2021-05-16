package com.google.android.systemui;

import android.content.Context;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.phone.StatusBar;
import com.google.android.systemui.ambientmusic.AmbientIndicationContainer;
import com.google.android.systemui.ambientmusic.AmbientIndicationService;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GoogleServices extends SystemUI {
    private final StatusBar mStatusBar;

    @Inject
    public GoogleServices(Context context, StatusBar statusBar) {
        super(context);
        mStatusBar = statusBar;
    }

    @Override
    public void start() {
        AmbientIndicationContainer ambientIndicationContainer = (AmbientIndicationContainer) mStatusBar.getNotificationShadeWindowView().findViewById(
            R.id.ambient_indication_container);
        ambientIndicationContainer.initializeView(mStatusBar);
        new AmbientIndicationService(mContext, ambientIndicationContainer);
    }
}

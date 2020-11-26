package com.google.android.systemui.assist.uihints;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.assist.AssistLogger;
import com.android.systemui.assist.ui.DefaultUiController;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GoogleDefaultUiController extends DefaultUiController {
    @Inject
    public GoogleDefaultUiController(Context context, AssistLogger assistLogger) {
        super(context, assistLogger);
        setGoogleAssistant(false);
        AssistantInvocationLightsView assistantInvocationLightsView = (AssistantInvocationLightsView) LayoutInflater.from(context).inflate(R.layout.invocation_lights, (ViewGroup) mRoot, false);
        mInvocationLightsView = assistantInvocationLightsView;
        mRoot.addView(assistantInvocationLightsView);
    }

    public void setGoogleAssistant(boolean z) {
        ((AssistantInvocationLightsView) mInvocationLightsView).setGoogleAssistant(z);
    }
}

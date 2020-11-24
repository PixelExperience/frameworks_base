package com.google.android.systemui.smartspace;

public interface SmartSpaceUpdateListener {
    default void onGsaChanged(){
    }

    default void onSensitiveModeChanged(boolean hideSensitiveData, boolean hideWorkData){
    }

    void onSmartSpaceUpdated(SmartSpaceData smartSpaceData);
}

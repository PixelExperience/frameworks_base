package com.google.android.systemui.power;

import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.fuelgauge.EstimateKt;
import com.android.systemui.power.EnhancedEstimates;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EnhancedEstimatesGoogleImpl implements EnhancedEstimates {

    @Inject
    public EnhancedEstimatesGoogleImpl() {
        android.util.Log.d("HENRIQUE", "EnhancedEstimatesGoogleImpl started");
    }

    @Override
    public boolean isHybridNotificationEnabled() {
        android.util.Log.d("HENRIQUE", "EnhancedEstimatesGoogleImpl, isHybridNotificationEnabled started");
        return false;
    }

    @Override
    public Estimate getEstimate() {
        android.util.Log.d("HENRIQUE", "EnhancedEstimatesGoogleImpl, getEstimate");
        // Returns an unknown estimate.
        return new Estimate(EstimateKt.ESTIMATE_MILLIS_UNKNOWN,
                false /* isBasedOnUsage */,
                EstimateKt.AVERAGE_TIME_TO_DISCHARGE_UNKNOWN);
    }

    @Override
    public long getLowWarningThreshold() {
        android.util.Log.d("HENRIQUE", "EnhancedEstimatesGoogleImpl, getLowWarningThreshold");
        return 0;
    }

    @Override
    public long getSevereWarningThreshold() {
        android.util.Log.d("HENRIQUE", "EnhancedEstimatesGoogleImpl, getSevereWarningThreshold");
        return 0;
    }

    @Override
    public boolean getLowWarningEnabled() {
        android.util.Log.d("HENRIQUE", "EnhancedEstimatesGoogleImpl, getLowWarningEnabled");
        return true;
    }
}

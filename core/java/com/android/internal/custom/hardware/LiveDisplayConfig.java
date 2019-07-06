/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.internal.custom.hardware;

import static com.android.internal.custom.hardware.LiveDisplayManager.FEATURE_COLOR_BALANCE;
import static com.android.internal.custom.hardware.LiveDisplayManager.FEATURE_FIRST;
import static com.android.internal.custom.hardware.LiveDisplayManager.FEATURE_LAST;
import static com.android.internal.custom.hardware.LiveDisplayManager.MODE_FIRST;
import static com.android.internal.custom.hardware.LiveDisplayManager.MODE_LAST;
import static com.android.internal.custom.hardware.LiveDisplayManager.MODE_OFF;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Range;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import com.android.internal.util.custom.Concierge;
import com.android.internal.util.custom.Concierge.ParcelInfo;

/**
 * Holder class for LiveDisplay static configuration.
 *
 * This class holds various defaults and hardware capabilities
 * which are involved with LiveDisplay.
 */
public class LiveDisplayConfig implements Parcelable {

    private final BitSet mCapabilities;
    private final BitSet mAllModes = new BitSet();

    private final boolean mDefaultAutoContrast;
    private final boolean mDefaultAutoOutdoorMode;
    private final boolean mDefaultCABC;
    private final boolean mDefaultColorEnhancement;

    private final Range<Float> mHueRange;
    private final Range<Float> mSaturationRange;
    private final Range<Float> mIntensityRange;
    private final Range<Float> mContrastRange;
    private final Range<Float> mSaturationThresholdRange;

    public LiveDisplayConfig(BitSet capabilities, boolean defaultAutoOutdoorMode,
            boolean defaultAutoContrast,
            boolean defaultCABC, boolean defaultColorEnhancement,
            Range<Float> hueRange,
            Range<Float> saturationRange,
            Range<Float> intensityRange,
            Range<Float> contrastRange,
            Range<Float> saturationThresholdRange) {
        super();
        mCapabilities = (BitSet) capabilities.clone();
        mAllModes.set(MODE_FIRST, MODE_LAST);
        mDefaultAutoContrast = defaultAutoContrast;
        mDefaultAutoOutdoorMode = defaultAutoOutdoorMode;
        mDefaultCABC = defaultCABC;
        mDefaultColorEnhancement = defaultColorEnhancement;
        mHueRange = hueRange;
        mSaturationRange = saturationRange;
        mIntensityRange = intensityRange;
        mContrastRange = contrastRange;
        mSaturationThresholdRange = saturationThresholdRange;
    }

    private LiveDisplayConfig(Parcel parcel) {
        // Read parcelable version via the Concierge
        ParcelInfo parcelInfo = Concierge.receiveParcel(parcel);
        int parcelableVersion = parcelInfo.getParcelVersion();

        // temp vars
        long capabilities = 0;
        boolean defaultAutoContrast = false;
        boolean defaultAutoOutdoorMode = false;
        boolean defaultCABC = false;
        boolean defaultColorEnhancement = false;
        float[] paRanges = new float[10];

        capabilities = parcel.readLong();
        defaultAutoContrast = parcel.readInt() == 1;
        defaultAutoOutdoorMode = parcel.readInt() == 1;
        defaultCABC = parcel.readInt() == 1;
        defaultColorEnhancement = parcel.readInt() == 1;
        parcel.readFloatArray(paRanges);

        // set temps
        mCapabilities = BitSet.valueOf(new long[] { capabilities });
        mAllModes.set(MODE_FIRST, MODE_LAST);
        mDefaultAutoContrast = defaultAutoContrast;
        mDefaultAutoOutdoorMode = defaultAutoOutdoorMode;
        mDefaultCABC = defaultCABC;
        mDefaultColorEnhancement = defaultColorEnhancement;
        mHueRange = Range.create(paRanges[0], paRanges[1]);
        mSaturationRange = Range.create(paRanges[2], paRanges[3]);
        mIntensityRange = Range.create(paRanges[4], paRanges[5]);
        mContrastRange = Range.create(paRanges[6], paRanges[7]);
        mSaturationThresholdRange = Range.create(paRanges[8], paRanges[9]);

        // Complete parcel info for the concierge
        parcelInfo.complete();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("capabilities=").append(mCapabilities.toString());
        sb.append(" defaultAutoOutdoorMode=").append(mDefaultAutoOutdoorMode);
        sb.append(" defaultAutoContrast=").append(mDefaultAutoContrast);
        sb.append(" defaultCABC=").append(mDefaultCABC);
        sb.append(" defaultColorEnhancement=").append(mDefaultColorEnhancement);
        if (mCapabilities.get(LiveDisplayManager.FEATURE_PICTURE_ADJUSTMENT)) {
            sb.append(" hueRange=").append(mHueRange);
            sb.append(" saturationRange=").append(mSaturationRange);
            sb.append(" intensityRange=").append(mIntensityRange);
            sb.append(" contrastRange=").append(mContrastRange);
            sb.append(" saturationThresholdRange=").append(mSaturationThresholdRange);
        }
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        // Tell the concierge to prepare the parcel
        ParcelInfo parcelInfo = Concierge.prepareParcel(out);

        // ==== FIG =====
        long[] caps = mCapabilities.toLongArray();
        out.writeLong(caps != null && caps.length > 0 ? caps[0] : 0L);
        out.writeInt(mDefaultAutoContrast ? 1 : 0);
        out.writeInt(mDefaultAutoOutdoorMode ? 1 : 0);
        out.writeInt(mDefaultCABC ? 1 : 0);
        out.writeInt(mDefaultColorEnhancement ? 1 : 0);
        out.writeFloatArray(new float[] {
                mHueRange.getLower(), mHueRange.getUpper(),
                mSaturationRange.getLower(), mSaturationRange.getUpper(),
                mIntensityRange.getLower(), mIntensityRange.getUpper(),
                mContrastRange.getLower(), mContrastRange.getUpper(),
                mSaturationThresholdRange.getLower(), mSaturationThresholdRange.getUpper() } );

        // Complete the parcel info for the concierge
        parcelInfo.complete();
    }

    /**
     * Checks if a particular feature or mode is supported by the system.
     *
     * @param feature
     * @return true if capable
     */
    public boolean hasFeature(int feature) {
        return ((feature >= MODE_FIRST && feature <= MODE_LAST) ||
                (feature >= FEATURE_FIRST && feature <= FEATURE_LAST)) &&
                (feature == MODE_OFF || mCapabilities.get(feature));
    }

    /**
     * Checks if LiveDisplay is available for use on this device.
     *
     * @return true if any feature is enabled
     */
    public boolean isAvailable() {
        return !mCapabilities.isEmpty();
    }

    /**
     * Checks if LiveDisplay has support for adaptive modes.
     *
     * @return true if adaptive modes are available
     */
    public boolean hasModeSupport() {
        return isAvailable() && mCapabilities.intersects(mAllModes);
    }

    /**
     * Get the default value for auto contrast
     *
     * @return true if enabled
     */
    public boolean getDefaultAutoContrast() {
        return mDefaultAutoContrast;
    }

    /**
     * Get the default value for automatic outdoor mode
     *
     * @return true if enabled
     */
    public boolean getDefaultAutoOutdoorMode() {
        return mDefaultAutoOutdoorMode;
    }

    /**
     * Get the default value for CABC
     *
     * @return true if enabled
     */
    public boolean getDefaultCABC() {
        return mDefaultCABC;
    }

    /**
     * Get the default value for color enhancement
     *
     * @return true if enabled
     */
    public boolean getDefaultColorEnhancement() {
        return mDefaultColorEnhancement;
    }

    /**
     * Get the supported range for hue adjustment
     *
     * @return float range
     */
    public Range<Float> getHueRange() { return mHueRange; }

    /**
     * Get the supported range for saturation adjustment
     *
     * @return float range
     */
    public Range<Float> getSaturationRange() { return mSaturationRange; }

    /**
     * Get the supported range for intensity adjustment
     *
     * @return float range
     */
    public Range<Float> getIntensityRange() { return mIntensityRange; }

    /**
     * Get the supported range for contrast adjustment
     *
     * @return float range
     */
    public Range<Float> getContrastRange() { return mContrastRange; }

    /**
     * Get the supported range for saturation threshold adjustment
     *
     * @return float range
     */
    public Range<Float> getSaturationThresholdRange() {
        return mSaturationThresholdRange;
    }


    /**
     * Convenience method to get a list of all picture adjustment ranges
     * with a single call.
     *
     * @return List of float ranges
     */
    public List<Range<Float>> getPictureAdjustmentRanges() {
        return Arrays.asList(mHueRange, mSaturationRange, mIntensityRange,
                             mContrastRange, mSaturationThresholdRange);
    }

    /** @hide */
    public static final Parcelable.Creator<LiveDisplayConfig> CREATOR =
            new Parcelable.Creator<LiveDisplayConfig>() {
        public LiveDisplayConfig createFromParcel(Parcel in) {
            return new LiveDisplayConfig(in);
        }

        @Override
        public LiveDisplayConfig[] newArray(int size) {
            return new LiveDisplayConfig[size];
        }
    };
}

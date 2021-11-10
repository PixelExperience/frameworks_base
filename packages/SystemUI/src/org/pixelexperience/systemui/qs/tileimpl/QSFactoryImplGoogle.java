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

package org.pixelexperience.systemui.qs.tileimpl;

import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tileimpl.QSFactoryImpl;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.qs.tiles.AirplaneModeTile;
import com.android.systemui.qs.tiles.AlarmTile;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.qs.tiles.CameraToggleTile;
import com.android.systemui.qs.tiles.CastTile;
import com.android.systemui.qs.tiles.CellularTile;
import com.android.systemui.qs.tiles.ColorInversionTile;
import com.android.systemui.qs.tiles.DataSaverTile;
import com.android.systemui.qs.tiles.DeviceControlsTile;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.FlashlightTile;
import com.android.systemui.qs.tiles.HotspotTile;
import com.android.systemui.qs.tiles.InternetTile;
import com.android.systemui.qs.tiles.LocationTile;
import com.android.systemui.qs.tiles.MicrophoneToggleTile;
import com.android.systemui.qs.tiles.NfcTile;
import com.android.systemui.qs.tiles.NightDisplayTile;
import com.android.systemui.qs.tiles.QuickAccessWalletTile;
import com.android.systemui.qs.tiles.ReduceBrightColorsTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.qs.tiles.ScreenRecordTile;
import com.android.systemui.qs.tiles.UiModeNightTile;
import com.android.systemui.qs.tiles.UserTile;
import com.android.systemui.qs.tiles.WifiTile;
import com.android.systemui.qs.tiles.WorkModeTile;
import com.android.systemui.util.leak.GarbageMonitor;
import com.google.android.systemui.qs.tiles.BatterySaverTileGoogle;
import com.google.android.systemui.qs.tiles.OverlayToggleTile;
import com.google.android.systemui.qs.tiles.ReverseChargingTile;

import javax.inject.Inject;
import javax.inject.Provider;

import dagger.Lazy;

@SysUISingleton
public class QSFactoryImplGoogle extends QSFactoryImpl {
    private final Provider<BatterySaverTileGoogle> mBatterySaverTileGoogleProvider;
    private final Provider<OverlayToggleTile> mOverlayToggleTileProvider;
    private final Provider<ReverseChargingTile> mReverseChargingTileProvider;

    @Inject
    public QSFactoryImplGoogle(
            Lazy<QSHost> qsHostLazy,
            Provider<CustomTile.Builder> customTileBuilderProvider,
            Provider<WifiTile> wifiTileProvider,
            Provider<InternetTile> internetTileProvider,
            Provider<BluetoothTile> bluetoothTileProvider,
            Provider<CellularTile> cellularTileProvider,
            Provider<DndTile> dndTileProvider,
            Provider<ColorInversionTile> colorInversionTileProvider,
            Provider<AirplaneModeTile> airplaneModeTileProvider,
            Provider<WorkModeTile> workModeTileProvider,
            Provider<RotationLockTile> rotationLockTileProvider,
            Provider<FlashlightTile> flashlightTileProvider,
            Provider<LocationTile> locationTileProvider,
            Provider<CastTile> castTileProvider,
            Provider<HotspotTile> hotspotTileProvider,
            Provider<UserTile> userTileProvider,
            Provider<BatterySaverTileGoogle> batterySaverTileGoogleProvider,
            Provider<DataSaverTile> dataSaverTileProvider,
            Provider<NightDisplayTile> nightDisplayTileProvider,
            Provider<NfcTile> nfcTileProvider,
            Provider<GarbageMonitor.MemoryTile> memoryTileProvider,
            Provider<UiModeNightTile> uiModeNightTileProvider,
            Provider<ScreenRecordTile> screenRecordTileProvider,
            Provider<ReduceBrightColorsTile> reduceBrightColorsTileProvider,
            Provider<CameraToggleTile> cameraToggleTileProvider,
            Provider<MicrophoneToggleTile> microphoneToggleTileProvider,
            Provider<DeviceControlsTile> deviceControlsTileProvider,
            Provider<AlarmTile> alarmTileProvider,
            Provider<QuickAccessWalletTile> quickAccessWalletTileProvider,
            Provider<OverlayToggleTile> overlayToggleTileProvider,
            Provider<ReverseChargingTile> reverseChargingTileProvider) {
        super(qsHostLazy,
                customTileBuilderProvider,
                wifiTileProvider,
                internetTileProvider,
                bluetoothTileProvider,
                cellularTileProvider,
                dndTileProvider,
                colorInversionTileProvider,
                airplaneModeTileProvider,
                workModeTileProvider,
                rotationLockTileProvider,
                flashlightTileProvider,
                locationTileProvider,
                castTileProvider,
                hotspotTileProvider,
                userTileProvider,
                () -> batterySaverTileGoogleProvider.get(),
                dataSaverTileProvider,
                nightDisplayTileProvider,
                nfcTileProvider,
                memoryTileProvider,
                uiModeNightTileProvider,
                screenRecordTileProvider,
                reduceBrightColorsTileProvider,
                cameraToggleTileProvider,
                microphoneToggleTileProvider,
                deviceControlsTileProvider,
                alarmTileProvider,
                quickAccessWalletTileProvider);
        this.mReverseChargingTileProvider = reverseChargingTileProvider;
        this.mBatterySaverTileGoogleProvider = batterySaverTileGoogleProvider;
        this.mOverlayToggleTileProvider = overlayToggleTileProvider;
    }

    @Override
    public QSTile createTile(String str) {
        QSTileImpl createTileInternal = createTileInternal(str);
        if (createTileInternal != null) {
            return createTileInternal;
        }
        return super.createTile(str);
    }

    private QSTileImpl createTileInternal(String str) {
        if (str.equals("ott")) {
            return this.mOverlayToggleTileProvider.get();
        } else if (str.equals("reverse")) {
            return this.mReverseChargingTileProvider.get();
        }
        return null;
    }
}

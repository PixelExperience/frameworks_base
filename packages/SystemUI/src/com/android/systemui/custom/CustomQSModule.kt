package com.android.systemui.custom

import com.android.systemui.qs.tileimpl.QSTileImpl
import com.android.systemui.qs.tiles.PowerShareTile
import com.android.systemui.qs.tiles.CellularTile
import com.android.systemui.qs.tiles.WifiTile
import com.android.systemui.qs.tiles.CaffeineTile
import com.android.systemui.qs.tiles.HeadsUpTile
import com.android.systemui.qs.tiles.SyncTile
import com.android.systemui.qs.tiles.AmbientDisplayTile
import com.android.systemui.qs.tiles.UsbTetherTile
import com.android.systemui.qs.tiles.AODTile
import com.android.systemui.qs.tiles.VpnTile
import com.android.systemui.qs.tiles.LiveDisplayTile
import com.android.systemui.qs.tiles.ReadingModeTile
import com.android.systemui.qs.tiles.AntiFlickerTile

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
interface CustomQSModule {
    /** Inject PowerShareTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(PowerShareTile.TILE_SPEC)
    fun bindPowerShareTile(powerShareTile: PowerShareTile): QSTileImpl<*>

    /** Inject CellularTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(CellularTile.TILE_SPEC)
    fun bindCellularTile(cellularTile: CellularTile): QSTileImpl<*>

    /** Inject WifiTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(WifiTile.TILE_SPEC)
    fun bindWifiTile(wifiTile: WifiTile): QSTileImpl<*>

    /** Inject CaffeineTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(CaffeineTile.TILE_SPEC)
    fun bindCaffeineTile(caffeineTile: CaffeineTile): QSTileImpl<*>

    /** Inject HeadsUpTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(HeadsUpTile.TILE_SPEC)
    fun bindHeadsUpTile(headsUpTile: HeadsUpTile): QSTileImpl<*>

    /** Inject SyncTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(SyncTile.TILE_SPEC)
    fun bindSyncTile(syncTile: SyncTile): QSTileImpl<*>

    /** Inject AmbientDisplayTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(AmbientDisplayTile.TILE_SPEC)
    fun bindAmbientDisplayTile(ambientDisplayTile: AmbientDisplayTile): QSTileImpl<*>

    /** Inject UsbTetherTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(UsbTetherTile.TILE_SPEC)
    fun bindUsbTetherTile(usbTetherTile: UsbTetherTile): QSTileImpl<*>

    /** Inject AODTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(AODTile.TILE_SPEC)
    fun bindAODTile(aodTile: AODTile): QSTileImpl<*>

    /** Inject VpnTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(VpnTile.TILE_SPEC)
    fun bindVpnTile(vpnTile: VpnTile): QSTileImpl<*>

    /** Inject LiveDisplayTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(LiveDisplayTile.TILE_SPEC)
    fun bindLiveDisplayTile(liveDisplayTile: LiveDisplayTile): QSTileImpl<*>

    /** Inject ReadingModeTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(ReadingModeTile.TILE_SPEC)
    fun bindReadingModeTile(readingModeTile: ReadingModeTile): QSTileImpl<*>

    /** Inject AntiFlickerTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(AntiFlickerTile.TILE_SPEC)
    fun bindAntiFlickerTile(antiFlickerTile: AntiFlickerTile): QSTileImpl<*>
}

package com.android.systemui.statusbar.policy;

import java.text.DecimalFormat;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkStats;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;

import java.util.HashMap;

/*
 *
 * Seeing how an Integer object in java requires at least 16 Bytes, it seemed awfully wasteful
 * to only use it for a single boolean. 32-bits is plenty of room for what we need it to do.
 *
 */
public class NetworkTraffic extends TextView {

    private static final String TAG = "NetworkTraffic";

    private static final boolean DEBUG = false;

    private static final int MESSAGE_TYPE_PERIODIC_REFRESH = 0;
    private static final int MESSAGE_TYPE_UPDATE_VIEW = 1;
    private static final int MESSAGE_TYPE_ADD_NETWORK = 2;
    private static final int MESSAGE_TYPE_REMOVE_NETWORK = 3;

    private static final int REFRESH_INTERVAL = 2000;

    // Thresholds themselves are always defined in kbps
    private static final long AUTOHIDE_THRESHOLD = 10;

    protected static final int MODE_DISABLED = 0;
    protected static final int MODE_STATUS_BAR = 1;
    private static final int MODE_QS = 2;

    private static final int UNIT_TYPE_BYTES = 0;
    private static final int UNIT_TYPE_BITS = 1;

    private long mTxKbps;
    private long mRxKbps;
    private long mLastTxBytes;
    private long mLastRxBytes;
    private long mLastUpdateTime;
    private int mTxtSizeStatusbar;
    private int mTxtImgPadding;
    private boolean mAutoHide;
    private boolean mScreenOn = true;
    private int mUnitType = UNIT_TYPE_BYTES;
    private boolean mIndicatorUp = false;
    private boolean mIndicatorDown = false;
    private SettingsObserver mObserver;

    protected boolean mAttached;
    protected int mTintColor;
    protected int mMode;
    protected boolean mTrafficVisible = false;

    // Network tracking related variables
    private final ConnectivityManager mConnectivityManager;
    private final HashMap<Network, LinkProperties> mLinkPropertiesMap = new HashMap<>();

    // Used to indicate that the set of sources contributing
    // to current stats have changed.
    private boolean mNetworksChanged = true;

    private INetworkManagementService mNetworkManagementService;

    private Handler mTrafficHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TYPE_PERIODIC_REFRESH:
                    recalculateStats();
                    displayStatsAndReschedule();
                    break;

                case MESSAGE_TYPE_UPDATE_VIEW:
                    displayStatsAndReschedule();
                    break;

                case MESSAGE_TYPE_ADD_NETWORK:
                    final LinkPropertiesHolder lph = (LinkPropertiesHolder) msg.obj;
                    mLinkPropertiesMap.put(lph.getNetwork(), lph.getLinkProperties());
                    mNetworksChanged = true;
                    break;

                case MESSAGE_TYPE_REMOVE_NETWORK:
                    mLinkPropertiesMap.remove((Network) msg.obj);
                    mNetworksChanged = true;
                    break;
            }
        }

        private void recalculateStats() {
            final long now = SystemClock.elapsedRealtime();
            final long timeDelta = now - mLastUpdateTime; /* ms */
            if (timeDelta < REFRESH_INTERVAL * 0.95f) {
                return;
            }
            // Sum tx and rx bytes from all sources of interest
            long txBytes = 0;
            long rxBytes = 0;
            // Add interface stats
            for (LinkProperties linkProperties : mLinkPropertiesMap.values()) {
                final String iface = linkProperties.getInterfaceName();
                if (iface == null) {
                    continue;
                }
                final long ifaceTxBytes = TrafficStats.getTxBytes(iface);
                final long ifaceRxBytes = TrafficStats.getRxBytes(iface);
                if (DEBUG) {
                    Log.d(TAG, "adding stats from interface " + iface
                            + " txbytes " + ifaceTxBytes + " rxbytes " + ifaceRxBytes);
                }
                txBytes += ifaceTxBytes;
                rxBytes += ifaceRxBytes;
            }

            // Add tether hw offload counters since these are
            // not included in netd interface stats.
            final TetheringStats tetheringStats = getOffloadTetheringStats();
            txBytes += tetheringStats.txBytes;
            rxBytes += tetheringStats.rxBytes;

            if (DEBUG) {
                Log.d(TAG, "mNetworksChanged = " + mNetworksChanged);
                Log.d(TAG, "tether hw offload txBytes: " + tetheringStats.txBytes
                        + " rxBytes: " + tetheringStats.rxBytes);
            }

            final long txBytesDelta = txBytes - mLastTxBytes;
            final long rxBytesDelta = rxBytes - mLastRxBytes;

            if (!mNetworksChanged && timeDelta > 0 && txBytesDelta >= 0 && rxBytesDelta >= 0) {
                mTxKbps = (long) (txBytesDelta * 8f / 1000f / (timeDelta / 1000f));
                mRxKbps = (long) (rxBytesDelta * 8f / 1000f / (timeDelta / 1000f));
            } else if (mNetworksChanged) {
                mTxKbps = 0;
                mRxKbps = 0;
                mNetworksChanged = false;
            }
            mLastTxBytes = txBytes;
            mLastRxBytes = rxBytes;
            mLastUpdateTime = now;
        }

        private void displayStatsAndReschedule() {
            final boolean enabled = mMode != MODE_DISABLED && mScreenOn
                    && isConnectionAvailable();
            final boolean shouldHide = !enabled ||
                    (mAutoHide &&
                        mRxKbps < AUTOHIDE_THRESHOLD && mTxKbps < AUTOHIDE_THRESHOLD);

            if (shouldHide) {
                setText("");
                mTrafficVisible = false;
            } else if (mTxKbps > mRxKbps) {
                // Show information for uplink if it's called for
                String output = formatOutput(mTxKbps * 125 /* Convert kilobit to bytes */);

                // Update view if there's anything new to show
                if (!output.contentEquals(getText())) {
                    setText(output);
                    mIndicatorUp = mTxKbps != 0;
                    mIndicatorDown = false;
                }
                mTrafficVisible = true;
            } else {
                // Add information for downlink if it's called for
                String output = formatOutput(mRxKbps * 125 /* Convert kilobit to bytes */);

                // Update view if there's anything new to show
                if (!output.contentEquals(getText())) {
                    setText(output);
                    mIndicatorDown = mRxKbps != 0;
                    mIndicatorUp = false;
                }
                mTrafficVisible = true;
            }
            updateVisibility();
            updateTrafficDrawable();

            // Schedule periodic refresh
            mTrafficHandler.removeMessages(MESSAGE_TYPE_PERIODIC_REFRESH);
            if (enabled) {
                mTrafficHandler.sendEmptyMessageDelayed(MESSAGE_TYPE_PERIODIC_REFRESH,
                        REFRESH_INTERVAL);
            }
        }

        private String formatOutput(long size) {
            String[] units = new String[]{"", "kB/s", "MB/s", "GB/s", "TB/s", "PB/s"};
            int mod = 1024;
            if (mUnitType == UNIT_TYPE_BITS){
                units = new String[]{"", "kbps", "Mbps", "Gbps", "Tbps", "Pbps"};
                mod = 1000;
                size = size * 8;
            }
            double power = (size > 0) ? Math.floor(Math.log(size) / Math.log(mod)) : 0;
            String unit = units[(int) power];
            String separator = getMyMode() == MODE_STATUS_BAR ? "\n" : " ";
            if (size <= 0) {
                return String.format("%d%s%s", 0, separator, units[1]);
            }else if (unit.equals("")) {
                return String.format("< %d%s%s", 0, separator, units[1]);
            }
            double result = size / Math.pow(mod, power);
            return String.format("%d%s%s", (int) result, separator, unit);
        }
    };

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.NETWORK_TRAFFIC_LOCATION), false,
                    this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.NETWORK_TRAFFIC_AUTOHIDE), false,
                    this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.NETWORK_TRAFFIC_UNIT_TYPE), false,
                    this, UserHandle.USER_ALL);
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public NetworkTraffic(Context context) {
        this(context, null);
    }

    public NetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final Resources resources = getResources();
        mTxtSizeStatusbar = resources.getDimensionPixelSize(R.dimen.net_traffic_status_bar_text_size);
        mTxtImgPadding = resources.getDimensionPixelSize(R.dimen.net_traffic_txt_img_padding);
        mTintColor = resources.getColor(android.R.color.white);
        setTextColor(mTintColor);

        mNetworkManagementService = INetworkManagementService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));

        mConnectivityManager = getContext().getSystemService(ConnectivityManager.class);
        final NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build();
        mConnectivityManager.registerNetworkCallback(request, mNetworkCallback);
        mObserver = new SettingsObserver(mTrafficHandler);
        setVisibility(View.GONE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            mContext.registerReceiver(mIntentReceiver, filter, null, getHandler());
            mObserver.observe();
            updateSettings();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mContext.unregisterReceiver(mIntentReceiver);
            mObserver.unobserve();
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                mScreenOn = true;
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mScreenOn = false;
            }
            updateViewState();
        }
    };

    private boolean isConnectionAvailable() {
        ConnectivityManager connManager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = (connManager != null) ? connManager.getActiveNetworkInfo() : null;
        return network != null;
    }

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mMode = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_LOCATION, 0,
                UserHandle.USER_CURRENT);
        mAutoHide = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_AUTOHIDE, 0, UserHandle.USER_CURRENT) == 1;
        mUnitType = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_UNIT_TYPE, UNIT_TYPE_BYTES,
                UserHandle.USER_CURRENT);
        updateViewState();
    }

    protected void updateViewState() {
        mTrafficHandler.removeMessages(MESSAGE_TYPE_UPDATE_VIEW);
        mTrafficHandler.sendEmptyMessage(MESSAGE_TYPE_UPDATE_VIEW);
    }

    protected void updateTrafficDrawable() {
        int indicatorDrawable;
        if (mMode == getMyMode() && mTrafficVisible) {
            if (mIndicatorUp) {
                indicatorDrawable = R.drawable.stat_sys_network_traffic_up_arrow;
            } else if (mIndicatorDown) {
                indicatorDrawable = R.drawable.stat_sys_network_traffic_down_arrow;
            } else {
                indicatorDrawable = R.drawable.stat_sys_network_traffic;
            }
            Drawable d = getContext().getDrawable(indicatorDrawable);
            d.setColorFilter(mTintColor, Mode.MULTIPLY);
            setCompoundDrawablePadding(mTxtImgPadding);
            setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
            mIndicatorUp = false;
            mIndicatorDown = false;
        } else {
            setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }
    }

    public void onDensityOrFontScaleChanged() {
        final Resources resources = getResources();
        mTxtSizeStatusbar = resources.getDimensionPixelSize(R.dimen.net_traffic_status_bar_text_size);
        mTxtImgPadding = resources.getDimensionPixelSize(R.dimen.net_traffic_txt_img_padding);
        setCompoundDrawablePadding(mTxtImgPadding);
    }

    protected void updateVisibility() {
        if (mMode != MODE_DISABLED && mMode == getMyMode() && mTrafficVisible) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

    protected int getMyMode(){
        return MODE_QS;
    }

    private class TetheringStats {
        long txBytes;
        long rxBytes;
    }

    private TetheringStats getOffloadTetheringStats() {
        TetheringStats tetheringStats = new TetheringStats();

        NetworkStats stats = null;
        try {
            // STATS_PER_UID returns hw offload and netd stats combined (as entry UID_TETHERING)
            // STATS_PER_IFACE returns only hw offload stats (as entry UID_ALL)
            stats = mNetworkManagementService.getNetworkStatsTethering(
                    NetworkStats.STATS_PER_IFACE);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to call getNetworkStatsTethering: " + e);
        }
        if (stats == null) {
            // nothing we can do except return zero stats
            return tetheringStats;
        }

        NetworkStats.Entry entry = null;
        // Entries here are per tethered interface.
        // Counters persist even after tethering has been disabled.
        for (int i = 0; i < stats.size(); i++) {
            entry = stats.getValues(i, entry);
            if (DEBUG) {
                Log.d(TAG, "tethering stats entry: " + entry);
            }
            // hw offload tether stats are reported under UID_ALL.
            if (entry.uid == NetworkStats.UID_ALL) {
                tetheringStats.txBytes += entry.txBytes;
                tetheringStats.rxBytes += entry.rxBytes;
            }
        }
        return tetheringStats;
    }

    private ConnectivityManager.NetworkCallback mNetworkCallback =
            new ConnectivityManager.NetworkCallback() {

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            Message msg = new Message();
            msg.what = MESSAGE_TYPE_ADD_NETWORK;
            msg.obj = new LinkPropertiesHolder(network, linkProperties);
            mTrafficHandler.sendMessage(msg);
        }

        @Override
        public void onLost(Network network) {
            Message msg = new Message();
            msg.what = MESSAGE_TYPE_REMOVE_NETWORK;
            msg.obj = network;
            mTrafficHandler.sendMessage(msg);
        }
    };

    private class LinkPropertiesHolder {
        private Network mNetwork;
        private LinkProperties mLinkProperties;

        public LinkPropertiesHolder(Network network, LinkProperties linkProperties) {
            mNetwork = network;
            mLinkProperties = linkProperties;
        }

        public LinkPropertiesHolder(Network network) {
            mNetwork = network;
        }

        public Network getNetwork() {
            return mNetwork;
        }

        public LinkProperties getLinkProperties() {
            return mLinkProperties;
        }
    }

    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        mTintColor = tint;
        setTextColor(mTintColor);
        updateTrafficDrawable();
    }
}

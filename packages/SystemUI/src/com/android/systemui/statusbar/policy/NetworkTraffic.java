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
import android.graphics.Typeface;
import android.view.Gravity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;

/*
 *
 * Seeing how an Integer object in java requires at least 16 Bytes, it seemed awfully wasteful
 * to only use it for a single boolean. 32-bits is plenty of room for what we need it to do.
 *
 */
public class NetworkTraffic extends TextView {

    private static final int INTERVAL = 1500; //ms

    private static final int UNITS_KILOBITS = 0;
    private static final int UNITS_MEGABITS = 1;
    private static final int UNITS_KILOBYTES = 2;
    private static final int UNITS_MEGABYTES = 3;

    // Thresholds themselves are always defined in kbps
    private static final long AUTOHIDE_THRESHOLD_KILOBITS  = 10;
    private static final long AUTOHIDE_THRESHOLD_MEGABITS  = 100;
    private static final long AUTOHIDE_THRESHOLD_KILOBYTES = 8;
    private static final long AUTOHIDE_THRESHOLD_MEGABYTES = 80;

    protected int mIsEnabled;
    private boolean mAttached;
    private long totalRxBytes;
    private long totalTxBytes;
    private long lastUpdateTime;
    private int txtSize;
    private int txtImgPadding;
    private boolean mAutoHide;
    private long mAutoHideThreshold;
    private int mUnits;
    protected int mTintColor;
    protected boolean mTrafficVisible = false;
    private boolean indicatorUp = false;
    private boolean indicatorDown = false;
    private String txtFont;

    private boolean mScreenOn = true;

    private Handler mTrafficHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime;

            if (timeDelta < INTERVAL * .95) {
                if (msg.what != 1) {
                    // we just updated the view, nothing further to do
                    return;
                }
                if (timeDelta < 1) {
                    // Can't div by 0 so make sure the value displayed is minimal
                    timeDelta = Long.MAX_VALUE;
                }
            }
            lastUpdateTime = SystemClock.elapsedRealtime();

            // Calculate the data rate from the change in total bytes and time
            long newTotalRxBytes = TrafficStats.getTotalRxBytes();
            long newTotalTxBytes = TrafficStats.getTotalTxBytes();
            long rxData = newTotalRxBytes - totalRxBytes;
            long txData = newTotalTxBytes - totalTxBytes;

            if (shouldHide(rxData, txData, timeDelta)) {
                setText("");
                mTrafficVisible = false;
            } else if (shouldShowUpload(rxData, txData, timeDelta)) {
                // Show information for uplink if it's called for
                String output = formatOutput(timeDelta, txData);

                // Update view if there's anything new to show
                if (!output.contentEquals(getText())) {
                    txtFont = getResources().getString(com.android.internal.R.string.config_headlineFontFamilyMedium);
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)txtSize);
                    setTypeface(Typeface.create(txtFont, Typeface.NORMAL));
                    setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                    setText(output);
                    indicatorUp = true;
                }
                mTrafficVisible = true;
            } else {
                // Add information for downlink if it's called for
                String output = formatOutput(timeDelta, rxData);

                // Update view if there's anything new to show
                if (!output.contentEquals(getText())) {
                    txtFont = getResources().getString(com.android.internal.R.string.config_headlineFontFamilyMedium);
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)txtSize);
		            setTypeface(Typeface.create(txtFont, Typeface.NORMAL));
                    setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                    setText(output);
                    indicatorDown = true;
                }
                mTrafficVisible = true;
            }
            updateVisibility();
            updateTrafficDrawable();

            // Post delayed message to refresh in ~1000ms
            totalRxBytes = newTotalRxBytes;
            totalTxBytes = newTotalTxBytes;
            clearHandlerCallbacks();
            mTrafficHandler.postDelayed(mRunnable, INTERVAL);
        }

        private String formatOutput(long timeDelta, long data) {
            long kbps = (long)(data / (timeDelta / 1000F));
            final String value;
            final String unit;
            switch (mUnits) {
                case UNITS_KILOBITS:
                    value = String.format("%d", kbps);
                    unit = mContext.getString(R.string.kilobitspersecond_short);
                    break;
                case UNITS_MEGABITS:
                    value = String.format("%.1f", (float) kbps / 1000);
                    unit = mContext.getString(R.string.megabitspersecond_short);
                    break;
                case UNITS_KILOBYTES:
                    value = String.format("%d", kbps / 8);
                    unit = mContext.getString(R.string.kilobytespersecond_short);
                    break;
                case UNITS_MEGABYTES:
                    value = String.format("%.2f", (float) kbps / 8000);
                    unit = mContext.getString(R.string.megabytespersecond_short);
                    break;
                default:
                    value = "unknown";
                    unit = "unknown";
                    break;
            }

            return value + " " + unit;
        }

        private boolean shouldHide(long rxData, long txData, long timeDelta) {
            long speedRx = (long)(rxData / (timeDelta / 1000f));
            long speedTx = (long)(txData / (timeDelta / 1000f));
            return !getConnectAvailable() ||
                    (mAutoHide &&
                    speedRx < mAutoHideThreshold && speedTx < mAutoHideThreshold);
        }

        private boolean shouldShowUpload(long rxData, long txData, long timeDelta) {
	        long speedRxKB = (long)(rxData / (timeDelta / 1000f));
            long speedTxKB = (long)(txData / (timeDelta / 1000f));
	        return (speedTxKB > speedRxKB);
	    }
    };

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mTrafficHandler.sendEmptyMessage(0);
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
                    .getUriFor(Settings.System.NETWORK_TRAFFIC_UNITS), false,
                    this, UserHandle.USER_ALL);
        }

        /*
         *  @hide
         */
        @Override
        public void onChange(boolean selfChange) {
            setMode();
            updateSettings();
        }
    }

    /*
     *  @hide
     */
    public NetworkTraffic(Context context) {
        this(context, null);
    }

    /*
     *  @hide
     */
    public NetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /*
     *  @hide
     */
    public NetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final Resources resources = getResources();
        txtSize = resources.getDimensionPixelSize(R.dimen.net_traffic_multi_text_size);
        txtImgPadding = resources.getDimensionPixelSize(R.dimen.net_traffic_txt_img_padding);
        mTintColor = resources.getColor(android.R.color.white);
        setTextColor(mTintColor);
        Handler mHandler = new Handler();
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();
        setMode();
        updateSettings();
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
        }
        updateSettings();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mContext.unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) && mScreenOn) {
                updateSettings();
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                mScreenOn = true;
                updateSettings();
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mScreenOn = false;
                clearHandlerCallbacks();
            }
        }
    };

    private boolean getConnectAvailable() {
        ConnectivityManager connManager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = (connManager != null) ? connManager.getActiveNetworkInfo() : null;
        return network != null;
    }

    private void updateSettings() {
        updateVisibility();
        if (mIsEnabled == 2) {
            if (mAttached) {
                totalRxBytes = TrafficStats.getTotalRxBytes();
                lastUpdateTime = SystemClock.elapsedRealtime();
                mTrafficHandler.sendEmptyMessage(1);
            }
            updateTrafficDrawable();
            return;
        } else {
            clearHandlerCallbacks();
        }
    }

    private void setMode() {
        ContentResolver resolver = mContext.getContentResolver();
        mIsEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_LOCATION, 0,
                UserHandle.USER_CURRENT);
        mAutoHide = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_AUTOHIDE, 0, UserHandle.USER_CURRENT) == 1;
        mUnits = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_UNITS, /* Mbps */ 1,
                UserHandle.USER_CURRENT);
        switch (mUnits) {
            case UNITS_KILOBITS:
                mAutoHideThreshold = AUTOHIDE_THRESHOLD_KILOBITS;
                break;
            case UNITS_MEGABITS:
                mAutoHideThreshold = AUTOHIDE_THRESHOLD_MEGABITS;
                break;
            case UNITS_KILOBYTES:
                mAutoHideThreshold = AUTOHIDE_THRESHOLD_KILOBYTES;
                break;
            case UNITS_MEGABYTES:
                mAutoHideThreshold = AUTOHIDE_THRESHOLD_MEGABYTES;
                break;
            default:
                mAutoHideThreshold = 0;
                break;
        }
    }

    private void clearHandlerCallbacks() {
        mTrafficHandler.removeCallbacks(mRunnable);
        mTrafficHandler.removeMessages(0);
        mTrafficHandler.removeMessages(1);
    }

    private void updateTrafficDrawable() {
        int indicatorDrawable;
        if (mIsEnabled == 2) {
            if (indicatorUp) {
                indicatorDrawable = R.drawable.stat_sys_network_traffic_up_arrow;
                Drawable d = getContext().getDrawable(indicatorDrawable);
                d.setColorFilter(mTintColor, Mode.MULTIPLY);
                setCompoundDrawablePadding(txtImgPadding);
                setCompoundDrawablesWithIntrinsicBounds(null, null, d, null);
            } else if (indicatorDown) {
                indicatorDrawable = R.drawable.stat_sys_network_traffic_down_arrow;
                Drawable d = getContext().getDrawable(indicatorDrawable);
                d.setColorFilter(mTintColor, Mode.MULTIPLY);
                setCompoundDrawablePadding(txtImgPadding);
                setCompoundDrawablesWithIntrinsicBounds(null, null, d, null);
            } else {
                setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
	    }
        indicatorUp = false;
        indicatorDown = false;
    }

    public void onDensityOrFontScaleChanged() {
        final Resources resources = getResources();
        txtSize = resources.getDimensionPixelSize(R.dimen.net_traffic_multi_text_size);
        txtImgPadding = resources.getDimensionPixelSize(R.dimen.net_traffic_txt_img_padding);
        txtFont = resources.getString(com.android.internal.R.string.config_headlineFontFamilyMedium);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)txtSize);
        setCompoundDrawablePadding(txtImgPadding);
        setTypeface(Typeface.create(txtFont, Typeface.NORMAL));
    }

    protected void updateVisibility() {
        if ((mIsEnabled == 2) && mTrafficVisible) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }
}

package com.google.android.systemui.keyguard;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.graphics.drawable.Icon;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.HeaderBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import com.android.systemui.R;
import com.android.systemui.keyguard.KeyguardSliceProvider;
import com.google.android.systemui.smartspace.SmartSpaceCard;
import com.google.android.systemui.smartspace.SmartSpaceController;
import com.google.android.systemui.smartspace.SmartSpaceData;
import com.google.android.systemui.smartspace.SmartSpaceUpdateListener;
import java.lang.ref.WeakReference;

public class KeyguardSliceProviderGoogle extends KeyguardSliceProvider implements SmartSpaceUpdateListener {
    private SmartSpaceData mSmartSpaceData;
    private final Uri mSmartSpaceMainUri = Uri.parse("content://com.android.systemui.keyguard/smartSpace/main");
    private final Uri mSmartSpaceSecondaryUri = Uri.parse("content://com.android.systemui.keyguard/smartSpace/secondary");
    private final Uri mWeatherUri = Uri.parse("content://com.android.systemui.keyguard/smartSpace/weather");
    private final Object mLock = new Object();
    private boolean mHideSensitiveContent;

    private static class AddShadowTask extends AsyncTask<Bitmap, Void, Bitmap> {
        private final float mBlurRadius;
        private final WeakReference<KeyguardSliceProviderGoogle> mProviderReference;
        private final SmartSpaceCard mWeatherCard;

        AddShadowTask(KeyguardSliceProviderGoogle keyguardSliceProviderGoogle, SmartSpaceCard smartSpaceCard) {
            mProviderReference = new WeakReference<>(keyguardSliceProviderGoogle);
            mWeatherCard = smartSpaceCard;
            mBlurRadius = keyguardSliceProviderGoogle.getContext().getResources().getDimension(R.dimen.smartspace_icon_shadow);
        }

        @Override
        public Bitmap doInBackground(Bitmap... bitmapArr) {
            return applyShadow(bitmapArr[0]);
        }

        @Override
        public void onPostExecute(Bitmap bitmap) {
            KeyguardSliceProviderGoogle keyguardSliceProviderGoogle;
            mWeatherCard.setIcon(bitmap);
            keyguardSliceProviderGoogle = (KeyguardSliceProviderGoogle) mProviderReference.get();
            if (keyguardSliceProviderGoogle != null) {
                keyguardSliceProviderGoogle.notifyChange();
            }
        }

        private Bitmap applyShadow(Bitmap bitmap) {
            BlurMaskFilter blurMaskFilter = new BlurMaskFilter(mBlurRadius, Blur.NORMAL);
            Paint paint = new Paint();
            paint.setMaskFilter(blurMaskFilter);
            int[] iArr = new int[2];
            Bitmap extractAlpha = bitmap.extractAlpha(paint, iArr);
            Bitmap createBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(createBitmap);
            Paint paint2 = new Paint();
            paint2.setAlpha(70);
            canvas.drawBitmap(extractAlpha, (float) iArr[0], ((float) iArr[1]) + (mBlurRadius / 2.0f), paint2);
            extractAlpha.recycle();
            paint2.setAlpha(255);
            canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint2);
            return createBitmap;
        }
    }

    @Override
    public boolean onCreateSliceProvider() {
        boolean onCreateSliceProvider = super.onCreateSliceProvider();
        SmartSpaceController.get(getContext()).addListener(this);
        mSmartSpaceData = new SmartSpaceData();
        return onCreateSliceProvider;
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        boolean hideSensitiveData;
        SmartSpaceCard currentCard = mSmartSpaceData.getCurrentCard();
        synchronized (mLock) {
            hideSensitiveData = mHideSensitiveContent;
        }
        ListBuilder listBuilder = new ListBuilder(getContext(), mSliceUri);
        if (isDndSuppressingNotifications() || currentCard == null || currentCard.isExpired() || TextUtils.isEmpty(currentCard.getTitle()) || hideSensitiveData) {
            listBuilder.addRow(new RowBuilder(listBuilder, mDateUri).setTitle(getFormattedDate()));
        } else {
            HeaderBuilder headerBuilder = new HeaderBuilder(listBuilder, mSmartSpaceMainUri).setTitle(currentCard.getTitle());
            RowBuilder contentBuilder = new RowBuilder(listBuilder, mSmartSpaceSecondaryUri).setTitle(currentCard.getSubtitle());
            Bitmap icon = currentCard.getIcon();
            if (icon != null) {
                contentBuilder.addEndItem(Icon.createWithBitmap(icon));
            }
            listBuilder.setHeader(headerBuilder).addRow(contentBuilder);
        }
        addWeather(listBuilder);
        addNextAlarm(listBuilder);
        addZenMode(listBuilder);
        addPrimaryAction(listBuilder);
        return listBuilder.build();
    }

    @Override
    public void onSensitiveModeChanged(boolean hidePrivateData) {
        boolean changed = false;
        synchronized (mLock) {
            if (mHideSensitiveContent != hidePrivateData) {
                mHideSensitiveContent = hidePrivateData;
                changed = true;
            }
        }
        if (changed) {
            notifyChange();
        }
    }

    private void addWeather(ListBuilder listBuilder) {
        SmartSpaceCard weatherCard = mSmartSpaceData.getWeatherCard();
        if (weatherCard != null && !weatherCard.isExpired()) {
            RowBuilder rowBuilder = new RowBuilder(listBuilder, mWeatherUri);
            rowBuilder.setTitle(weatherCard.getTitle());
            Bitmap icon = weatherCard.getIcon();
            if (icon != null) {
                Icon createWithBitmap = Icon.createWithBitmap(icon);
                createWithBitmap.setTintMode(Mode.DST);
                rowBuilder.addEndItem(createWithBitmap, 1);
            }
            listBuilder.addRow(rowBuilder);
        }
    }

    @Override
    public void onSmartSpaceUpdated(SmartSpaceData smartSpaceData) {
        mSmartSpaceData = smartSpaceData;
        SmartSpaceCard weatherCard = smartSpaceData.getWeatherCard();
        if (weatherCard == null || weatherCard.getIcon() == null || weatherCard.isIconProcessed()) {
            notifyChange();
            return;
        }
        weatherCard.setIconProcessed(true);
        new AddShadowTask(this, weatherCard).execute(new Bitmap[]{weatherCard.getIcon()});
    }

    @Override
    public void updateClock() {
        notifyChange();
    }

    public void notifyChange() {
        getContext().getContentResolver().notifyChange(mSliceUri, null);
    }
}

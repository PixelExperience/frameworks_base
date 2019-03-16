/*
 * Copyright (C) 2018 CypherOS
 * Copyright (C) 2019 PixelExperience
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, see <http://www.gnu.org/licenses>.
 */
package com.android.systemui.ambient.play;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.text.TextUtils;
import java.util.Locale;

import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBar;

public class AmbientIndicationContainer extends AutoReinflateContainer {
    private View mAmbientIndication;
    private boolean mDozing;
    private CharSequence mIndication;
    private StatusBar mStatusBar;
    private TextView mText;
    private Context mContext;

    private String mSong;
    private String mArtist;
    
    private AnimatedVectorDrawable mAnimatedIcon;

    public AmbientIndicationContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mContext = context;
    }

    public void hideIndication() {
        if (mAmbientIndication != null){
            mAmbientIndication.setVisibility(View.GONE);
            mAnimatedIcon.stop();
            mText.setSelected(false);
        }
    }

    public void showIndication() {
        if (mAmbientIndication != null && mSong != null && mArtist != null){
            mAmbientIndication.setVisibility(View.VISIBLE);
            mAmbientIndication.setClickable(false);
            boolean rtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL;
            mText.setCompoundDrawables(rtl ? null : mAnimatedIcon, null, rtl ? mAnimatedIcon : null, null);
            mText.setText(String.format(mContext.getResources().getString(
                    com.android.internal.R.string.ambient_recognition_information), mSong, mArtist));
            mText.setSelected(true);
            mAnimatedIcon.start();
        }
    }

    public void initializeView(StatusBar statusBar) {
        mStatusBar = statusBar;
        addInflateListener(new AmbientIndicationInflateListener(this));
    }

    public void updateAmbientIndicationView(View view) {
        mAmbientIndication = findViewById(R.id.ambient_indication);
        mText = (TextView) findViewById(R.id.ambient_indication_text);
        int iconSize = mContext.getResources().getDimensionPixelSize(R.dimen.ambient_indication_icon_size);
        mAnimatedIcon = (AnimatedVectorDrawable) mContext.getDrawable(R.drawable.audioanim_animation).getConstantState().newDrawable();
        mAnimatedIcon.setBounds(0, 0, iconSize, iconSize);
        setIndication(mSong, mArtist);
    }

    public void setIndication(String song, String artist) {
        mSong = song;
        mArtist = artist;
        if (mSong == null || mArtist == null){
            hideIndication();
        }
    }

    private class AmbientIndicationInflateListener implements AutoReinflateContainer.InflateListener {
        private Object mContainer;

        public AmbientIndicationInflateListener(Object object) {
            mContainer = object;
        }

        private void setAmbientIndicationView(View view) {
            ((AmbientIndicationContainer) mContainer).updateAmbientIndicationView(view);
        }

        @Override
        public void onInflated(View view) {
            setAmbientIndicationView(view);
        }
    }
}

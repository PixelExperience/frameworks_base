/*
 * Copyright (C) 2017-2018 Google Inc.
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
 * limitations under the License
 */

package com.android.systemui.opa;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.res.Resources;
import android.util.ArraySet;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import com.android.systemui.R;

public final class OpaUtils {

    public static final Interpolator INTERPOLATOR_40_40 = new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f);
    public static final Interpolator INTERPOLATOR_40_OUT = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);

    public static Animator getScaleAnimatorX(View v, float factor, int duration, Interpolator interpolator) {
        RenderNodeAnimator anim = new RenderNodeAnimator(3, factor);
        anim.setTarget(v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    public static Animator getScaleAnimatorY(View v, float factor, int duration, Interpolator interpolator) {
        RenderNodeAnimator anim = new RenderNodeAnimator(4, factor);
        anim.setTarget(v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    public static Animator getDeltaAnimatorX(View v, Interpolator interpolator, float deltaX, int duration) {
        RenderNodeAnimator anim = new RenderNodeAnimator(8, v.getX() + deltaX);
        anim.setTarget(v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    public static Animator getDeltaAnimatorY(View v, Interpolator interpolator, float deltaY, int duration) {
        RenderNodeAnimator anim = new RenderNodeAnimator(9, v.getY() + deltaY);
        anim.setTarget(v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    public static Animator getTranslationAnimatorX(View v, Interpolator interpolator, int duration) {
        RenderNodeAnimator anim = new RenderNodeAnimator(0, 0.0f);
        anim.setTarget(v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    public static Animator getTranslationAnimatorY(View v, Interpolator interpolator, int duration) {
        RenderNodeAnimator anim = new RenderNodeAnimator(1, 0.0f);
        anim.setTarget(v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    public static ObjectAnimator getAlphaObjectAnimator(View v, float alpha, int duration, int delay, Interpolator interpolator) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, View.ALPHA, new float[]{alpha});
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        anim.setStartDelay((long) delay);
        return anim;
    }

    public static Animator getAlphaAnimator(View v, float alpha, int duration, Interpolator interpolator) {
        return getAlphaAnimator(v, alpha, duration, 0, interpolator);
    }

    public static Animator getAlphaAnimator(View v, float alpha, int duration, int startDelay, Interpolator interpolator) {
        RenderNodeAnimator anim = new RenderNodeAnimator(11, alpha);
        anim.setTarget(v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        anim.setStartDelay((long) startDelay);
        return anim;
    }

    public static Animator getLongestAnim(ArraySet<Animator> animators) {
        long longestDuration = Long.MIN_VALUE;
        Animator longestAnim = null;
        for (int i = animators.size() - 1; i >= 0; i--) {
            Animator a = (Animator) animators.valueAt(i);
            if (a.getTotalDuration() > longestDuration) {
                longestAnim = a;
                longestDuration = a.getTotalDuration();
            }
        }
        return longestAnim;
    }

    public static ObjectAnimator getScaleObjectAnimator(View v, float factor, int duration, Interpolator interpolator) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, new float[]{factor});
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, new float[]{factor});
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(v, scaleX, scaleY);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    public static ObjectAnimator getTranslationObjectAnimatorY(View v, Interpolator interpolator, float deltaY, float startY, int duration) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, View.Y, new float[]{startY, startY + deltaY});
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    public static ObjectAnimator getTranslationObjectAnimatorX(View v, Interpolator interpolator, float deltaX, float startX, int duration) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, View.X, new float[]{startX, startX + deltaX});
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    public static float getPxVal(Resources resources, int id) {
        return (float) resources.getDimensionPixelOffset(id);
    }

    public static float getDeltaDiamondPositionTopX() {
        return 0.0f;
    }

    public static float getDeltaDiamondPositionTopY(Resources resources) {
        return -getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    public static float getDeltaDiamondPositionLeftX(Resources resources) {
        return -getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    public static float getDeltaDiamondPositionLeftY() {
        return 0.0f;
    }

    public static float getDeltaDiamondPositionRightX(Resources resources) {
        return getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    public static float getDeltaDiamondPositionRightY() {
        return 0.0f;
    }

    public static float getDeltaDiamondPositionBottomX() {
        return 0.0f;
    }

    public static float getDeltaDiamondPositionBottomY(Resources resources) {
        return getPxVal(resources, R.dimen.opa_diamond_translation);
    }
}

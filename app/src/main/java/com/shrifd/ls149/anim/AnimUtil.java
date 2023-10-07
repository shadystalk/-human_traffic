package com.shrifd.ls149.anim;

/*
 * author : 万涛
 * date : 2020/8/6 17:18
 */

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.View;
import android.widget.TextView;

import java.text.NumberFormat;

public class AnimUtil {


    public static void moveKeyView(View view, int start, int end) {
        PropertyValuesHolder tan = PropertyValuesHolder.ofKeyframe(View.TRANSLATION_Y,
                Keyframe.ofFloat(0f, start),
                Keyframe.ofFloat(1.0f, end)
        );
        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(view, tan);
        objectAnimator.setDuration(500);
        objectAnimator.start();
    }

    /**
     * @param tv TextView
     */
    public static void setText(TextView tv, int mStartValue, int mEndValue, NumberFormat numberFormat) {
        if (tv.getTag() != null && tv.getTag() instanceof ValueAnimator) {
            ValueAnimator animator = (ValueAnimator) tv.getTag();
            animator.cancel();
        }
        ValueAnimator animator = ValueAnimator.ofInt(mStartValue, mEndValue);
        tv.setTag(animator);
        animator.setDuration(1000);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int integer = (Integer) animation.getAnimatedValue();
                tv.setText(numberFormat.format(integer));
            }
        });
        animator.start();

    }


}

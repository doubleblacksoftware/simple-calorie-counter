package com.android.calculator2;

import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

public class RevealColorView extends ViewGroup {

    private static final float SCALE = 8f;

    private View inkView;
    private ShapeDrawable circle;
    private ViewPropertyAnimator animator;

    public RevealColorView(Context context) {
        this(context, null);
    }

    public RevealColorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RevealColorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (isInEditMode()) {
            return;
        }

        inkView = new View(context);
        addView(inkView);

        circle = new ShapeDrawable(new OvalShape());

        Utils.setBackgroundCompat(inkView, circle);
        ViewHelper.setAlpha(this, 0);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        inkView.layout(left, top, left + inkView.getMeasuredWidth(), top + inkView.getMeasuredHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);

        final float circleSize = (float) Math.sqrt(width * width + height * height) * 2f;
        final int size = (int) (circleSize / SCALE);
        final int sizeSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        inkView.measure(sizeSpec, sizeSpec);
    }

    public Animator createCircularReveal(final int x, final int y, final int color) {
        ViewHelper.setAlpha(this, 1);
        Utils.setLayerTypeCompat(inkView, LAYER_TYPE_SOFTWARE);
        circle.getPaint().setColor(color);

        final float finalScale = calculateScale(x, y) * SCALE;

        prepareView(inkView, x, y, 0);
        ObjectAnimator animatorX = ObjectAnimator.ofFloat(inkView, "scaleX", 0, finalScale);
        ObjectAnimator animatorY = ObjectAnimator.ofFloat(inkView, "scaleY", 0, finalScale);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animatorX, animatorY);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        return animatorSet;
    }

    private void prepareView(View view, int x, int y, float scale) {
        final int centerX = (view.getWidth() / 2);
        final int centerY = (view.getHeight() / 2);
        ViewHelper.setTranslationX(view, x - centerX);
        ViewHelper.setTranslationY(view, y - centerY);
        ViewHelper.setPivotX(view, centerX);
        ViewHelper.setPivotY(view, centerY);
        ViewHelper.setScaleX(view, scale);
        ViewHelper.setScaleY(view, scale);
    }

    /**
     * calculates the required scale of the ink-view to fill the whole view
     *
     * @param x circle center x
     * @param y circle center y
     * @return
     */
    private float calculateScale(int x, int y) {
        final float centerX = getWidth() / 2f;
        final float centerY = getHeight() / 2f;
        final float maxDistance = (float) Math.sqrt(centerX * centerX + centerY * centerY);

        final float deltaX = centerX - x;
        final float deltaY = centerY - y;
        final float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        final float scale = 0.5f + (distance / maxDistance) * 0.5f;
        return scale;
    }
}
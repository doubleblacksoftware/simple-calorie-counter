package com.android.calculator2;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

@SuppressLint("NewApi")

public class Utils {

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static void setLayerTypeCompat(View view, int layerType) {
        if (hasHoneycomb()) {
            view.setLayerType(layerType, null);
        }
    }

    public static void setBackgroundCompat(View view, Drawable drawable) {
        if (hasJellyBean()) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }

    public static void setStatusBarColorCompat(Window window, int color) {
        if (hasLollipop()) {
            window.setStatusBarColor(color);
        }
    }

    public static void removeOnGlobalLayoutListenerCompat(View view, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (hasJellyBean()) {
            view.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        } else {
            view.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        }
    }
}

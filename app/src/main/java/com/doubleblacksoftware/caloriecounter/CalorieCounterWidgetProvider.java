package com.doubleblacksoftware.caloriecounter;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by x on 7/4/15.
 */
public class CalorieCounterWidgetProvider extends AppWidgetProvider {
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // get the number of calories left for today
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Cursor cursor = context.getContentResolver().query(
                Uri.parse(CalorieDataProvider.URL + "/summary/day/" + sdf.format(Calendar.getInstance().getTime())),
                new String[] {"total"},
                null,
                null,
                null);
        cursor.moveToFirst();
        int totalCalories = cursor.getInt(cursor.getColumnIndex("total"));
        cursor.close();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int goalCalories = Integer.parseInt(prefs.getString("preference_calorie_goal", "2000"));

        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            views.setOnClickPendingIntent(R.id.textView, pendingIntent);
            views.setTextViewText(R.id.textView, (goalCalories - totalCalories) + " left");

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}

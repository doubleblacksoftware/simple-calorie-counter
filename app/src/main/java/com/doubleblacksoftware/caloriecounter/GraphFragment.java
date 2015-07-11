package com.doubleblacksoftware.caloriecounter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by x on 6/3/15.
 */
public class GraphFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>{

    private LineChartView hellochart;
    Context context;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.graph, container, false);
        hellochart = (LineChartView) view.findViewById(R.id.hellochart);
        getLoaderManager().initLoader(1, null, this);
        return view;
    }

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private long getTimeFromRow(Cursor cursor) {
        if (cursor.isAfterLast()) {
            return 0;
        }
        String date = cursor.getString(cursor.getColumnIndex("timestamp"));
        Date date1 = null;
        try {
            date1 = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date1.getTime()/1000;
    }

    private void updateGraph(Cursor cursor) {
        // set up the goal line
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int goalCalories = Integer.parseInt(prefs.getString("preference_calorie_goal", "2000"));
        List<PointValue> goalValues = new ArrayList<>();

        // set up the data
        List<PointValue> values = new ArrayList<>();
        cursor.moveToFirst();

        if (!cursor.isAfterLast()) {
            goalValues.add(new PointValue(getTimeFromRow(cursor), goalCalories));
        }

        while (cursor.isAfterLast() == false) {
            float value = cursor.getFloat(cursor.getColumnIndex("total"));
            //Log.d("graph", "adding " + value + " on " + sdf.format(getTimeFromRow(cursor)));
            values.add(new PointValue(getTimeFromRow(cursor), value));
            cursor.moveToNext();
        }

        cursor.moveToLast();
        goalValues.add(new PointValue(getTimeFromRow(cursor), goalCalories));

        List<Line> lines = new ArrayList<Line>();

        Line goalLine = new Line(goalValues).setColor(Color.parseColor("#ff0000"));
        lines.add(goalLine);

        Line line = new Line(values).setColor(Color.parseColor("#44a134")).setCubic(true);
        //line.setFilled(true);
        //line.setHasLabels(true);

        lines.add(line);

        LineChartData data = new LineChartData();
        data.setLines(lines);

        Axis timeAxis = new Axis();
        //timeAxis.setName("");
        //timeAxis.setTextColor(ChartUtils.COLOR_ORANGE);
        timeAxis.setMaxLabelChars(5);
        timeAxis.setFormatter(new TimestampAxisFormatter());
        timeAxis.setHasLines(true);
        timeAxis.setHasTiltedLabels(true);
        data.setAxisXBottom(timeAxis);

        Axis calorieAxis = new Axis();
        //weightAxis.setName("");
        //weightAxis.setTextColor(ChartUtils.COLOR_GREEN);
        calorieAxis.setMaxLabelChars(7);
        calorieAxis.setFormatter(new SimpleAxisValueFormatter().setAppendedText(" C".toCharArray()));
        calorieAxis.setHasLines(true);
        calorieAxis.setHasTiltedLabels(true);
        data.setAxisYLeft(calorieAxis);

        hellochart.setLineChartData(data);
        hellochart.setZoomType(ZoomType.HORIZONTAL);
        hellochart.setOnValueTouchListener(new ValueTouchListener());

        // set up zoom so we can see 0 calories
        Viewport viewport = new Viewport(hellochart.getMaximumViewport());
        viewport.bottom = 0;
        hellochart.setMaximumViewport(viewport);
        hellochart.setCurrentViewport(viewport);

        hellochart.invalidate();
    }

    private class ValueTouchListener implements LineChartOnValueSelectListener {

        @Override
        public void onValueSelected(int lineIndex, int pointIndex, PointValue value) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat(MainActivity.dateFormat);
            cal.setTimeInMillis((long) value.getX()*1000L);
            Toast.makeText(getActivity(), "Timestamp: " + sdf.format(cal.getTime()) + " \nCalories: " + value.getY(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onValueDeselected() {
            // TODO Auto-generated method stub

        }

    }

    private static class TimestampAxisFormatter extends SimpleAxisValueFormatter {

        private Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("M/dd");

        @Override
        public int formatValueForAutoGeneratedAxis(char[] formattedValue, float value, int autoDecimalDigits) {
            cal.setTimeInMillis((long)value*1000L);
            char[] timestampA = sdf.format(cal.getTime()).toCharArray();
            System.arraycopy(timestampA, 0, formattedValue, formattedValue.length - timestampA.length, timestampA.length);
            return timestampA.length;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle)
    {
        String[] projection = { "_id", "value", "timestamp" };

        CursorLoader cursorLoader = new CursorLoader(getActivity(),
                Uri.parse(CalorieDataProvider.URL + "/summary/last30days"), projection, null, null, "timestamp");
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

    /*
     * Moves the query results into the adapter, causing the
     * ListView fronting this adapter to re-display
     */
        updateGraph(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    /*
     * Clears out the adapter's reference to the Cursor.
     * This prevents memory leaks.
     */
        //updateGraph(null);
    }
}

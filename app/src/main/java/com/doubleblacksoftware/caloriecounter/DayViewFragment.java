package com.doubleblacksoftware.caloriecounter;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by x on 5/28/15.
 */
public class DayViewFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    public static final String ARG_SECTION_NUMBER = "section_number";
    public static final String ARG_ID = "_id";
    private CalorieDataAdapter adapter;
    private int sectionNumber = 0;
    private Calendar fragmentDate;
    private FloatingActionButton fab;
    //private boolean loaderCompletedFirstTime = false;
    private ProgressBar progressBar;
    private TextView total;
    private TextView label;
    private static final int LOADER_LIST_ID = 0;
    private static final int LOADER_SUMMARY_ID = 1;

    public static DayViewFragment newInstance(int sectionNumber) {
        DayViewFragment f = new DayViewFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_SECTION_NUMBER, sectionNumber);
        f.setArguments(b);
        return f;
    }

    public DayViewFragment() {}

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        // super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sectionNumber == 0) {
            sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // set label to selected date.  Get date from Bundle.
        int dayOffset = sectionNumber - DayViewPager.pagerPageToday;
        fragmentDate = Calendar.getInstance();
        fragmentDate.add(Calendar.DATE, dayOffset);
        SimpleDateFormat sdf = new SimpleDateFormat(MainActivity.dateFormat);

        String labelText = sdf.format(fragmentDate.getTime());
        switch (dayOffset) {
            case 0:
                labelText += " (Today)";
                break;
            case 1:
                labelText += " (Tomorrow)";
                break;
            case -1:
                labelText += " (Yesterday)";
                break;
        }
        label.setText(labelText);
        getLoaderManager().initLoader(LOADER_LIST_ID, null, this);
        getLoaderManager().initLoader(LOADER_SUMMARY_ID, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.day_view_fragment, container, false);
        label = (TextView)rootView.findViewById(R.id.section_label);

        // set up list
        ListView listview = (ListView)rootView.findViewById(R.id.listView);
        adapter = new CalorieDataAdapter(getActivity(), null);
        listview.setAdapter(adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View view, int i, long l) {
                final String _id = l + "";
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Action")
                        .setItems(R.array.entry_actions, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // The 'which' argument contains the index position
                                // of the selected item
                                switch (which) {
                                    case 0: // edit
                                        Intent intent = new Intent(getActivity(), EditEntryActivity.class);
                                        intent.putExtra(ARG_ID, _id);
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            // this is kind of dumb, from the fragment you can't pass the animation.  So we have to
                                            // do it from the activity, but then we can't get the actvityResult here, so we'll
                                            // have to refresh the page onResume or something.
                                            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(getActivity(),
                                                    Pair.create((View)fab, "fab"),
                                                    Pair.create(view.findViewById(R.id.label), "label"),
                                                    Pair.create(view.findViewById(R.id.valueLabel), "value"));
                                            getActivity().startActivityForResult(intent, 0, options.toBundle());
                                        } else {
                                            startActivityForResult(intent, 0);
                                        }
                                        break;
                                    case 1: // delete
                                        int numDeleted = getActivity().getContentResolver().delete(
                                                CalorieDataProvider.CONTENT_URI, CalorieDataProvider.ID + "=?", new String[]{_id});
                                        Toast.makeText(getActivity(),
                                                numDeleted + " record deleted!", Toast.LENGTH_LONG).show();
                                        break;
                                    case 2: // duplicate
                                        String[] projection = { "value", "label" };
                                        Cursor recordToCopy = getActivity().getContentResolver().query(
                                                Uri.parse(CalorieDataProvider.URL + "/" + _id), projection, null, null, null);
                                        recordToCopy.moveToFirst();
                                        Calendar cal = Calendar.getInstance();
                                        Long timestamp = cal.getTimeInMillis()/1000;
                                        AddEntryActivity.storeEntry(
                                                getActivity(),
                                                recordToCopy.getString(recordToCopy.getColumnIndex("label")),
                                                recordToCopy.getString(recordToCopy.getColumnIndex("value")),
                                                timestamp,
                                                timestamp);
                                        recordToCopy.close();
                                        Toast.makeText(getActivity(), "Added this for today", Toast.LENGTH_LONG).show();
                                        break;
                                }
                            }
                        });
                builder.create().show();
            }
        });

        // set up fab
        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEntry();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            fab.setScaleX(0);
            fab.setScaleY(0);
            fab.animate()
                    .scaleX(1)
                    .scaleY(1)
                    .setDuration(500)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setStartDelay(500)
                    .start();
        }
        fab.attachToListView(listview);

        // set up progress bar
        progressBar = (ProgressBar)rootView.findViewById(R.id.progressBar);
        total = (TextView)rootView.findViewById(R.id.total_calories);
        return rootView;
    }

    public void addEntry() {
        //ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(getActivity());
        Intent intent = new Intent(getActivity(), AddEntryActivity.class);
        intent.putExtra(ARG_SECTION_NUMBER, sectionNumber);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // this is kind of dumb, from the fragment you can't pass the animation.  So we have to
            // do it from the activity, but then we can't get the actvityResult here, so we'll
            // have to refresh the page onResume or something.
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(getActivity(),
                    Pair.create((View)fab, "fab"));
            getActivity().startActivityForResult(intent, 0, options.toBundle());
            //startActivityForResult(intent, 0);
        } else {
            startActivityForResult(intent, 0);
        }
    }



    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        switch (loaderID) {
            case LOADER_LIST_ID:
                String[] projection = {"_id", "value", "timestamp", "label"};
                //Log.d("looking up date", sdf.format(fragmentDate.getTime()));
                return new CursorLoader(getActivity(),
                        Uri.parse(CalorieDataProvider.URL + "/" + sdf.format(fragmentDate.getTime())), projection, null, null, null);
            case LOADER_SUMMARY_ID:
                return new CursorLoader(
                        getActivity(),
                        Uri.parse(CalorieDataProvider.URL + "/summary/day/" + sdf.format(fragmentDate.getTime())),
                        new String[] {"total"},
                        null,
                        null,
                        null);

        }
        //Log.d("uri path", sdf.format(fragmentDate.getTime()));
        //return cursorLoader;
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

    /*
     * Moves the query results into the adapter, causing the
     * ListView fronting this adapter to re-display
     */
        switch (loader.getId()) {
            case LOADER_LIST_ID:
                adapter.swapCursor(cursor);
                break;
            case LOADER_SUMMARY_ID:
                cursor.moveToFirst();
                int totalCalories = cursor.getInt(cursor.getColumnIndex("total"));
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                int goalCalories = Integer.parseInt(prefs.getString("preference_calorie_goal", "2000"));
                boolean goalIsMinimum = prefs.getBoolean("preference_goal_is_minimum", false);
                //progressBar.setMax((totalCalories > 2600) ? totalCalories : 2600);
                //progressBar.setProgress(cursor.getInt(cursor.getColumnIndex("total")));
                boolean overGoal = (totalCalories > goalCalories);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (goalIsMinimum) {
                        int[][] states = new int[][]{
                                new int[]{android.R.attr.state_enabled}, // enabled
                                new int[]{-android.R.attr.state_enabled}, // disabled
                                new int[]{-android.R.attr.state_checked}, // unchecked
                                new int[]{android.R.attr.state_pressed}  // pressed
                        };

                        int[] colors = new int[]{
                                Color.RED,
                                Color.RED,
                                Color.GREEN,
                                Color.BLACK
                        };
                        int[] secondaryColors = new int[]{
                                Color.BLUE,
                                Color.RED,
                                Color.GREEN,
                                Color.BLACK
                        };
                        progressBar.setSecondaryProgressTintList(new ColorStateList(states, secondaryColors));
                        progressBar.setProgressTintList(new ColorStateList(states, colors));
                    }
                }

                ProgressBarAnimation anim = new ProgressBarAnimation(progressBar,
                        progressBar.getProgress(), overGoal ? goalCalories : totalCalories,
                        progressBar.getSecondaryProgress(), overGoal ? totalCalories : 0,
                        progressBar.getMax(), overGoal ? totalCalories : goalCalories);
                anim.setDuration(1000);
                progressBar.startAnimation(anim);
                total.setText(totalCalories + "");

                // update icon on widget
                if (sectionNumber - DayViewPager.pagerPageToday == 0) {
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getActivity());
                    int[] allWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(getActivity(), CalorieCounterWidgetProvider.class));
                    for (int widgetId : allWidgetIds) {
                        RemoteViews views = new RemoteViews(getActivity().getPackageName(),
                                R.layout.widget);
                        // update icon on widget
                        views.setTextViewText(R.id.textView, (goalCalories - totalCalories) + " left");
                        appWidgetManager.updateAppWidget(widgetId, views);
                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    /*
     * Clears out the adapter's reference to the Cursor.
     * This prevents memory leaks.
     */
        switch (loader.getId()) {
            case LOADER_LIST_ID:
                adapter.swapCursor(null);
                break;
        }
    }

    /*@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 0) { // this is adding a new entry, so we might need to refresh
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                //adapter.notifyDataSetChanged();
                getLoaderManager().restartLoader(0, null, this);
            }
        }
    }*/

    public class ProgressBarAnimation extends Animation {
        private ProgressBar progressBar;
        private float progressFrom;
        private float progressTo;
        private float progressSecondaryFrom;
        private float progressSecondaryTo;
        private float maxFrom;
        private float maxTo;

        public ProgressBarAnimation(ProgressBar progressBar,
                                    float progressFrom, float progressTo,
                                    float progressSecondaryFrom, float progressSecondaryTo,
                                    float maxFrom, float maxTo) {
            super();
            this.progressBar = progressBar;
            this.progressFrom = progressFrom;
            this.progressTo = progressTo;
            this.progressSecondaryFrom = progressSecondaryFrom;
            this.progressSecondaryTo = progressSecondaryTo;
            this.maxFrom = maxFrom;
            this.maxTo = maxTo;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);

            float progressValue = progressFrom + (progressTo - progressFrom) * interpolatedTime;
            progressBar.setProgress((int) progressValue);

            float progressSecondaryValue = progressSecondaryFrom + (progressSecondaryTo - progressSecondaryFrom) * interpolatedTime;
            progressBar.setSecondaryProgress((int) progressSecondaryValue);

            float maxValue = maxFrom + (maxTo - maxFrom) * interpolatedTime;
            progressBar.setMax((int) maxValue);
        }

    }
}

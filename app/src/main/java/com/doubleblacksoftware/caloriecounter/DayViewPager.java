package com.doubleblacksoftware.caloriecounter;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Toast;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by x on 5/30/15.
 */
public class DayViewPager extends Fragment {

    SectionsPagerAdapter mSectionsPagerAdapter;
    public static int pagerSize = 60;
    //public static int pagerPageToday = (int)(pagerSize/2);
    public static int pagerPageToday = 53; // why have 30 days blank ahead?
    ViewPager mViewPager;
    private String export_filename="calorie_counter.csv";
    public static String format = "M/dd/yy h:mm a";
    public static String format2 = "M/dd/yy";
    public static String format3 = "yyyy-M-dd h:mm a";
    public static String format4 = "yyyy-M-dd";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.day_view_pager, container, false);
    }
    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) view.findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mViewPager.setPageTransformer(true, new CustomTransformer());
        }
        mViewPager.setCurrentItem(pagerPageToday);
    }

    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            return DayViewFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return pagerSize;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Page " + position;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_export) {
            exportToSdCard();
            Toast.makeText(getActivity(),
                    "Exported to " + export_filename, Toast.LENGTH_LONG).show();
            return true;
        } else if (id == R.id.action_import) {
            importFromSdCard();
            return true;
        } else if (id == R.id.action_pick_date) {
            pickDate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void exportToSdCard() {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath());
        File file = new File(dir, export_filename);
        FileOutputStream f;
        try {
            file.createNewFile();
            f = new FileOutputStream(file);
            Cursor cursor = getActivity().getContentResolver().query(
                    CalorieDataProvider.CONTENT_URI, new String[] {
                            "value", "timestamp", "label"
                    }, null, null, "timestamp");
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                long time = cursor.getLong(cursor.getColumnIndex("timestamp")) * 1000L;
                cal.setTimeInMillis(time);
                String timestamp = sdf.format(cal.getTime());
                String value = cursor.getString(cursor.getColumnIndex("value"));
                String label = cursor.getString(cursor.getColumnIndex("label"));
                String line = timestamp + "," + label + "," + value + "\n";
                f.write(line.getBytes());
                cursor.moveToNext();
            }
            f.close();
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void importFromSdCard() {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath());
        File file = new File(dir, export_filename);
        if (file.exists()) {
            //FileInputStream f;
            try {
                CSVReader reader = new CSVReader(new FileReader(file));
                //f = new FileInputStream(file);
                final List<String[]> resultList = new ArrayList();
                //BufferedReader reader = new BufferedReader(new InputStreamReader(f));
                //String line;
                //while ((line = reader.readLine()) != null) {
                    //String[] row = line.split(",");
                    //String[] row = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                   // resultList.add(row);
                //}
                //f.close();
                String[] line;
                while ((line = reader.readNext()) != null) {
                    resultList.add(line);
                }
                new AlertDialog.Builder(getActivity())
                        .setTitle("Import records?")
                        .setMessage("Found " + resultList.size() + " records to import.  This will delete any records already in the app.")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Delete all records in app
                                getActivity().getContentResolver().delete(
                                        CalorieDataProvider.CONTENT_URI, null, null);
                                // Add all the new records
                                SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                                SimpleDateFormat dateFormat2 = new SimpleDateFormat(format2);
                                SimpleDateFormat dateFormat3 = new SimpleDateFormat(format3);
                                SimpleDateFormat dateFormat4 = new SimpleDateFormat(format4);
                                Long time = 0L;
                                for (String[] row : resultList) {
                                    try {
                                        time = dateFormat.parse(row[0]).getTime() / 1000;
                                    } catch (Exception e) {
                                        try {
                                            time = dateFormat2.parse(row[0]).getTime() / 1000;
                                        } catch (Exception e2) {
                                            try {
                                                time = dateFormat3.parse(row[0]).getTime() / 1000;
                                            } catch (Exception e3) {
                                                try {
                                                    time = dateFormat4.parse(row[0]).getTime() / 1000;
                                                } catch (Exception e4) {
                                                    Toast.makeText(getActivity(),
                                                            "Error: couldn't parse " + row[0], Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        }
                                    }
                                    AddEntryActivity.storeEntry(getActivity(), row[1], row[2], time, time);
                                }
                                Toast.makeText(getActivity(),
                                        "Import succeeded!", Toast.LENGTH_LONG).show();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Import Instructions")
                    .setMessage("Create a CSV file in the root of the SD card named " + export_filename + " containing a column with the date and time in format " + format + ", a column with the description, and a column with the calories.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing
                        }
                    }).show();
        }
    }

    void pickDate() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                Calendar calSelected = Calendar.getInstance();
                calSelected.set(year, month, day);
                mViewPager.setCurrentItem(Utils.dateToPagerIndex(calSelected.getTime(), pagerPageToday), true);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        cal.add(Calendar.DAY_OF_MONTH, pagerSize - pagerPageToday);
        Date maxDate = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -pagerSize);
        Date minDate = cal.getTime();
        datePickerDialog.getDatePicker().setMinDate(minDate.getTime());
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTime());
        datePickerDialog.show();
    }

    public class CustomTransformer implements ViewPager.PageTransformer {
        private final float[] speeds = { 1f, 1.2f, .9f, 1.1f, 1.3f };
        private final int speedsSize = speeds.length;

        public void transformPage(View view, float position) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                int pageWidth = view.getWidth();

                if (position <= 1) { // [-1,1]

                    //FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
                    //View sectionLabel = view.findViewById(R.id.section_header);
                    //View progressBar = view.findViewById(R.id.progressBar);
                    ListView listView = (ListView) view.findViewById(R.id.listView);
                    if (position < 0) {
                        //sectionLabel.setTranslationX((float) (-(Math.abs(position)) * 1.5 * pageWidth));
                        //progressBar.setTranslationX((float) (-(Math.abs(position)) * 1.5 * pageWidth));
                        //fab.setTranslationX((float) (-(Math.abs(position)) * 1.5 * pageWidth));
                        for (int i = 0; i < listView.getChildCount(); i++) {
                            listView.getChildAt(i).setTranslationX(-(Math.abs(position)) * speeds[i%speedsSize]  * pageWidth);
                        }
                    } else {
                        //sectionLabel.setTranslationX((float) (-(Math.abs(position)) * 0.5 * pageWidth));
                        //progressBar.setTranslationX((float) (-(Math.abs(position)) * 0.5 * pageWidth));
                        //fab.setTranslationX((float) (-(Math.abs(position)) * 0.5 * pageWidth));
                        for (int i = 0; i < listView.getChildCount(); i++) {
                            listView.getChildAt(i).setTranslationX((Math.abs(position)) * speeds[i%speedsSize]  * pageWidth);
                        }
                    }
                }
            }
        }
    }

    /*public class DepthPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        public void transformPage(View view, float position) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                int pageWidth = view.getWidth();

                if (position < -1) { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    view.setAlpha(0);

                } else if (position <= 0) { // [-1,0]
                    // Use the default slide transition when moving to the left page
                    view.setAlpha(1);
                    view.setTranslationX(0);
                    view.setScaleX(1);
                    view.setScaleY(1);

                } else if (position <= 1) { // (0,1]
                    // Fade the page out.
                    view.setAlpha(1 - position);

                    // Counteract the default slide transition
                    view.setTranslationX(pageWidth * -position);

                    // Scale the page down (between MIN_SCALE and 1)
                    float scaleFactor = MIN_SCALE
                            + (1 - MIN_SCALE) * (1 - Math.abs(position));
                    view.setScaleX(scaleFactor);
                    view.setScaleY(scaleFactor);

                } else { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    view.setAlpha(0);
                }
            }
        }
    }*/

    /*public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.9f;

        public void transformPage(View view, float position) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                int pageWidth = view.getWidth();
                int pageHeight = view.getHeight();

                if (position < -1) { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    view.setAlpha(0);

                } else if (position <= 1) { // [-1,1]
                    // Modify the default slide transition to shrink the page as well
                    float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                    float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                    float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                    if (position < 0) {
                        view.setTranslationX(horzMargin - vertMargin / 2);
                    } else {
                        view.setTranslationX(-horzMargin + vertMargin / 2);
                    }

                    // Scale the page down (between MIN_SCALE and 1)
                    view.setScaleX(scaleFactor);
                    view.setScaleY(scaleFactor);

                    // Fade the page relative to its size.
                    view.setAlpha(MIN_ALPHA +
                            (scaleFactor - MIN_SCALE) /
                                    (1 - MIN_SCALE) * (1 - MIN_ALPHA));

                } else { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    view.setAlpha(0);
                }
            }
        }
    }*/
}

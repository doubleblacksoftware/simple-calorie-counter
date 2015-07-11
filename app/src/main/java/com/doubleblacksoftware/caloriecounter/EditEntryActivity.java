package com.doubleblacksoftware.caloriecounter;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Explode;
import android.transition.Transition;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.calculator2.CalculatorGB;
import com.android.calculator2.CalculatorL;

import java.text.SimpleDateFormat;

/**
 * Created by x on 5/28/15.
 */
public class EditEntryActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    private int sectionNumber = 0;
    CalorieDataAdapter adapter;
    private static int calcResultCode = 0;
    private View fab;
    private String _id;
    private static final int LOADER_AUTOCOMPLETE = 0;
    private static final int LOADER_ROW = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // set up animation for this activity. This needs to happen before super.onCreate.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            Transition enterTrans = new Explode();
            getWindow().setEnterTransition(enterTrans);

            Transition returnTrans = new Explode();
            getWindow().setReturnTransition(returnTrans);
        }

        // set up activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_entry);

        // get which item we're supposed to be editing
        Intent intent = getIntent();
        //sectionNumber = intent.getIntExtra(DayViewFragment.ARG_SECTION_NUMBER, 0);
        _id = intent.getStringExtra(DayViewFragment.ARG_ID);

        if (_id == null) {
            finish();
        }

        // number box for calories
        final EditText calories = (EditText)findViewById(R.id.calories);

        // get the fab handle because we need it in the description box below
        fab = findViewById(R.id.fab);

        // set up description box
        final AutoCompleteTextView description = (AutoCompleteTextView)findViewById(R.id.description);
        adapter = new CalorieDataAdapter(getApplicationContext(), null); // we'll set the array when the loader finishes
        adapter.setViewId(R.layout.auto_complete_row); // this needs to be called before any data is added (next line)
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                String s = '%' + constraint.toString() + '%';
                return getContentResolver().query(Uri.parse(CalorieDataProvider.URL + "/unique/"),
                        new String[]{"value || label as _id", "value", "label", "max(timestamp) as maxtimestamp"},
                        "label LIKE ?",
                        new String[]{s},
                        "maxtimestamp desc");
            }
        });

        // set up action buttons
        // the description box should always have data in it, so lets assume we can always save when we start
        //fab.setEnabled(false);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateEntryAndExit(description.getText().toString(), calories.getText().toString());
            }
        });
        View fabcancel = findViewById(R.id.fabcancel);
        fabcancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAfterClosingKeyboard();
            }
        });

        // set up calc button
        ImageButton calc = (ImageButton)findViewById(R.id.calc);
        calc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startActivityForResult(new Intent(getApplicationContext(), CalculatorL.class), calcResultCode);
                } else {
                    startActivityForResult(new Intent(getApplicationContext(), CalculatorGB.class), calcResultCode);
                }

            }
        });

        // get the data for this page
        getSupportLoaderManager().initLoader(LOADER_ROW, null, this);
        getSupportLoaderManager().initLoader(LOADER_AUTOCOMPLETE, null, this);

        description.clearFocus();
        calories.requestFocus();

        // show keyboard after the animations are done
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                //InputMethodManager keyboard = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                //keyboard.showSoftInput(description, 0);
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                InputMethodManager keyboard = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(calories, 0);

                description.setThreshold(1);
                description.setAdapter(adapter);
                description.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        calories.setText(((TextView) view.findViewById(R.id.valueLabel)).getText().toString());
                    }
                });
                description.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        fab.setEnabled(description.getText().toString().length() > 0);
                    }
                });
            }
        }, 700);
    }

    public void updateEntry(Context context, String description, String value) {
        ContentValues values = new ContentValues();
        values.put(CalorieDataProvider.VALUE, value);
        values.put(CalorieDataProvider.LABEL, description);
        context.getContentResolver().update(
                Uri.parse(CalorieDataProvider.URL + "/" + _id),
                values,
                null,
                null
        );
    }

    private void updateEntryAndExit(String description, String value) {
        updateEntry(this, description, value);
        setResult(Activity.RESULT_OK);
        finishAfterClosingKeyboard();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == calcResultCode) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // put the result of the calc into the calorie box
                Bundle bundle = data.getExtras();
                String result = data.getStringExtra("result");
                ((EditText)findViewById(R.id.calories)).setText(result);
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle)
    {
        switch (loaderID) {
            case LOADER_AUTOCOMPLETE:
                String[] autoCompleteProjection = {"value || label as _id", "value", "label", "max(timestamp) as maxtimestamp"};

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
                return new CursorLoader(getApplicationContext(),
                        Uri.parse(CalorieDataProvider.URL + "/unique/"), autoCompleteProjection, null, null, "maxtimestamp desc");
            case LOADER_ROW:
                String[] rowProjection = {"value", "label"};
                return new CursorLoader(getApplicationContext(),
                        Uri.parse(CalorieDataProvider.URL + "/" + _id), rowProjection, null, null, null);
            default:
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

    /*
     * Moves the query results into the adapter, causing the
     * ListView fronting this adapter to re-display
     */
        switch (loader.getId()) {
            case LOADER_AUTOCOMPLETE:
                adapter.swapCursor(cursor);
                break;
            case LOADER_ROW:
                cursor.moveToFirst();
                AutoCompleteTextView description = (AutoCompleteTextView)findViewById(R.id.description);
                description.setText(cursor.getString(cursor.getColumnIndex("label")));
                EditText calories = (EditText)findViewById(R.id.calories);
                calories.setText(cursor.getString(cursor.getColumnIndex("value")));
                calories.setSelection(calories.getText().toString().length());
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
            case LOADER_AUTOCOMPLETE:
                adapter.swapCursor(null);
                break;
        }
    }

    private void finishAfterClosingKeyboard() {
        // don't show the drop down anymore
        AutoCompleteTextView description = (AutoCompleteTextView)findViewById(R.id.description);
        description.setAdapter(null);

        // hide keyboard then start animations
        InputMethodManager keyboard = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(fab.getWindowToken(), 0);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                supportFinishAfterTransition();

            }
        }, 300);
    }
}

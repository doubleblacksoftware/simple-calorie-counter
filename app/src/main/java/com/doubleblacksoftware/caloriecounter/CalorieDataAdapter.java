package com.doubleblacksoftware.caloriecounter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by x on 5/28/15.
 */
public class CalorieDataAdapter extends CursorAdapter {
    private final LayoutInflater mInflater;
    private int viewId = R.layout.row_item;

    public CalorieDataAdapter(Context context, Cursor cursor) {
        super(context, cursor, false);
        mInflater = LayoutInflater.from(context);
    }

    public void setViewId(int viewId) {
        this.viewId = viewId;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(viewId, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // get the fields from the row
        TextView label = (TextView) view.findViewById(R.id.label);
        TextView valueLabel = (TextView) view.findViewById(R.id.valueLabel);

        // set value label
        final String value = cursor.getString(cursor.getColumnIndex("value"));
        valueLabel.setText(value);

        // set the description label
        label.setText(cursor.getString(cursor.getColumnIndex("label")));

        // set up the buttons
        int IDColumnIndex = cursor.getColumnIndex("_id");
        if (IDColumnIndex != -1) { // -1 means column not found
            final String _id = cursor.getString(IDColumnIndex);
            view.setTag(_id);
        }
    }

    @Override
    public String convertToString(Cursor cursor) {
        // this method dictates what is shown when the user clicks each entry in your autocomplete list
        return cursor.getString(cursor.getColumnIndex("label"));
    }
}
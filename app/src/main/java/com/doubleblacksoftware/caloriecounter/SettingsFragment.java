package com.doubleblacksoftware.caloriecounter;

import android.os.Bundle;
import android.support.v4.preference.PreferenceFragment;

/**
 * Created by x on 5/30/15.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}

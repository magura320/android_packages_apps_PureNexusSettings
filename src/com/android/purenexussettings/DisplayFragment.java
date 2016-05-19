/*
 * Copyright (C) 2016 The Pure Nexus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.purenexussettings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public class DisplayFragment extends PreferenceFragment {

    public DisplayFragment(){}

    private static final String KEY_NOTIFICATION_LIGHT = "notification_light";
    private static final String KEY_BATTERY_LIGHT = "battery_light";

    private static final String CATEGORY_LEDS = "leds";

    private Preference mNotifLedFrag;
    private Preference mBattLedFrag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.display_fragment);

        final PreferenceScreen prefScreen = getPreferenceScreen();

        final PreferenceCategory leds = (PreferenceCategory)
                findPreference(CATEGORY_LEDS);

        mNotifLedFrag = findPreference(KEY_NOTIFICATION_LIGHT);
        //remove notification led settings if device doesnt support it
        if (!getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            leds.removePreference(findPreference(KEY_NOTIFICATION_LIGHT));
        }

        mBattLedFrag = findPreference(KEY_BATTERY_LIGHT);
        //remove battery led settings if device doesnt support it
        if (!getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveBatteryLed)) {
            leds.removePreference(findPreference(KEY_BATTERY_LIGHT));
        }

        //remove led category if device doesnt support notification or battery
        if (!getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveNotificationLed)
                && !getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveBatteryLed)) {
            prefScreen.removePreference(findPreference(CATEGORY_LEDS));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mNotifLedFrag) {
            ((TinkerActivity)getActivity()).displaySubFrag(getString(R.string.notification_light_frag_title));

            return true;
        }
        if (preference == mBattLedFrag) {
            ((TinkerActivity)getActivity()).displaySubFrag(getString(R.string.battery_light_frag_title));

            return true;
        }
        return false;
    }
}

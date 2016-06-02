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
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public class StatusBarFragment extends PreferenceFragment {

    public StatusBarFragment(){}

    private static final String CLOCKDATEFRAG = "clockdatefrag";
    private static final String NETWORKTRAFFRAG = "nettraffrag";

    private Preference mClockDate;
    private Preference mNetTraf;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.statusbar_fragment);

        mClockDate = findPreference(CLOCKDATEFRAG);
        mNetTraf = findPreference(NETWORKTRAFFRAG);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mClockDate) {
            ((TinkerActivity)getActivity()).displaySubFrag(getString(R.string.clockdate_frag_title));

            return true;
        }
        if (preference == mNetTraf) {
            ((TinkerActivity)getActivity()).displaySubFrag(getString(R.string.nettraffic_frag_title));

            return true;
        }
        return false;
    }
}

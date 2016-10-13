/*
 * Copyright (C) 2015 The Pure Nexus Project
 * Original credit for changelog aspects seen below goes to MartinvanZ and Inscription library
 * xml structure and a good chunk of xml parsing are all that remain of original
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BulletSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;


public class AboutFragment extends Fragment {

    private AlertDialog popUpInfo;
    private int clickCount;
    private SharedPreferences prefs;
    final private static String DIAGTYPE = "diagType";
    final private static int DIAG_CHANGE = 0;
    final private static int DIAG_THANKS = 1;

    public AboutFragment() {
    }

    public static class MyDialogFragment extends DialogFragment
    {
        private static final String RELEASETAG = "release";
        private static final String CHANGETAG = "change";
        private static final String VERSIONTAG = "version";
        private static final String VERSIONNUM = "versioncode";

        private static final int RELEASE_NUM = 0;
        private static final int RELEASE_NAME = 1;
        private static final int RELEASE_CHANGES = 2;

        public MyDialogFragment() {}

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            final int diagType = getArguments().getInt(DIAGTYPE);
            Resources resources = getActivity().getResources();
            View view;
            String titleText;
            boolean itWorked = true;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            switch (diagType) {
                case DIAG_CHANGE:
                    view = getActivity().getLayoutInflater().inflate(R.layout.changelog_layout, null);

                    LinearLayout mainLayout = (LinearLayout) view.findViewById(R.id.scrollLayout);
                    itWorked = getChangelogEntries(mainLayout, R.xml.changelog, resources);
                    titleText = resources.getString(R.string.changetitle);
                    break;
                case DIAG_THANKS:
                    view = getActivity().getLayoutInflater().inflate(R.layout.credits, null);
                    TextView mChangeText = (TextView)view.findViewById(R.id.credittext);
                    String[] credits = resources.getString(R.string.credits).split(",", -1);
                    SpannableStringBuilder creditText = new SpannableStringBuilder();

                    if (credits.length <= 0) {
                        creditText.append("ERROR LOADING STRING");
                    } else {
                        creditText.append(credits[0]);
                        creditText.append("\n\n");

                        for (int i = 1; i < credits.length; i++) {
                            Spannable creditEntry = new SpannableString(credits[i] + "\n\n");
                            creditEntry.setSpan(new BulletSpan(40), 0, creditEntry.length(), 0);
                            creditText.append(creditEntry);
                        }

                        // clip last /n off...
                        creditText.delete(creditText.length() - 1, creditText.length());
                    }

                    mChangeText.setText(creditText);
                    titleText = getString(R.string.setnegative);
                    break;
                default:
                    view = null;
                    titleText = null;
            }

            if (view != null && itWorked) {
                builder.setTitle(titleText);
                builder.setView(view);
                builder.setPositiveButton(getString(R.string.setpositive), null);

                if (diagType == DIAG_CHANGE) {
                    builder.setNegativeButton(getString(R.string.setnegative), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MyDialogFragment myDiag = new MyDialogFragment();
                            Bundle diagType = new Bundle();
                            diagType.putInt(DIAGTYPE, DIAG_THANKS);
                            myDiag.setArguments(diagType);
                            myDiag.show(getFragmentManager(), "Credits");
                        }
                    });
                }
                return builder.create();
            } else {
                return null;
            }
        }

        private CharSequence[] ParseReleaseTag(XmlResourceParser xmlResourceParser) throws XmlPullParserException, IOException {
            SpannableStringBuilder mVersionName =
                    new SpannableStringBuilder("Release: " + xmlResourceParser.getAttributeValue(null, VERSIONTAG));
            SpannableStringBuilder mVersionNum =
                    new SpannableStringBuilder(xmlResourceParser.getAttributeValue(null, VERSIONNUM));

            SpannableStringBuilder bulletText = new SpannableStringBuilder();

            int eventType = xmlResourceParser.getEventType();
            while ((eventType != XmlPullParser.END_TAG) || (xmlResourceParser.getName().equals(CHANGETAG))) {
                if ((eventType == XmlPullParser.START_TAG) && (xmlResourceParser.getName().equals(CHANGETAG))){
                    xmlResourceParser.next();
                    Spannable changeItem = new SpannableString(xmlResourceParser.getText() + "\n");
                    changeItem.setSpan(new BulletSpan(40), 0, changeItem.length(), 0);
                    bulletText.append(changeItem);
                }
                eventType = xmlResourceParser.next();
            }
            // clip last /n off...
            bulletText.delete(bulletText.length() - 1, bulletText.length());

            SpannableStringBuilder[] results = new SpannableStringBuilder[3];
            results[RELEASE_NUM] = mVersionNum;
            results[RELEASE_NAME] = mVersionName;
            results[RELEASE_CHANGES] = bulletText;

            return results;
        }

        private boolean getChangelogEntries(LinearLayout scrollLayout, int resourceID, Resources resources) {
            boolean somethingLoaded = false;

            XmlResourceParser xmlResourceParser = resources.getXml(resourceID);
            try {
                int eventType = xmlResourceParser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if ((eventType == XmlPullParser.START_TAG) && (xmlResourceParser.getName().equals(RELEASETAG))){
                        // get parsed string array
                        CharSequence[] infoArray = ParseReleaseTag(xmlResourceParser);
                        // get inflater and inflate entry layout xml
                        LayoutInflater layoutInflater =
                                (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View view = layoutInflater.inflate(R.layout.changelog_entry, scrollLayout, false);
                        // grab textviews and setText based on string array
                        TextView textViewMain = (TextView) view.findViewById(R.id.release_entry);
                        textViewMain.setText(infoArray[RELEASE_NAME]);
                        TextView textViewItems = (TextView) view.findViewById(R.id.release_changes);
                        textViewItems.setText(infoArray[RELEASE_CHANGES]);
                        // add view to linearlayout
                        scrollLayout.addView(view);
                        // set itworked flag
                        somethingLoaded = true;
                    }
                    eventType = xmlResourceParser.next();
                }
            } catch (org.xmlpull.v1.XmlPullParserException | java.io.IOException e) {
                somethingLoaded = false;
            }

            xmlResourceParser.close();

            return somethingLoaded;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getActivity().getSharedPreferences(getActivity().getPackageName(), Context.MODE_PRIVATE);

        this.popUpInfo = null;
        clickCount = 0;
    }

    private void noBrowserSnack(View v) {
        int bgColor = TinkerActivity.isLight(prefs)
                ? getActivity().getResources().getColor(R.color.snackbar_bg_light, null)
                : getActivity().getResources().getColor(R.color.snackbar_bg, null);
        TinkerActivity.showSnack(
                v,
                getString(R.string.no_browser_error),
                bgColor,
                true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.about_frag_card, container, false);

        final LinearLayout logo = (LinearLayout)v.findViewById(R.id.logo_card);
        LinearLayout thanks = (LinearLayout)v.findViewById(R.id.credits_card);
        final int bgColor = TinkerActivity.isLight(prefs)
                ? getActivity().getResources().getColor(R.color.snackbar_bg_light, null)
                : getActivity().getResources().getColor(R.color.snackbar_bg, null);

        //pushbullet
        LinearLayout link1 = (LinearLayout)v.findViewById(R.id.link1_card);
        //gplus
        LinearLayout link2 = (LinearLayout)v.findViewById(R.id.link2_card);
        //twitter
        LinearLayout link3 = (LinearLayout)v.findViewById(R.id.link3_card);
        //donate
        LinearLayout link4 = (LinearLayout)v.findViewById(R.id.link4_card);

        thanks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popUpInfo == null || !popUpInfo.isShowing()) {
                    MyDialogFragment myDiag = new MyDialogFragment();
                    Bundle diagType = new Bundle();
                    diagType.putInt(DIAGTYPE, DIAG_CHANGE);
                    myDiag.setArguments(diagType);
                    myDiag.show(getFragmentManager(), "Changelog");
                    logo.setClickable(true);
                }
            }
        });

        link1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent link = new Intent(Intent.ACTION_VIEW);
                Uri url = Uri.parse(getString(R.string.pushbullet_data));
                link.setData(url);
                if (TinkerActivity.checkIntent(getContext(), link)) {
                    startActivity(link);
                } else {
                    noBrowserSnack(v);
                }
            }
        });

        link2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent link = new Intent(Intent.ACTION_VIEW);
                Uri url = Uri.parse(getString(R.string.gplus_data));
                link.setData(url);
                if (TinkerActivity.checkIntent(getContext(), link)) {
                    startActivity(link);
                } else {
                    noBrowserSnack(v);
                }
            }
        });

        link3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent link = new Intent(Intent.ACTION_VIEW);
                Uri url = Uri.parse(getString(R.string.twit_data));
                link.setData(url);
                if (TinkerActivity.checkIntent(getContext(), link)) {
                    startActivity(link);
                } else {
                    noBrowserSnack(v);
                }
            }
        });

        link4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent link = new Intent(Intent.ACTION_VIEW);
                Uri url = Uri.parse(getString(R.string.payp_data));
                link.setData(url);
                if (TinkerActivity.checkIntent(getContext(), link)) {
                    startActivity(link);
                } else {
                    noBrowserSnack(v);
                }
            }
        });

        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickCount++;
                if (clickCount == 5) {
                    TinkerActivity.showSnack(
                            v,
                            getString(R.string.click1),
                            bgColor,
                            false);
                } else if (clickCount > 5 && clickCount < 10) {
                    TinkerActivity.showSnack(
                            v,
                            String.format(getString(R.string.click2), clickCount),
                            bgColor,
                            false);
                } else if (clickCount == 10) {
                    TinkerActivity.showSnack(
                            v,
                            String.format(getString(R.string.click3), clickCount),
                            bgColor,
                            false);

                    clickCount = 0;
                }
            }
        });

        logo.setClickable(false);
        return v;
    }

}
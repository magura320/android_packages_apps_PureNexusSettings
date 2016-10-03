/*
 * Copyright (C) 2015 The Pure Nexus Project
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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


public class AboutFragment extends Fragment {

    private AlertDialog popUpInfo;
    private int clickCount;
    final private static String DIAGTYPE = "diagType";
    final private static int DIAG_CHANGE = 0;
    final private static int DIAG_THANKS = 1;

    public AboutFragment() {
    }

    public static class MyDialogFragment extends DialogFragment
    {
        public MyDialogFragment() {}

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            final int diagType = getArguments().getInt(DIAGTYPE);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            View view = getActivity().getLayoutInflater().inflate(R.layout.changelog, null);
            TextView mChangeText = (TextView)view.findViewById(R.id.changetext);

            Spanned contentText;
            String titleText;
            switch (diagType) {
                case DIAG_THANKS:
                    contentText = Html.fromHtml(getString(R.string.credits), 0);
                    titleText = getString(R.string.setnegative);
                    break;
                case DIAG_CHANGE:
                default:
                    contentText = Html.fromHtml(getString(R.string.changelog), 0);
                    titleText = getString(R.string.alertdiagtitle);
            }

            mChangeText.setText(contentText);
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

            return  builder.create();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.popUpInfo = null;
        clickCount = 0;
    }

    private void noBrowserSnack(View v) {
        Snackbar.make(v, getString(R.string.no_browser_error), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.about_frag_card, container, false);

        final LinearLayout logo = (LinearLayout)v.findViewById(R.id.logo_card);
        LinearLayout thanks = (LinearLayout)v.findViewById(R.id.credits_card);

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
                    Snackbar.make(v, getString(R.string.click1), Snackbar.LENGTH_SHORT).show();
                }
                if (clickCount > 5 && clickCount < 10) {
                    Snackbar.make(v, String.format(getString(R.string.click2), clickCount), Snackbar.LENGTH_SHORT).show();
                }
                if (clickCount == 10) {
                    Snackbar.make(v, String.format(getString(R.string.click3), clickCount), Snackbar.LENGTH_LONG).show();
                    clickCount = 0;
                }
            }
        });

        logo.setClickable(false);
        return v;
    }
}
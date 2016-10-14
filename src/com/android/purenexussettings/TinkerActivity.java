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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Surface;
import android.view.View;
import android.widget.PopupMenu;

import java.util.Arrays;
import java.util.Stack;

import com.android.purenexussettings.utils.ThemeSwitch;

public class TinkerActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavView;
    private ActionBarDrawerToggle mDrawerToggle;
    public static String mPackageName;
    private FragmentManager fragmentManager;

    // this might be handy later - something that can tell other things if root
    public static boolean isRoot;

    // this allows first # entries in stringarray to be skipped from navdrawer
    public static int FRAG_ARRAY_START;

    // stuff for widget calls to open fragments
    public static final String EXTRA_START_FRAGMENT = "com.android.purenexussettings.tinkerings.EXTRA_START_FRAGMENT";

    public static final String PROJFI_PACKAGE_NAME = "com.google.android.apps.tycho";

    // example - used to retain slidetab position
    public static int LAST_SLIDE_BAR_TAB;

    // nav drawer title
    private CharSequence mDrawerTitle;

    // used to store app title/position
    private CharSequence mTitle;
    private int mItemPosition;

    // for backstack tracking
    private Stack<String> fragmentStack;

    // various bools for this or that
    private boolean mBackPress;
    private boolean mIgnoreBack;
    private boolean mFromClick;
    private boolean mIgnore;
    private boolean mMenu;
    private boolean fullyClosed;
    private boolean openingHalf;

    // slide menu items
    private String[] navMenuTitles;
    private String[] navMenuFrags;

    // info for buildprop editor
    public static String mEditName;
    public static String mEditKey;

    // for theme checking
    private SharedPreferences prefs;
    public final static String THEME_TOGGLE = "appthemetog";

    // For handling quick back/About presses
    private Handler myHandler = new Handler();

    public static class ThemeDialogFragment extends DialogFragment
    {
        public ThemeDialogFragment() {}

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final SharedPreferences preferences = getActivity().getSharedPreferences(getActivity().getPackageName(), Context.MODE_PRIVATE);
            // grab current theme setting
            final int current = preferences.getInt(THEME_TOGGLE, ThemeSwitch.DARK);
            // get passed position value
            final int position = getArguments().getInt(EXTRA_START_FRAGMENT);

            builder.setTitle(getActivity().getResources().getString(R.string.action_theme));
            builder.setSingleChoiceItems(R.array.app_theme_entries, current, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    // only change theme if selection changes
                    if (current != item) {
                        preferences.edit().putInt(THEME_TOGGLE, item).apply();
                        ThemeSwitch.changeTheme(getActivity(), position);
                    }
                    dialog.dismiss();// dismiss the alertbox after chose option
                }
            });

            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            // need to do anything here...?
            super.onDismiss(dialog);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set theme based on toggle
        prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        ThemeSwitch.setTheme(this, prefs.getInt(THEME_TOGGLE, ThemeSwitch.DARK));

        setContentView(R.layout.activity_tinker);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set up some defaults
        FRAG_ARRAY_START = getResources().getIntArray(R.array.nav_drawer_cat_nums)[0];
        mTitle = mDrawerTitle = getTitle();
        mPackageName = getPackageName();
        LAST_SLIDE_BAR_TAB = 0;
        mBackPress = false;
        mIgnoreBack = false;
        mFromClick = false;
        mMenu = false;
        fullyClosed = true;
        openingHalf = true;
        final boolean mIsLight = isLight(prefs);

        // for backstack tracking
        fragmentStack = new Stack<>();

        // load slide menu items - titles and frag names
        navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);
        navMenuFrags = getResources().getStringArray(R.array.nav_drawer_fragments);

        // nav drawer icons from resources
        TypedArray navMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mNavView = (NavigationView) findViewById(R.id.slidermenu);

        // create navigationview items
        Menu menu;
        if (mNavView != null) {
            menu = mNavView.getMenu();
            menu.clear();
        } else {
            PopupMenu popupMenu = new PopupMenu(this, null);
            menu = popupMenu.getMenu();
            menu.clear();
        }

        // pull in category names and numbers in each
        String[] navMenuCats = getResources().getStringArray(R.array.nav_drawer_cats);
        int[] navMenuCatCounts = getResources().getIntArray(R.array.nav_drawer_cat_nums);

        // set up some counters
        int j=0;
        int total=0;
        SubMenu submenu=null;
        int fontColor = mIsLight
                ? getResources().getColor(R.color.cardview_pref_title_light, null)
                : getResources().getColor(R.color.cardview_pref_title, null);
        // go through the total possible menu list
        for (int i=0; i < navMenuTitles.length; i++) {
            // when the count equals a threshold value, increment/sum and add submenu
            if (i == (total + navMenuCatCounts[j])) {
                total += navMenuCatCounts[j];
                // format submenu headings
                SpannableString strcat= new SpannableString(navMenuCats[j]);
                strcat.setSpan(new ForegroundColorSpan(fontColor), 0, strcat.length(),0);
                strcat.setSpan(new RelativeSizeSpan(0.85f), 0, strcat.length(), 0);
                strcat.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, strcat.length(), 0);
                // is the 10 * (j + 1) bit needed...? Maybe not... meh
                submenu = menu.addSubMenu((j + 1), 10 * (j + 1), 10 * (j + 1), strcat);
                j++;
            }
            // assuming all are skipped before first submenu, only add menu items if total <> 0
            if (total > 0) {
                // format menu item title
                SpannableString stritem= new SpannableString(navMenuTitles[i]);
                stritem.setSpan(new ForegroundColorSpan(fontColor), 0, stritem.length(),0);
                // group id is j, i is item id and order..., then title - includes logic for conditional entries
                // an attempt to add icon if included...
                if (navMenuIcons.getResourceId(i, -1) != -1) {
                    submenu.add(j, i, i, stritem).setIcon(navMenuIcons.getResourceId(i, -1));
                } else {
                    submenu.add(j, i, i, stritem);
                }
             }
        }

        // remove icon tint from NavView
        //mNavView.setItemIconTintList(null);

        mNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // check for external app launching navdrawer items
                // ...nothing here currently...

                // if nothing was caught in the above, do the usual prep to show frag stuff
                if (!mIgnore) {
                    if (mItemPosition != item.getItemId()) {
                        mItemPosition = item.getItemId();
                        mFromClick = true;
                        setTitle(navMenuTitles[mItemPosition]);
                        removeCurrent();
                    } else {
                        mIgnore = true;
                    }
                    mDrawerLayout.closeDrawer(mNavView);
                }

                return true;
            }
        });

        // Recycle the typed array
        navMenuIcons.recycle();

        // enabling action bar app icon and behaving it as toggle button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                toolbar, //nav menu toggle icon
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        ){
            @Override
            public void onDrawerClosed(View view) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mNavView);
                getSupportActionBar().setTitle(mTitle);
                // calling onPrepareOptionsMenu() to show action bar icons
                openingHalf = true;
                invalidateOptionsMenu();
                // now that the drawer animation is done - load fragment
                if (mIgnore || !mFromClick ) {
                    mIgnore = false;
                } else {
                    displayView(mItemPosition);
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                // calling onPrepareOptionsMenu() to hide action bar icons
                openingHalf = false;
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                fullyClosed = (slideOffset == 0.0f);
                if (slideOffset < 0.5f && !openingHalf) {
                    openingHalf = true;
                    invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
                    // if light theme use normal light icons (black) when opening drawer...
                    if (mIsLight) {
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    }
                } else if (slideOffset > 0.5f && openingHalf) {
                    openingHalf = false;
                    invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
                    // if light theme flip em back to dark icons (so white) when drawer is mostly open
                    if (mIsLight) {
                        getWindow().getDecorView().setSystemUiVisibility(
                                getWindow().getDecorView().getSystemUiVisibility() - View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    }

                }
            }

        };

        mDrawerLayout.addDrawerListener(mDrawerToggle);

        fragmentManager = getFragmentManager();

        // TODO - rework this for theme reset stuff?
        if (savedInstanceState == null) {
            // Use About as default start point
            displayView(mItemPosition = getIntent().getIntExtra(EXTRA_START_FRAGMENT, 1));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private boolean checkPosition(int position) {
        // identify if position should skip stack clearing
        // these are the non-About frags not shown in navdrawer
        return position < FRAG_ARRAY_START;
    }

    private int checkSubFrag(int origposition, int newposition) {
        // see if current frag is further subfrag to force origfrag on backpress
        // not used for much currently...
        switch(origposition) {
            default:
                return newposition;
        }
    }

    public static boolean isLight(SharedPreferences prefs) {
        return prefs.getInt(THEME_TOGGLE, ThemeSwitch.DARK) == ThemeSwitch.LIGHT;
    }

    private void resetEmptyStack(int position) {
        // force about back in
        fragmentStack.push("AboutFragment");
        switch (position) {
            case 0: // editprop
                fragmentStack.push("BuildPropFragment");
                break;
        }
    }

    /* Displaying fragment view for selected nav drawer list item */
    private void displayView(int position) {
        // before anything else - check to see if position matches intent-launching "frags" - for example
        // if ( navMenuTitles[position].equals("TARGETNAME") ) { position = 0; do something}
        boolean mKeepStack = checkPosition(position);

        // update the main content by replacing fragments
        Fragment frags;
        String fragname = navMenuFrags[position];
        try {
            frags = (Fragment)Class.forName(mPackageName + "." + fragname).newInstance();
            }
        catch (Exception e) {
            frags = null;
            }
        if (frags != null) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
            }

            try {
                FragmentTransaction fragtrans = fragmentManager.beginTransaction();
                if (mFromClick || mMenu || mBackPress) {
                    fragtrans.setCustomAnimations(R.animator.fadein, R.animator.fadeout, R.animator.fadein, R.animator.fadeout);
                }
                fragtrans.add(R.id.frame_container, frags);
                // The backstack should be cleared if not coming from a fragment flagged as stack keeping or from a backpress
                // After clearing the only entry should be About/main
                if (!mKeepStack && !mBackPress) {
                    fragmentStack.clear();
                    fragmentStack.push("AboutFragment");
                }
                // add fragment name to custom stack for backstack tracking
                // only do it if not a backpress, flagged as stack keeping, or dup of last entry
                if (!mBackPress && !mKeepStack && !(fragmentStack.size() >= 1 && fragmentStack.peek().equals(navMenuFrags[position]))) {
                    fragmentStack.push(navMenuFrags[position]);
                }

                fragtrans.commit();
            } catch (Exception e) { }

            // update selected item and title, then close the drawer
            if (mFromClick || mBackPress) {
                mFromClick = false;
                mBackPress = false;
            } else {
                setTitle(navMenuTitles[position]);
                if (mMenu) {
                    mMenu = false;
                    mItemPosition = position;
                } else {
                    mDrawerLayout.closeDrawer(mNavView);
                }
            }
            invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
        } else {
            // error in creating fragment
            Log.e("TinkerActivity", "Error in creating fragment");
        }
    }

    private void removeCurrent() {
        // update the main content by replacing fragments, first by removing the old
        FragmentTransaction fragtrans = fragmentManager.beginTransaction();
        fragtrans.setCustomAnimations(R.animator.fadein, R.animator.fadeout, R.animator.fadein, R.animator.fadeout);
        fragtrans.remove(fragmentManager.findFragmentById(R.id.frame_container));
        fragtrans.commit();
    }

    public void displayEditProp(String name, String key) {
        // put the name and key strings in here for editprop access
        mEditName = name;
        mEditKey = key;

        myHandler.removeCallbacksAndMessages(null);
        mMenu = true;
        removeCurrent();
        // below replicates the visual delay seen when launching frags from navdrawer
        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                displayView(0);
            }
        }, 400);
    }

    public void displaySubFrag(String title) {
        int poscheck = -1;
        boolean isLight = isLight(prefs);

        // Look for title in array of titles to get position
        for (int i=0; i < navMenuTitles.length; i++) {
            if (navMenuTitles[i].equals(title)) {
                poscheck = i;
                break;
            }
        }

        // needs to be final for myHandler
        final int position = poscheck;

        // only do this if something was found - i.e. position != -1 - otherwise do nothing
        if (position >= 0) {
            myHandler.removeCallbacksAndMessages(null);
            mMenu = true;
            removeCurrent();
            // below replicates the visual delay seen when launching frags from navdrawer
            myHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    displayView(position);
                }
            }, 400);
        } else {
            int bgColor = isLight
                    ? getResources().getColor(R.color.snackbar_bg_light, null)
                    : getResources().getColor(R.color.snackbar_bg, null);
            showSnack(
                    findViewById(R.id.frame_container),
                    getString(R.string.general_error),
                    bgColor,
                    false);
        }
    }

    public static boolean checkIntent(Context context, Intent intent) {
        return context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }

    @Override
    public void onBackPressed() {
        boolean mKeepStack = checkPosition(mItemPosition);

        if (!fullyClosed || mDrawerLayout.isDrawerOpen(mNavView)) {
            // backpress closes drawer if open
            mDrawerLayout.closeDrawer(mNavView);
        } else if (fragmentStack.size() > 1 || mKeepStack) {
            if (!mIgnoreBack) {
                mIgnoreBack = true;

                // cancels any pending postdelays just in case
                myHandler.removeCallbacksAndMessages(null);

                // removes latest (current) entry in custom stack if it wasn't one flagged for stack keeping and not added to stack
                if (!mKeepStack) {
                    fragmentStack.pop();
                }

                // reset stack if empty - can happen w/ theme switch
                if (fragmentStack.size() == 0) {
                    resetEmptyStack(mItemPosition);
                }
                // uses fragment name to find displayview-relevant position
                final int position = Arrays.asList(navMenuFrags).indexOf(fragmentStack.lastElement());

                // set position based on above or origfrag if nested subfrag
                mItemPosition = checkSubFrag(mItemPosition, position);
                // a setup similar to onclickitem
                setTitle(navMenuTitles[mItemPosition]);
                removeCurrent();
                // below replicates the visual delay seen when launching frags from navdrawer
                myHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBackPress = true;
                        displayView(mItemPosition);
                        mIgnoreBack = false;
                    }
                }, 400);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tinker, menu);
        menu.findItem(R.id.action_launchhide).setChecked(!isLauncherIconEnabled());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle action bar actions click
        switch (item.getItemId()) {
            case R.id.action_theme:
                ThemeDialogFragment themeDiag = new ThemeDialogFragment();
                // pass current frag position
                Bundle diagType = new Bundle();
                diagType.putInt(EXTRA_START_FRAGMENT, mItemPosition);
                themeDiag.setArguments(diagType);
                themeDiag.show(getFragmentManager(), "Theme picker");
                // trigger dialog with theme options?
                return true;
            case R.id.action_launchhide:
                boolean checked = item.isChecked();
                item.setChecked(!checked);
                setLauncherIconEnabled(checked);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* Called when invalidateOptionsMenu() is triggered */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // if nav drawer is opened/opening, hide the action items
        // add in bits to enable/disable menu items that are fragment specific
        if ( openingHalf ) {
            menu.setGroupVisible(R.id.action_items, true);
            boolean isbuildprop = (mItemPosition == 2);
            boolean iseditprop = (mItemPosition == 0);
            boolean isfiswitch = (mItemPosition == 3);
            menu.findItem(R.id.action_backup).setVisible(isbuildprop);
            menu.findItem(R.id.action_restore).setVisible(isbuildprop);
            menu.findItem(R.id.action_search).setVisible(isbuildprop);
            menu.findItem(R.id.action_discard).setVisible(iseditprop);
            menu.findItem(R.id.action_delete).setVisible(iseditprop);
            menu.findItem(R.id.action_fabhide).setVisible(isfiswitch);
            menu.findItem(R.id.action_theme).setVisible(true);
            menu.findItem(R.id.action_launchhide).setVisible(!(isbuildprop || iseditprop || isfiswitch));
        } else {
            menu.setGroupVisible(R.id.action_items, false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mTitle);
        }
    }

    /* When using the mDrawerToggle, you must call it during onPostCreate() and onConfigurationChanged()... */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /* Methods related to showing/hiding app from app drawer */
    private void setLauncherIconEnabled(boolean enabled) {
        int newState;
        PackageManager packman = getPackageManager();
        if (enabled) {
            newState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        } else {
            newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        }
        packman.setComponentEnabledSetting(new ComponentName(this, LauncherActivity.class), newState, PackageManager.DONT_KILL_APP);
    }

    private boolean isLauncherIconEnabled() {
        PackageManager packman = getPackageManager();
        return (packman.getComponentEnabledSetting(new ComponentName(this, LauncherActivity.class)) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    public static void showSnack(View view, String text, int bgColor, boolean isLong) {
        // make snackbar bgs change w/ theme
        Snackbar snackbar = Snackbar.make(
                view,
                text,
                isLong ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(bgColor);
        snackbar.show();
    }

    public static void lockCurrentOrientation(Activity activity) {
        int currentRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = activity.getResources().getConfiguration().orientation;
        int frozenRotation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        switch (currentRotation) {
            case Surface.ROTATION_0:
                frozenRotation = orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case Surface.ROTATION_90:
                frozenRotation = orientation == Configuration.ORIENTATION_PORTRAIT
                        ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case Surface.ROTATION_180:
                frozenRotation = orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                break;
            case Surface.ROTATION_270:
                frozenRotation = orientation == Configuration.ORIENTATION_PORTRAIT
                        ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
        }
        activity.setRequestedOrientation(frozenRotation);
    }

}

package com.android.purenexussettings.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.android.purenexussettings.R;
import com.android.purenexussettings.TinkerActivity;

public class ThemeSwitch {
    public static final int DARK = 0;
    public static final int LIGHT = 1;
    public static final int BLACK = 2;

    public static void changeTheme(Activity activity, int position) {
        // reset activity to apply theme
        activity.finish();
        // Pass along tab number of theme settings so app restarts in same place
        activity.startActivity(new Intent(activity, activity.getClass())
                .putExtra(TinkerActivity.EXTRA_START_FRAGMENT, position)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static void setTheme(Activity activity, int theme) {
        // seems the statusbar needs this done separate to any switch
        switch(theme) {
            case LIGHT:
                activity.setTheme(R.style.AppThemeLight);
                activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.transp, null));
                activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                break;
            case BLACK:
                activity.setTheme(R.style.AppThemeBlack);
                activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.transp, null));
                break;
            case DARK:
            default:
                activity.setTheme(R.style.AppTheme);
                activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.transp, null));
                break;
        }
    }
}

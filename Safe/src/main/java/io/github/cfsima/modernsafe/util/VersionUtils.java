package io.github.cfsima.modernsafe.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import io.github.cfsima.modernsafe.R;

public class VersionUtils {
    public static String getVersionNumber(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "Unknown";
        }
    }

    public static String getApplicationName(Context context) {
        return context.getString(R.string.app_name);
    }
}

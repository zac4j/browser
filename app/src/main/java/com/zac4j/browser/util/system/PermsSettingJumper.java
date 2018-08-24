package com.zac4j.browser.util.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * Created by Zaccc on 2018/8/24.
 */
public class PermsSettingJumper {

    public static void goToPermsSettingPage(Context context) {
        Intent intent;
        switch (Build.MANUFACTURER) {
            case Manufacturer.XIAOMI:
                intent = xiaomi(context);
                break;
            case Manufacturer.HUAWEI:
                intent = huawei(context);
                break;
            case Manufacturer.OPPO:
                intent = oppo(context);
                break;
            case Manufacturer.VIVO:
                intent = vivo(context);
                break;
            case Manufacturer.MEIZU:
                intent = meizu(context);
                break;
            case Manufacturer.SONY:
                intent = sony(context);
                break;
            case Manufacturer.LG:
                intent = lg(context);
                break;
            case Manufacturer.LETV:
                intent = letv(context);
                break;
            default:
                intent = google(context);
                break;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            intent = google(context);
            context.startActivity(intent);
        }
    }

    private static Intent google(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        return intent;
    }

    private static Intent huawei(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.huawei.systemmanager",
            "com.huawei.permissionmanager.ui.MainActivity"));
        if (hasIntent(context, intent)) return intent;
        intent.setComponent(new ComponentName("com.huawei.systemmanager",
            "com.huawei.systemmanager.addviewmonitor.AddViewMonitorActivity"));
        if (hasIntent(context, intent)) return intent;
        intent.setComponent(new ComponentName("com.huawei.systemmanager",
            "com.huawei.notificationmanager.ui.NotificationManagmentActivity"));
        return intent;
    }

    private static Intent xiaomi(Context context) {
        Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
        intent.putExtra("extra_pkgname", context.getPackageName());
        if (hasIntent(context, intent)) return intent;

        intent.setPackage("com.miui.securitycenter");
        if (hasIntent(context, intent)) return intent;

        intent.setClassName("com.miui.securitycenter",
            "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
        if (hasIntent(context, intent)) return intent;

        intent.setClassName("com.miui.securitycenter",
            "com.miui.permcenter.permissions.PermissionsEditorActivity");
        return intent;
    }

    private static Intent oppo(Context context) {
        Intent intent = new Intent();
        intent.putExtra("packageName", context.getPackageName());
        intent.setClassName("com.color.safecenter",
            "com.color.safecenter.permission.floatwindow.FloatWindowListActivity");
        if (hasIntent(context, intent)) return intent;

        intent.setClassName("com.coloros.safecenter",
            "com.coloros.safecenter.sysfloatwindow.FloatWindowListActivity");
        if (hasIntent(context, intent)) return intent;

        intent.setClassName("com.oppo.safe", "com.oppo.safe.permission.PermissionAppListActivity");
        return intent;
    }

    private static Intent vivo(Context context) {
        Intent intent = new Intent();
        intent.setClassName("com.iqoo.secure",
            "com.iqoo.secure.ui.phoneoptimize.FloatWindowManager");
        intent.putExtra("packagename", context.getPackageName());
        if (hasIntent(context, intent)) return intent;

        intent.setComponent(new ComponentName("com.iqoo.secure",
            "com.iqoo.secure.safeguard.SoftPermissionDetailActivity"));
        return intent;
    }

    private static Intent meizu(Context context) {
        Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
        intent.putExtra("packageName", context.getPackageName());
        intent.setComponent(
            new ComponentName("com.meizu.safe", "com.meizu.safe.security.AppSecActivity"));
        return intent;
    }

    private static Intent sony(Context context) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("packageName", context.getPackageName());
        intent.setComponent(
            new ComponentName("com.sonymobile.cta", "com.sonymobile.cta.SomcCTAMainActivity"));
        return intent;
    }

    private static Intent lg(Context context) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("packageName", context.getPackageName());
        ComponentName comp = new ComponentName("com.android.settings",
            "com.android.settings.Settings$AccessLockSummaryActivity");
        intent.setComponent(comp);
        return intent;
    }

    private static Intent letv(Context context) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("packageName", context.getPackageName());
        ComponentName comp = new ComponentName("com.letv.android.letvsafe",
            "com.letv.android.letvsafe.PermissionAndApps");
        intent.setComponent(comp);
        return intent;
    }

    private static boolean hasIntent(Context context, Intent intent) {
        return context.getPackageManager()
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .size() > 0;
    }
}

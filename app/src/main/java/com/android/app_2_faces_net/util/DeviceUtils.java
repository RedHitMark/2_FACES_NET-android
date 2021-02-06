package com.android.app_2_faces_net.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class DeviceUtils {

    private DeviceUtils() {
        // Never instantiated this class
    }

    /**
     * Get API level to send back to server
     *
     * @return string with API level to send back to server
     */
    public static String getApiLevel() {
        return "API:" + android.os.Build.VERSION.SDK_INT;
    }

    /**
     * Get model of device to send back to server
     *
     * @return string with model of device to send back to server
     */
    public static String getDeviceModel() {
        return "Model:" + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
    }

    /**
     *
     * @param context applicationContext
     * @param onlyGranted
     * @return
     * @throws PackageManager.NameNotFoundException
     */
    public static String getPermissions(Context context, boolean onlyGranted) throws PackageManager.NameNotFoundException {
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
        String[] permissions = info.requestedPermissions;

        StringBuilder permissionsAssembled = new StringBuilder("Permissions:");
        StringBuilder permissionsGrantedAssembled = new StringBuilder("Permissions Granted:");
        for (int i = 0; i < permissions.length; i++) {
            if ((info.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                permissionsGrantedAssembled.append(permissions[i]).append('|');
            }

            permissionsAssembled.append(permissions[i]).append('|');
        }

        return onlyGranted ? permissionsGrantedAssembled.toString() : permissionsAssembled.toString();
    }

}

package com.android.captureinterface.utils;

public class currentClickUtil {
    private static String clickFilePath = null;

    public static String getClickFilePath() {
        return clickFilePath;
    }

    public static void setClickFilePath(String clickFilePath) {
        currentClickUtil.clickFilePath = clickFilePath;
    }
}

package com.android.captureinterface.utils;

public class currentClickUtil {
    private static String clickFilePath = null;
    private static String clickPackageName = null;
    private static int interfaceNum = 0;


    public static String getClickFilePath() {
        return clickFilePath;
    }

    public static void setClickFilePath(String clickFilePath) {
        currentClickUtil.clickFilePath = clickFilePath;
    }

    public static String getClickPackageName() {
        return clickPackageName;
    }

    public static void setClickPackageName(String clickPackageName) {
        currentClickUtil.clickPackageName = clickPackageName;
    }

    public static int getInterfaceNum() {
        return interfaceNum;
    }

    public static void setInterfaceNum(int interfaceNum) {
        currentClickUtil.interfaceNum = interfaceNum;
    }
}

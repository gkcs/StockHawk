package com.sam_chordas.android.stockhawk;

public class GCMStateHolder {
    private static boolean status = true;

    public static void setGCMStatus(boolean bool) {
        status = bool;
    }

    public static boolean getGCMStatus() {
        return status;
    }
}
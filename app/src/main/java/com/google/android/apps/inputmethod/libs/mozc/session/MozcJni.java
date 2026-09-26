package com.google.android.apps.inputmethod.libs.mozc.session;

public class MozcJni {
    static {
        try {
            System.loadLibrary("mozc");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public static native void initialize();
    
    // Returns serialized protobuf Command bytes
    public static native byte[] evalCommand(byte[] commandBytes);
}

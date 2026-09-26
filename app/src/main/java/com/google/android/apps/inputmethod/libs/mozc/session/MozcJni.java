package com.google.android.apps.inputmethod.libs.mozc.session;

/**
 * JNI bridge to libmozc.so.
 *
 * The package and class name must stay exactly as-is: libmozc.so exports
 * Java_com_google_android_apps_inputmethod_libs_mozc_session_MozcJni_initialize, which calls
 * RegisterNatives for evalCommand / onPostLoad / getDataVersion on this class. If any of those
 * declarations is missing, initialize() fails.
 */
public final class MozcJni {
    private static final boolean LIBRARY_LOADED;

    static {
        boolean loaded;
        try {
            System.loadLibrary("mozc");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            loaded = false;
        }
        LIBRARY_LOADED = loaded;
    }

    private MozcJni() {}

    public static boolean isLibraryLoaded() {
        return LIBRARY_LOADED;
    }

    /** Registers the native methods below. Must be called once before any other method. */
    public static native void initialize();

    /**
     * Sets up the engine.
     *
     * @param userProfileDirectoryPath writable directory for user history / learning data
     * @param dataFilePath absolute path to mozc.data (built from the same Mozc commit as the .so)
     * @return true on success. If the data file cannot be loaded, Mozc falls back to a minimal
     *     engine without the dictionary.
     */
    public static native boolean onPostLoad(String userProfileDirectoryPath, String dataFilePath);

    /** Takes a serialized mozc.commands.Command and returns the serialized result Command. */
    public static native byte[] evalCommand(byte[] commandBytes);

    /** Version string of the loaded data set. */
    public static native String getDataVersion();
}

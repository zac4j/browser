package com.zac4j.browser.util;

import android.text.TextUtils;
import com.zac4j.browser.Logger;
import java.io.Closeable;
import java.io.IOException;

/**
 * Created by Zaccc on 2018/8/22.
 */
public class Utils {

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Logger.e("Closeable", "close met error : " + e.getMessage());
            }
        }
    }

    public static boolean isNotEmptyString(String string) {
        return !TextUtils.isEmpty(string) && !"null".equalsIgnoreCase(string);
    }
}

package com.zac4j.browser.util;

import android.os.Build;
import com.zac4j.browser.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is help for getting Chinese phone manufacture system ui version.
 * Created by Zaccc on 2018/8/22.
 */
public class RomUtil {

    private static final String TAG = "RomUtil";

    private static String mRomName;
    private static String mRomVersion;

    /** Manufacturer */
    private static final String MANU_XIAOMI = "xiaomi";
    private static final String MANU_HUAWEI = "huawei";
    private static final String MANU_OPPO = "oppo";
    private static final String MANU_VIVO = "vivo";

    /** XiaoMi : MIUI */
    private static final String KEY_MIUI_VERSION = "ro.build.version.incremental"; // "7.6.15"
    private static final String KEY_MIUI_VERSION_NANE = "ro.miui.ui.version.name"; // "V8"
    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code"; // "6"
    private static final String VALUE_MIUI_CLIENT_ID_BASE = "android-xiaomi";

    /** HuaWei : EMUI */
    private static final String KEY_EMUI_VERSION = "ro.build.version.emui"; // "EmotionUI_3.0"
    private static final String KEY_EMUI_API_LEVEL = "ro.build.hw_emui_api_level"; //
    private static final String KEY_EMUI_SYSTEM_VERSION = "ro.confg.hw_systemversion";

    /** OPPO: ColorOS */
    private static final String KEY_COLOROS_VERSION = "ro.oppo.theme.version"; // "703"
    private static final String KEY_COLOROS_THEME_VERSION = "ro.oppo.version"; // ""
    private static final String KEY_COLOROS_ROM_VERSION = "ro.rom.different.version";
    // ColorOS 2.1
    private static final String VALUE_COLOROS_BASE_OS_VERSION_CONTAIN = "OPPO";
    // "OPPO/R7sm/R7sm:5.1.1/LMY47V/1440928800:user/release-keys"
    private static final String VALUE_COLOROS_CLIENT_ID_BASE = "android-oppo";

    /** vivo: FuntouchOS */
    private static final String KEY_FUNTOUCHOS_BOARD_VERSION = "ro.vivo.board.version"; // "MD"
    private static final String KEY_FUNTOUCHOS_OS_NAME = "ro.vivo.os.name"; // "Funtouch"
    private static final String KEY_FUNTOUCHOS_OS_VERSION = "ro.vivo.os.version"; // "3.0"
    private static final String KEY_FUNTOUCHOS_DISPLAY_ID = "ro.vivo.os.build.display.id";
    // "FuntouchOS_3.0"
    private static final String KEY_FUNTOUCHOS_ROM_VERSION = "ro.vivo.rom.version"; // "rom_3.1"
    private static final String VALUE_FUNTOUCHOS_CLIENT_ID_BASE = "android-vivo";

    public static String getRomType() {
        Reader reader = null;
        try {
            Properties props = new Properties();
            Process process = Runtime.getRuntime().exec("getprop");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            props.load(reader);

            String manufacturer = Build.MANUFACTURER.toLowerCase();
            switch (manufacturer) {
                case MANU_XIAOMI:
                    mRomName = Rom.MIUI.name();
                    mRomVersion = getMiuiVersion(props);
                    break;
                case MANU_HUAWEI:
                    mRomName = Rom.EMUI.name();
                    mRomVersion = getEmuiVersion(props);
                    break;
                case MANU_OPPO:
                    mRomName = Rom.ColorOS.name();
                    mRomVersion = getColorOSVersion(props);
                    break;
                case MANU_VIVO:
                    mRomName = Rom.FuntouchOS.name();
                    mRomVersion = getFuntouchOSVersion(props);
                    break;
            }

            return props.getProperty("[ro.miui.ui.version.name]");
        } catch (IOException e) {
            Logger.e(TAG, "get rom type error: " + e.getMessage());
        } finally {
            Utils.closeQuietly(reader);
        }
        return "Android";
    }

    /**
     * Get MIUI version name, like 7, v8, v9.
     *
     * @param props system properties object.
     * @return MIUI version name.
     */
    private static String getMiuiVersion(Properties props) {
        String versionName;
        if (props.containsKey(KEY_MIUI_VERSION_NANE)) {
            versionName = props.getProperty(KEY_MIUI_VERSION_NANE);
            if (!Utils.isEmptyString(versionName) && versionName.matches("[Vv]\\d+")) {
                return String.valueOf(versionName.toCharArray()[1]);
            }
        }

        if (props.containsKey(KEY_MIUI_VERSION)) {
            versionName = props.getProperty(KEY_MIUI_VERSION);
            if (!Utils.isEmptyString(versionName) && versionName.matches("[\\d.]+")) {
                return versionName;
            }
        }

        return Build.VERSION.RELEASE;
    }

    /**
     * Get EmotionUI version.
     *
     * @param props system properties object.
     * @return EmotionUI version name.
     */
    private static String getEmuiVersion(Properties props) {
        String versionName;
        if (props.containsKey(KEY_EMUI_VERSION)
            || props.containsKey(KEY_EMUI_API_LEVEL)
            || props.containsKey(KEY_EMUI_SYSTEM_VERSION)) {
            versionName = props.getProperty(KEY_EMUI_VERSION);
            // EmotionUI_3.0
            Matcher matcher = Pattern.compile("EmotionUI_([\\d.]+)").matcher(versionName);
            if (!Utils.isEmptyString(versionName) && matcher.find()) {
                try {
                    return matcher.group(1);
                } catch (Exception e) {
                    Logger.e(TAG, "get EmotionUI version error: " + e.getMessage());
                }
            }
        }
        return Build.VERSION.RELEASE;
    }

    /**
     * Get ColorOS version.
     *
     * @param props system properties object.
     * @return EmotionUI version name.
     */
    private static String getColorOSVersion(Properties props) {
        if (props.containsKey(KEY_COLOROS_VERSION)
            || props.containsKey(KEY_COLOROS_THEME_VERSION)
            || props.containsKey(KEY_COLOROS_ROM_VERSION)) {
            String versionName = props.getProperty(KEY_COLOROS_ROM_VERSION);
            Matcher matcher = Pattern.compile("ColorOS([\\d.]+)").matcher(versionName);
            if (!Utils.isEmptyString(versionName) && matcher.find()) {
                return matcher.group(1);
            }
        }
        return Build.VERSION.RELEASE;
    }

    /**
     * Get FuntouchOS version.
     *
     * @param props system properties object.
     * @return EmotionUI version name.
     */
    private static String getFuntouchOSVersion(Properties props) {
        if (props.containsKey(KEY_FUNTOUCHOS_OS_NAME) || props.containsKey(
            KEY_FUNTOUCHOS_OS_VERSION) || props.containsKey(KEY_FUNTOUCHOS_DISPLAY_ID)) {
            String versionName = props.getProperty(KEY_FUNTOUCHOS_OS_VERSION);
            if (!Utils.isEmptyString(versionName) && versionName.matches("[\\d.]+")) {
                return versionName;
            }
        }
        return Build.VERSION.RELEASE;
    }

    public enum Rom {
        MIUI, // 小米
        Flyme, // 魅族
        EMUI, // 华为
        ColorOS, // OPPO
        FuntouchOS, // vivo
        SmartisanOS, // 锤子
        EUI, // 乐视
        Sense, // HTC
        AmigoOS, // 金立
        _360OS, // 奇酷360
        NubiaUI, // 努比亚
        H2OS, // 一加
        YunOS, // 阿里巴巴
        YuLong, // 酷派

        SamSung, // 三星
        Sony, // 索尼
        Lenovo, // 联想
        LG, // LG

        Google, // 原生

        Android // Others
    }
}

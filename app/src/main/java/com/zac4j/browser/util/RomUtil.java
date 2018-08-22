package com.zac4j.browser.util;

import android.os.Build;
import android.text.TextUtils;
import com.zac4j.browser.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is help for getting Chinese popular phone system ui version.
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
    private static final String MANU_LE = "lemobile";
    private static final String MANU_GIONEE = "gionee";

    /** Universal */
    private static final String KEY_DISPLAY_ID = "ro.build.display.id";

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
    private static final String KEY_COLOROS_ROM_VERSION = "ro.rom.different.version";// ColorOS 2.1
    // "OPPO/R7sm/R7sm:5.1.1/LMY47V/1440928800:user/release-keys"
    private static final String VALUE_COLOROS_BASE_OS_VERSION_CONTAIN = "OPPO";
    private static final String VALUE_COLOROS_CLIENT_ID_BASE = "android-oppo";

    /** vivo: FuntouchOS */
    private static final String KEY_FUNTOUCHOS_BOARD_VERSION = "ro.vivo.board.version"; // "MD"
    private static final String KEY_FUNTOUCHOS_OS_NAME = "ro.vivo.os.name"; // "Funtouch"
    private static final String KEY_FUNTOUCHOS_OS_VERSION = "ro.vivo.os.version"; // "3.0"
    // "FuntouchOS_3.0"
    private static final String KEY_FUNTOUCHOS_DISPLAY_ID = "ro.vivo.os.build.display.id";
    private static final String KEY_FUNTOUCHOS_ROM_VERSION = "ro.vivo.rom.version"; // "rom_3.1"
    private static final String VALUE_FUNTOUCHOS_CLIENT_ID_BASE = "android-vivo";

    /** LeTV: EUI */
    private static final String KEY_EUI_VERSION = "ro.letv.release.version"; // "5.9.023S"
    // "5.9.023S_03111"
    private static final String KEY_EUI_VERSION_DATE = "ro.letv.release.version_date";
    private static final String KEY_EUI_NAME = "ro.product.letv_name"; // "乐1s"
    private static final String KEY_EUI_MODEL = "ro.product.letv_model"; // "Letv X500"

    // 金立 : amigo
    private static final String KEY_AMIGO_ROM_VERSION = "ro.gn.gnromvernumber";
    // "GIONEE ROM5.0.16"
    private static final String KEY_AMIGO_SYSTEM_UI_SUPPORT = "ro.gn.amigo.systemui.support";
    private static final String VALUE_AMIGO_DISPLAY_ID_CONTAIN = "amigo"; // "amigo3.5.1"
    private static final String VALUE_AMIGO_CLIENT_ID_BASE = "android-gionee";

    private static final String DEFAULT_OS_VERSION = "Android " + Build.VERSION.RELEASE;

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
                case MANU_LE:
                    mRomName = Rom.EUI.name();
                    mRomVersion = getEuiVersion(props);
                    break;
                case MANU_GIONEE:
                    mRomName = Rom.AmigoOS.name();
                    mRomVersion = getAmigoOSVersion(props);
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
        // V8, V9, V10
        if (props.containsKey(KEY_MIUI_VERSION_NANE)) {
            versionName = props.getProperty(KEY_MIUI_VERSION_NANE);
            if (!Utils.isEmptyString(versionName) && versionName.matches("[Vv]\\d+")) {
                return String.valueOf(versionName.toCharArray()[1]);
            }
        }

        // 5, 6, 7
        if (props.containsKey(KEY_MIUI_VERSION)) {
            versionName = props.getProperty(KEY_MIUI_VERSION);
            if (!Utils.isEmptyString(versionName) && versionName.matches("[\\d.]+")) {
                return versionName;
            }
        }

        return DEFAULT_OS_VERSION;
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
        return DEFAULT_OS_VERSION;
    }

    /**
     * Get ColorOS version.
     *
     * @param props system properties object.
     * @return ColorOS version name.
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
        return DEFAULT_OS_VERSION;
    }

    /**
     * Get FuntouchOS version.
     *
     * @param props system properties object.
     * @return FuntouchOS version name.
     */
    private static String getFuntouchOSVersion(Properties props) {
        if (props.containsKey(KEY_FUNTOUCHOS_OS_NAME) || props.containsKey(
            KEY_FUNTOUCHOS_OS_VERSION) || props.containsKey(KEY_FUNTOUCHOS_DISPLAY_ID)) {
            String versionName = props.getProperty(KEY_FUNTOUCHOS_OS_VERSION);
            if (!Utils.isEmptyString(versionName) && versionName.matches("[\\d.]+")) {
                return versionName;
            }
        }
        return DEFAULT_OS_VERSION;
    }

    /**
     * Get EUI version.
     *
     * @param props system properties object.
     * @return EUI version name.
     */
    private static String getEuiVersion(Properties props) {
        if (props.containsKey(KEY_EUI_VERSION)
            || props.containsKey(KEY_EUI_NAME)
            || props.containsKey(KEY_EUI_MODEL)) {
            String versionName = props.getProperty(KEY_EUI_VERSION);
            Matcher matcher = Pattern.compile("([\\d.]+)[^\\d]*").matcher(versionName); // 5.9.023S
            if (!TextUtils.isEmpty(versionName) && matcher.find()) {
                return matcher.group(1);
            }
        }
        return DEFAULT_OS_VERSION;
    }

    /**
     * Get AmigoOS version.
     *
     * @param props system properties object.
     * @return AmigoOS version name.
     */
    private static String getAmigoOSVersion(Properties props) {
        if (props.containsKey(KEY_AMIGO_ROM_VERSION) || props.containsKey(
            KEY_AMIGO_SYSTEM_UI_SUPPORT)) {
            String versionName = props.getProperty(KEY_DISPLAY_ID);
            Matcher matcher =
                Pattern.compile("amigo([\\d.]+)[a-zA-Z]*").matcher(versionName); // "amigo3.5.1"
            if (!TextUtils.isEmpty(versionName) && matcher.find()) {
                return matcher.group(1);
            }
        }
        return DEFAULT_OS_VERSION;
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

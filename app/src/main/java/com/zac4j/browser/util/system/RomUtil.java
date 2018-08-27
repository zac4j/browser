package com.zac4j.browser.util.system;

import android.os.Build;
import android.text.TextUtils;
import com.zac4j.browser.Logger;
import com.zac4j.browser.util.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is help for getting China popular phone system ui version.
 * Created by Zaccc on 2018/8/22.
 */
public class RomUtil {

    private static final String TAG = "RomUtil";

    private static final String DEFAULT_ROM_VERSION = Build.VERSION.RELEASE;

    private static Properties sProps;

    static {
        BufferedReader reader = null;
        try {
            sProps = new Properties();
            Process process = Runtime.getRuntime().exec("getprop");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            sProps.load(reader);
        } catch (IOException e) {
            Logger.e(TAG, "Initialize system properties reader occur error: " + e.getMessage());
        } finally {
            Utils.closeQuietly(reader);
        }
    }

    public static Rom getRom() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Rom rom = new Rom();
        switch (manufacturer) {
            case Manufacturer.XIAOMI:
                rom.setType(Rom.MIUI);
                rom.setVersion(getMiuiVersion());
                break;
            case Manufacturer.HUAWEI:
                rom.setType(Rom.EMUI);
                rom.setVersion(getEmuiVersion());
                break;
            case Manufacturer.OPPO:
                rom.setType(Rom.ColorOS);
                rom.setVersion(getColorOSVersion());
                break;
            case Manufacturer.VIVO:
                rom.setType(Rom.FuntouchOS);
                rom.setVersion(getFuntouchOSVersion());
                break;
            case Manufacturer.MEIZU:
                rom.setType(Rom.FlymeOS);
                rom.setVersion(getFlymeOSVersion());
                break;
            case Manufacturer.LETV:
                rom.setType(Rom.EUI);
                rom.setVersion(getEuiVersion());
                break;
            case Manufacturer.GIONEE:
                rom.setType(Rom.AmigoOS);
                rom.setVersion(getAmigoOSVersion());
                break;
            case Manufacturer.SMARTISAN:
                rom.setType(Rom.SmartisanOS);
                rom.setVersion(getSmartisanOSVersion());
                break;
            default:
                rom.setType("Android");
                rom.setVersion(Build.VERSION.RELEASE);
        }
        return rom;
    }

    /**
     * Get MIUI version name, like 7, v8, v9.
     *
     * @return MIUI version name.
     */
    private static String getMiuiVersion() {
        String version = DEFAULT_ROM_VERSION;

        if (sProps == null) {
            return version;
        }

        // V8, V9, V10
        if (sProps.containsKey(RomPropertyKeys.MIUI_VERSION_CODE) || sProps.containsKey(
            RomPropertyKeys.MIUI_VERSION_NANE)) {
            version = sProps.getProperty(RomPropertyKeys.MIUI_VERSION_NANE);
            if (Utils.isNotEmptyString(version) && version.matches("\\[[Vv]\\d+]")) {
                version = obtainMidProperty(version);
            }
        }

        // 5, 6, 7
        if (sProps.contains(RomPropertyKeys.MIUI_VERSION)) {
            version = sProps.getProperty(RomPropertyKeys.MIUI_VERSION);
            if (Utils.isNotEmptyString(version) && version.matches("\\[[\\d.]+]")) {
                version = obtainMidProperty(version);
            }
        }

        return version;
    }

    /**
     * Get EmotionUI version.
     *
     * @return EmotionUI version name.
     */
    private static String getEmuiVersion() {

        String version = DEFAULT_ROM_VERSION;

        if (sProps == null) {
            return version;
        }

        if (sProps.containsKey(RomPropertyKeys.EMUI_VERSION) || sProps.containsKey(
            RomPropertyKeys.EMUI_API_LEVEL) || sProps.containsKey(
            RomPropertyKeys.EMUI_SYSTEM_VERSION)) {
            version = sProps.getProperty(RomPropertyKeys.EMUI_VERSION);
            // EmotionUI_3.0
            Matcher matcher = Pattern.compile("\\[EmotionUI_([\\d.]+)]").matcher(version);
            if (Utils.isNotEmptyString(version) && matcher.find()) {
                try {
                    return matcher.group(1);
                } catch (Exception e) {
                    Logger.e(TAG, "get EmotionUI version error: " + e.getMessage());
                }
            }
        }
        return version;
    }

    /**
     * Get ColorOS version.
     *
     * @return ColorOS version name.
     */
    private static String getColorOSVersion() {

        String version = DEFAULT_ROM_VERSION;

        if (sProps == null) {
            return version;
        }

        if (sProps.containsKey(RomPropertyKeys.COLOROS_VERSION) || sProps.containsKey(
            RomPropertyKeys.COLOROS_THEME_VERSION) || sProps.containsKey(
            RomPropertyKeys.COLOROS_ROM_VERSION)) {
            version = sProps.getProperty(RomPropertyKeys.COLOROS_ROM_VERSION);
            Matcher matcher = Pattern.compile("\\[ColorOS([\\d.]+)]").matcher(version);
            if (Utils.isNotEmptyString(version) && matcher.find()) {
                return matcher.group(1);
            }
        }
        return version;
    }

    /**
     * Get FuntouchOS version.
     *
     * @return FuntouchOS version name.
     */
    private static String getFuntouchOSVersion() {

        String version = DEFAULT_ROM_VERSION;

        if (sProps == null) {
            return version;
        }

        if (sProps.containsKey(RomPropertyKeys.FUNTOUCHOS_OS_NAME) || sProps.containsKey(
            RomPropertyKeys.FUNTOUCHOS_OS_VERSION) || sProps.containsKey(
            RomPropertyKeys.FUNTOUCHOS_DISPLAY_ID)) {
            version = sProps.getProperty(RomPropertyKeys.FUNTOUCHOS_OS_VERSION);
            // [3.1]
            if (Utils.isNotEmptyString(version) && version.matches("\\[[\\d.]+]")) {
                version = obtainMidProperty(version);
            }
        }
        return version;
    }

    /**
     * Get Flyme OS version.
     *
     * @return Flyme OS version name.
     */
    private static String getFlymeOSVersion() {

        String version = DEFAULT_ROM_VERSION;

        if (sProps == null) {
            return version;
        }

        if (sProps.containsKey(RomPropertyKeys.FLYME_SETUP) || sProps.containsKey(
            RomPropertyKeys.FLYME_PUBLISHED)) {
            version = sProps.getProperty(RomPropertyKeys.DISPLAY_ID);
            // Flyme OS 4.5.4.2U
            Matcher matcher = Pattern.compile("\\[Flyme[^\\d]*([\\d.]+)[^\\d]*]").matcher(version);
            if (Utils.isNotEmptyString(version) && matcher.find()) {
                return matcher.group(1);
            }
        }
        return version;
    }

    /**
     * Get EUI version.
     *
     * @return EUI version name.
     */
    private static String getEuiVersion() {

        String version = DEFAULT_ROM_VERSION;

        if (sProps == null) {
            return version;
        }

        if (sProps.containsKey(RomPropertyKeys.EUI_VERSION) || sProps.containsKey(
            RomPropertyKeys.EUI_NAME) || sProps.containsKey(RomPropertyKeys.EUI_MODEL)) {
            version = sProps.getProperty(RomPropertyKeys.EUI_VERSION);
            // 5.9.023S
            Matcher matcher = Pattern.compile("\\[([\\d.]+)[^\\d]*]").matcher(version);
            if (Utils.isNotEmptyString(version) && matcher.find()) {
                return matcher.group(1);
            }
        }
        return version;
    }

    /**
     * Get AmigoOS version.
     *
     * @return AmigoOS version name.
     */
    private static String getAmigoOSVersion() {
        String version = DEFAULT_ROM_VERSION;

        if (sProps == null) {
            return version;
        }

        if (sProps.containsKey(RomPropertyKeys.AMIGO_ROM_VERSION) || sProps.containsKey(
            RomPropertyKeys.AMIGO_SYSTEM_UI_SUPPORT)) {
            version = sProps.getProperty(RomPropertyKeys.DISPLAY_ID);
            // "amigo3.5.1"
            Matcher matcher = Pattern.compile("\\[amigo([\\d.]+)[a-zA-Z]*]").matcher(version);
            if (Utils.isNotEmptyString(version) && matcher.find()) {
                return matcher.group(1);
            }
        }
        return version;
    }

    /**
     * Get Smartisan OS version.
     *
     * @return Smartisan OS version.
     */
    private static String getSmartisanOSVersion() {
        String version = DEFAULT_ROM_VERSION;

        if (sProps == null) {
            return version;
        }

        // ro.smartisan.version=2.1.2-2015120822-user-511
        if (sProps.containsKey(RomPropertyKeys.SMARTISAN_VERSION)) {
            version = sProps.getProperty(RomPropertyKeys.SMARTISAN_VERSION);
            String[] versionTexts = version.split("-");
            if (versionTexts.length != 0) {
                version = versionTexts[0];
            }
        }
        return version;
    }

    /**
     * Obtain middle property of given text.
     *
     * @param text text for take middle property.
     * @return the property of given text
     */
    private static String obtainMidProperty(String text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }

        int start = text.indexOf("[");
        int end = text.indexOf("]");

        return TextUtils.substring(text, start + 1, end);
    }
}

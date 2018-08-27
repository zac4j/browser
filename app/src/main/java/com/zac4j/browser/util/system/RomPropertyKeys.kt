package com.zac4j.browser.util.system

/**
 * Created by Zaccc on 2018/8/23.
 */
class RomPropertyKeys {

    companion object {
        /** Universal  */
        const val DISPLAY_ID = "ro.build.display.id"

        /** XiaoMi : MIUI  */
        const val MIUI_VERSION = "ro.build.version.incremental" // "7.6.15"
        const val MIUI_VERSION_NANE = "[ro.miui.ui.version.name]" // "[V9]"
        const val MIUI_VERSION_CODE = "[ro.miui.ui.version.code]" // "[7]"

        /** HuaWei : EMUI  */
        const val EMUI_VERSION = "[ro.build.version.emui]" // "EmotionUI_3.0"
        const val EMUI_API_LEVEL = "[ro.build.hw_emui_api_level]" //
        const val EMUI_SYSTEM_VERSION = "[ro.confg.hw_systemversion]"

        /** OPPO: ColorOS  */
        const val COLOROS_VERSION = "[ro.oppo.theme.version]" // "703"
        const val COLOROS_THEME_VERSION = "[ro.oppo.version]" // ""
        const val COLOROS_ROM_VERSION = "[ro.rom.different.version]"// ColorOS 2.1

        /** vivo: FuntouchOS  */
        const val FUNTOUCHOS_OS_NAME = "[ro.vivo.os.name]" // "Funtouch"
        const val FUNTOUCHOS_OS_VERSION = "[ro.vivo.os.version]" // "3.0"
        // "FuntouchOS_3.0"
        const val FUNTOUCHOS_DISPLAY_ID = "[ro.vivo.os.build.display.id]"

        /** LeTV: EUI  */
        const val EUI_VERSION = "[ro.letv.release.version]" // "5.9.023S"
        // "5.9.023S_03111"
        const val EUI_NAME = "[ro.product.letv_name]" // "乐1s"
        const val EUI_MODEL = "[ro.product.letv_model]" // "Letv X500"

        /** MeiZu : Flyme  */
        const val FLYME_PUBLISHED = "[ro.flyme.published]" // "true"
        const val FLYME_SETUP = "[ro.meizu.setupwizard.flyme]" // "true"

        /** Gionee: Amigo  */
        const val AMIGO_ROM_VERSION = "[ro.gn.gnromvernumber]"
        // "GIONEE ROM5.0.16"
        const val AMIGO_SYSTEM_UI_SUPPORT = "[ro.gn.amigo.systemui.support]"

        /** 🔨 : SmartisanOS */
        const val SMARTISAN_VERSION = "ro.smartisan.version"
    }

}
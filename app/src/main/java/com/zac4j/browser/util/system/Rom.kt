package com.zac4j.browser.util.system

import android.os.Build

/**
 * Created by Zaccc on 2018/8/23.
 */
class Rom {
    companion object {
        const val DEFAULT = "Android"
        const val MIUI = "MIUI"
        const val Flyme = "Flyme"
        const val EMUI = "EMUI"
        const val ColorOS = "ColorOS"
        const val FuntouchOS = "FuntouchOS"
        const val EUI = "EUI"
        const val AmigoOS = "AmigoOS"
    }

    var type: String = DEFAULT
    var version: String = Build.VERSION.RELEASE
}
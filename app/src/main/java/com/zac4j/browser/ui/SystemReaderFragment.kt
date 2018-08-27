package com.zac4j.browser.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.zac4j.browser.R
import com.zac4j.browser.util.system.RomUtil

/**
 * Created by Zaccc on 2018/8/24.
 */
class SystemReaderFragment : Fragment() {

    private lateinit var mRootLayout: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_system_reader, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mRootLayout = view.findViewById(R.id.root_layout)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val rom = RomUtil.getRom()
        val romInfo = "Rom type: " + rom.type + ", version: " + rom.version
        val romView = TextView(activity)
        romView.text = romInfo

        mRootLayout.addView(romView)
    }
}
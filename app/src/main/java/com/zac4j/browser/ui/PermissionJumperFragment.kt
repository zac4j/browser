package com.zac4j.browser.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zac4j.browser.R

/**
 * Created by Zaccc on 2018/8/24.
 */
class PermissionJumperFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_permission_setting, container, false)
    }
}
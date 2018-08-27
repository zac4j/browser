package com.zac4j.browser.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.zac4j.browser.R
import com.zac4j.browser.util.permission.PermsSettingJumper

/**
 * Created by Zaccc on 2018/8/24.
 */
class PermissionJumperFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_permission_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.perms_set_btn_go).setOnClickListener {
            PermsSettingJumper.goToPermsSettingPage(activity)
        }
    }

}
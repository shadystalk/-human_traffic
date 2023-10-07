package com.shrifd.ls149.router

import android.content.Context
import com.alibaba.android.arouter.launcher.ARouter


object RouterController {

    object LS149 {
        private const val GROUP = "/ls149"

        //主界面
        const val MAIN = "$GROUP/Main"


        fun jumpMainActivity(context: Context) {
            ARouter.getInstance().build(MAIN)
                .navigation(context)
        }


    }
}
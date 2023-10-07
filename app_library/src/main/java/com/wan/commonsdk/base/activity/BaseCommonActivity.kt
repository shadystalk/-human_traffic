package com.wan.commonsdk.base.activity

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import com.jess.arms.base.BaseActivity
import com.wan.commonsdk.R
import com.wan.commonsdk.base.BasePresenterImpl
import com.wan.commonsdk.base.IBaseContract

import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.common_activity_base_toolbar.*


abstract class BaseCommonActivity<P : BasePresenterImpl<*, *>> : BaseActivity<P>(),
    IBaseContract.IBaseView {

    override fun initView(savedInstanceState: Bundle?): Int {
        return R.layout.common_activity_base_toolbar
    }

    override fun initData(savedInstanceState: Bundle?) {
    }

    open fun initFlContainer(flContainer: FrameLayout) {
        fl_container.addView(LayoutInflater.from(this).inflate(getContentView(), null))
    }


    open fun initToolBar(
        toolBar: RelativeLayout,
        rlBack: RelativeLayout,
        toolBarTv: TextView,
        flRight: FrameLayout
    ) {
        rlBack.setOnClickListener {
            finish()
        }
    }

    abstract fun getContentView(): Int

    open fun showToolbar(): Boolean {
        return true
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        ImmersionBar.with(this).hideBar(BarHide.FLAG_HIDE_BAR).init()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        //设置toolbar
        val layoutResID = initView(savedInstanceState)
        if (layoutResID != 0) {
            if (showToolbar()) {
                initToolBar(rlTop, rlLeft, toolBarTv, flRight)
            } else {
                rlTop.visibility = View.GONE
            }
            initFlContainer(fl_container)
        }

        window.decorView.setOnSystemUiVisibilityChangeListener {
            var uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or  //布局位于状态栏下方
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or  //全屏
                    View.SYSTEM_UI_FLAG_FULLSCREEN or  //隐藏导航栏
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            uiOptions = if (Build.VERSION.SDK_INT >= 19) {
                uiOptions or 0x00001000
            } else {
                uiOptions or View.SYSTEM_UI_FLAG_LOW_PROFILE
            }
            window.decorView.systemUiVisibility = uiOptions
        }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }


    override fun isActive(): Boolean {
        return !isFinishing && !supportFragmentManager.isDestroyed
    }

    override fun getContext(): Context {
        return this
    }

    override fun showMessage(message: String) {
        Toasty.info(this, message).show()
    }

    override fun killMyself() {
        finish()
    }



}

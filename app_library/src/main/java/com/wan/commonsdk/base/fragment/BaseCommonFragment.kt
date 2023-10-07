package com.wan.commonsdk.base.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import com.jess.arms.base.BaseFragment
import com.wan.commonsdk.base.IBaseContract
import es.dmoral.toasty.Toasty

abstract class BaseCommonFragment<P : BaseFragmentPresenter> : BaseFragment<P>(),
    IBaseContract.IBaseView {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPresenter?.onCreate()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        mPresenter?.onHiddenChanged(hidden)
        if (hidden) {
            ImmersionBar.with(this).hideBar(BarHide.FLAG_HIDE_BAR).init()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mPresenter?.onActivityResult(requestCode, resultCode, data)
    }

    override fun getContext(): Context {
        return mContext
    }

    override fun isActive(): Boolean {
        return isAdded
    }

    override fun showMessage(message: String) {
        Toasty.info(context, message).show()
    }

    override fun initData(savedInstanceState: Bundle?) {
    }

    override fun setData(data: Any?) {
    }

    override fun killMyself() {
        if (isActive())
            activity?.finish()
    }
}
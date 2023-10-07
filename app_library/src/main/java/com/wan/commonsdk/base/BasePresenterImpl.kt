package com.wan.commonsdk.base

import android.app.Activity
import com.jess.arms.mvp.BasePresenter


abstract class BasePresenterImpl<M : IBaseContract.IIModel, V : IBaseContract.IBaseView>(model: M, rootView: V) :
    BasePresenter<M, V>(model, rootView) {

    fun isActive(): Boolean {
        return mRootView != null && mRootView.isActive()
    }

    fun getActivity(): Activity {
        return mRootView as Activity
    }

    override fun useEventBus(): Boolean {
        return false
    }



}
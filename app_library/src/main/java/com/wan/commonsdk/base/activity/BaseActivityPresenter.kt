package com.wan.commonsdk.base.activity

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.jess.arms.di.scope.ActivityScope
import com.wan.commonsdk.base.BasePresenterImpl
import com.wan.commonsdk.base.IBaseContract
import javax.inject.Inject

/**
 * author : 万涛
 * date : 2020/4/10 15:34
 */
@ActivityScope
open class BaseActivityPresenter @Inject constructor(
    model: IBaseContract.IIModel,
    rootView: IBaseContract.IBaseView
) :
    BasePresenterImpl<IBaseContract.IIModel, IBaseContract.IBaseView>(model, rootView) {


    //生命周期开始调用的方法
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    open fun onCreate() {

    }
}
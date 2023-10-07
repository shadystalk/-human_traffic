package com.wan.commonsdk.base.fragment

import android.content.Intent
import androidx.fragment.app.Fragment
import com.jess.arms.di.scope.ActivityScope
import com.wan.commonsdk.base.BasePresenterImpl
import com.wan.commonsdk.base.IBaseContract
import javax.inject.Inject

/**
 * author : 万涛
 * date : 2020/4/10 15:34
 */
@ActivityScope
open class BaseFragmentPresenter @Inject constructor(
    model: IBaseContract.IIModel,
    rootView: IBaseContract.IBaseView
) :
    BasePresenterImpl<IBaseContract.IIModel, IBaseContract.IBaseView>(model, rootView) {

    open fun onCreate() {

    }

    fun getFragment(): Fragment {
        return mRootView as Fragment
    }

    open fun onHiddenChanged(hidden: Boolean) {
    }

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    }
}
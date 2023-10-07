package com.wan.commonsdk.base

import android.content.Context
import com.jess.arms.mvp.IModel
import com.jess.arms.mvp.IView

/**
 * author : 万涛
 * date : 2020/4/10 15:25
 */
interface IBaseContract
{

    interface IBaseView : IView {
        fun isActive(): Boolean

        fun getContext(): Context
    }

    //Model层定义接口,外部只需关心Model返回的数据,无需关心内部细节,即是否使用缓存
    interface IIModel : IModel {

    }
}
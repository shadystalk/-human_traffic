package com.shrifd.ls149.splash.mvp.ui.activity

import android.os.Bundle
import com.jess.arms.di.component.AppComponent
import com.shrifd.ls149.splash.di.component.DaggerSplashComponent
import com.shrifd.ls149.splash.di.module.SplashModule
import com.shrifd.ls149.splash.mvp.contract.SplashContract
import com.shrifd.ls149.splash.mvp.presenter.SplashPresenter
import com.wan.commonsdk.base.activity.BaseCommonActivity


/**
 * ================================================
 * Description:
 * <p>
 * Created by MVPArmsTemplate on 02/20/2021 13:35
 * <a href="mailto:jess.yan.effort@gmail.com">Contact me</a>
 * <a href="https://github.com/JessYanCoding">Follow me</a>
 * <a href="https://github.com/JessYanCoding/MVPArms">Star me</a>
 * <a href="https://github.com/JessYanCoding/MVPArms/wiki">See me</a>
 * <a href="https://github.com/JessYanCoding/MVPArmsTemplate">模版请保持更新</a>
 * ================================================
 */
/**
 * 如果没presenter
 * 你可以这样写
 *
 * @ActivityScope(請注意命名空間) class NullObjectPresenterByActivity
 * @Inject constructor() : IPresenter {
 * override fun onStart() {
 * }
 *
 * override fun onDestroy() {
 * }
 * }
 */
class SplashActivity : BaseCommonActivity<SplashPresenter>() , SplashContract.View {

    override fun setupActivityComponent(appComponent:AppComponent) {
        DaggerSplashComponent //如找不到该类,请编译一下项目
                .builder()
                .appComponent(appComponent)
                .splashModule(SplashModule(this))
                .build()
                .inject(this)
    }

    override fun initView(savedInstanceState: Bundle?): Int {
        return 0
    }

    override fun getContentView(): Int {
        return 0
    }
}

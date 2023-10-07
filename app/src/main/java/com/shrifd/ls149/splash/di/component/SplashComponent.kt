package com.shrifd.ls149.splash.di.component

import dagger.Component
import com.jess.arms.di.component.AppComponent

import com.shrifd.ls149.splash.di.module.SplashModule

import com.jess.arms.di.scope.ActivityScope
import com.shrifd.ls149.splash.mvp.ui.activity.SplashActivity


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
@ActivityScope
@Component(modules = arrayOf(SplashModule::class),dependencies = arrayOf(AppComponent::class))
interface SplashComponent {
    fun inject(activity: SplashActivity)
}

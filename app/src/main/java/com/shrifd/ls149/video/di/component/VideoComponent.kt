package com.shrifd.ls149.video.di.component

import com.jess.arms.di.component.AppComponent
import com.jess.arms.di.scope.ActivityScope
import com.shrifd.ls149.video.di.module.VideoModule
import com.shrifd.ls149.video.mvp.ui.activity.VideoActivity
import dagger.Component


/**
 * ================================================
 * Description:
 * <p>
 * Created by MVPArmsTemplate on 04/13/2021 17:50
 * <a href="mailto:jess.yan.effort@gmail.com">Contact me</a>
 * <a href="https://github.com/JessYanCoding">Follow me</a>
 * <a href="https://github.com/JessYanCoding/MVPArms">Star me</a>
 * <a href="https://github.com/JessYanCoding/MVPArms/wiki">See me</a>
 * <a href="https://github.com/JessYanCoding/MVPArmsTemplate">模版请保持更新</a>
 * ================================================
 */
@ActivityScope
@Component(modules = arrayOf(VideoModule::class),dependencies = arrayOf(AppComponent::class))
interface VideoComponent {
    fun inject(activity: VideoActivity)
}

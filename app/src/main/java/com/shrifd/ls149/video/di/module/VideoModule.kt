package com.shrifd.ls149.video.di.module

import com.jess.arms.di.scope.ActivityScope
import com.shrifd.ls149.video.mvp.contract.VideoContract
import com.shrifd.ls149.video.mvp.model.VideoModel
import dagger.Module
import dagger.Provides


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
@Module
 //构建ServiceSearchModule时,将View的实现类传进来,这样就可以提供View的实现类给presenter
class VideoModule(private val view: VideoContract.View) {
    @ActivityScope
    @Provides
    fun provideView(): VideoContract.View{
        return this.view
    }

    @ActivityScope
    @Provides
    fun provideModel(model: VideoModel): VideoContract.Model{
        return model
    }


}

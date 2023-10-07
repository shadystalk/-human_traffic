package com.shrifd.ls149.main.di.module

import com.jess.arms.di.scope.ActivityScope
import com.shrifd.ls149.main.mvp.contract.MainContract
import com.shrifd.ls149.main.mvp.model.MainModel
import dagger.Module
import dagger.Provides
import java.text.DecimalFormat
import java.text.NumberFormat


/**
 * ================================================
 * Description:
 * <p>
 * Created by MVPArmsTemplate on 02/20/2021 13:36
 * <a href="mailto:jess.yan.effort@gmail.com">Contact me</a>
 * <a href="https://github.com/JessYanCoding">Follow me</a>
 * <a href="https://github.com/JessYanCoding/MVPArms">Star me</a>
 * <a href="https://github.com/JessYanCoding/MVPArms/wiki">See me</a>
 * <a href="https://github.com/JessYanCoding/MVPArmsTemplate">模版请保持更新</a>
 * ================================================
 */
@Module
//构建MainModule时,将View的实现类传进来,这样就可以提供View的实现类给presenter
class MainModule(private val view: MainContract.View) {

    @ActivityScope
    @Provides
    fun provideMainView(): MainContract.View {
        return this.view
    }

    @ActivityScope
    @Provides
    fun provideMainModel(model: MainModel): MainContract.Model {
        return model
    }

    @ActivityScope
    @Provides
    fun provideNumberFormat(): NumberFormat {
        return DecimalFormat("#,###")
    }

}

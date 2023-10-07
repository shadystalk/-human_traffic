package com.shrifd.ls149.video.mvp.model

import com.jess.arms.di.scope.ActivityScope
import com.jess.arms.integration.IRepositoryManager
import com.jess.arms.mvp.BaseModel
import com.shrifd.ls149.video.mvp.contract.VideoContract
import javax.inject.Inject


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
class VideoModel
@Inject
constructor(repositoryManager: IRepositoryManager) : BaseModel(repositoryManager), VideoContract.Model{


    override fun onDestroy() {
          super.onDestroy();
    }
}

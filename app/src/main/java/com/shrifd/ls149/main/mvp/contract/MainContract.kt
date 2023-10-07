package com.shrifd.ls149.main.mvp.contract


import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import com.shrifd.ls149.detect.CameraSurfaceRender
import com.wan.commonsdk.base.IBaseContract


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
interface MainContract {
    interface View :IBaseContract.IBaseView
    {
        fun initGlSurfaceView(render: CameraSurfaceRender)

        fun showFps(fps:String)

        fun showTrackView(up:Int,down:Int,bitmap: Bitmap)

        fun showFlowPeople(toDayEnter: Int, totalEnter: Int, totalOut: Int, toDayOut: Int)

        fun getGlSurfaceView():GLSurfaceView

    }

    //Model层定义接口,外部只需关心Model返回的数据,无需关心内部细节,即是否使用缓存
    interface Model : IBaseContract.IIModel
    {
    }

}

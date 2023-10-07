package com.shrifd.ls149.video.mvp.contract

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.view.View
import com.shrifd.ls149.detect.CameraSurfaceRender
import com.shrifd.ls149.detect.VideoSurfaceRender
import com.wan.commonsdk.base.IBaseContract


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
interface VideoContract {
    //对于经常使用的关于UI的方法可以定义到IView中,如显示隐藏进度条,和显示文字消息
    interface View : IBaseContract.IBaseView
    {
        fun initGlSurfaceView(render: VideoSurfaceRender)

        fun showFps(fps:String)

        fun showTrackView(up:Int,down:Int,bitmap: Bitmap)

        fun getGlSurfaceView(): GLSurfaceView

        fun getCanvasView():android.view.View

        fun getLineView(): android.view.View
    }

    //Model层定义接口,外部只需关心Model返回的数据,无需关心内部细节,即是否使用缓存
    interface Model : IBaseContract.IIModel
    {

    }

}

package com.shrifd.ls149.video.mvp.ui.activity

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.jess.arms.di.component.AppComponent
import com.shrifd.ls149.R
import com.shrifd.ls149.detect.VideoSurfaceRender
import com.shrifd.ls149.video.di.component.DaggerVideoComponent
import com.shrifd.ls149.video.di.module.VideoModule
import com.shrifd.ls149.video.mvp.contract.VideoContract
import com.shrifd.ls149.video.mvp.presenter.VideoPresenter
import com.wan.commonsdk.base.activity.BaseCommonActivity
import kotlinx.android.synthetic.main.activity_video.*

class VideoActivity : BaseCommonActivity<VideoPresenter>(), VideoContract.View {

    override fun setupActivityComponent(appComponent: AppComponent) {
        DaggerVideoComponent //如找不到该类,请编译一下项目
            .builder()
            .appComponent(appComponent)
            .videoModule(VideoModule(this))
            .build()
            .inject(this)
    }

    override fun getContentView(): Int {
      return R.layout.activity_video
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun initGlSurfaceView(render: VideoSurfaceRender) {
        gl_surface_view.setEGLContextClientVersion(3)
        gl_surface_view.setRenderer(render)
        gl_surface_view.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun getGlSurfaceView(): GLSurfaceView {
        return  gl_surface_view
    }

    override fun showFps(fpsTxt: String) {
        fps.text=fpsTxt
    }

    override fun showTrackView(up:Int,down:Int,bitmap: Bitmap) {
        canvasView.scaleType = ImageView.ScaleType.FIT_XY
        canvasView.setImageBitmap(bitmap)
        tvUP.text = up.toString()
        tvDown.text = down.toString()
    }

    override fun getCanvasView(): View {
        return canvasView
    }

    override fun getLineView(): View {
        return viewLine
    }
}
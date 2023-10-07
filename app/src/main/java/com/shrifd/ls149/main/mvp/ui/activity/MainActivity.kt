package com.shrifd.ls149.main.mvp.ui.activity


import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.jess.arms.di.component.AppComponent
import com.shrifd.ls149.R
import com.shrifd.ls149.anim.AnimUtil
import com.shrifd.ls149.detect.CameraSurfaceRender
import com.shrifd.ls149.main.di.component.DaggerMainComponent
import com.shrifd.ls149.main.di.module.MainModule
import com.shrifd.ls149.main.mvp.contract.MainContract
import com.shrifd.ls149.main.mvp.presenter.MainPresenter
import com.shrifd.ls149.router.RouterController
import com.wan.commonsdk.base.activity.BaseCommonActivity
import kotlinx.android.synthetic.main.activity_main_stand.canvasView
import kotlinx.android.synthetic.main.activity_main_stand.fps
import kotlinx.android.synthetic.main.activity_main_stand.gl_surface_view
import kotlinx.android.synthetic.main.activity_main_stand.tvTodayEnter
import kotlinx.android.synthetic.main.activity_main_stand.tvTodayOut
import java.text.NumberFormat
import javax.inject.Inject


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
@Route(path = RouterController.LS149.MAIN)
class MainActivity : BaseCommonActivity<MainPresenter>(), MainContract.View {


    @Inject
    lateinit var numberFormat: NumberFormat



    override fun setupActivityComponent(appComponent: AppComponent) {
        DaggerMainComponent //如找不到该类,请编译一下项目
            .builder()
            .appComponent(appComponent)
            .mainModule(MainModule(this))
            .build()
            .inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )


    }


    override fun getContentView(): Int {
        return  R.layout.activity_main_stand
    }

    override fun initGlSurfaceView(render: CameraSurfaceRender) {
        gl_surface_view.setEGLContextClientVersion(3)
        gl_surface_view.setRenderer(render)
        gl_surface_view.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun getGlSurfaceView(): GLSurfaceView {
        return gl_surface_view
    }

    override fun showFps(fpsTxt: String) {
        fps.text = fpsTxt
    }

    override fun showTrackView(up: Int, down: Int, bitmap: Bitmap) {
        canvasView.scaleType = ImageView.ScaleType.FIT_XY
        canvasView.setImageBitmap(bitmap)
    }

    override fun showFlowPeople(toDayEnter: Int, totalEnter: Int, totalOut: Int, toDayOut: Int) {
        AnimUtil.setText(
            tvTodayEnter, numberFormat.parse(tvTodayEnter.text.toString()).toInt(),
            toDayEnter, numberFormat
        )
        AnimUtil.setText(
            tvTodayOut, numberFormat.parse(tvTodayOut.text.toString()).toInt(),
            toDayOut, numberFormat
        )
    }

    override fun initToolBar(
        toolBar: RelativeLayout,
        rlBack: RelativeLayout,
        toolBarTv: TextView,
        flRight: FrameLayout
    ) {
        super.initToolBar(toolBar, rlBack, toolBarTv, flRight)
        rlBack.visibility = View.GONE
        toolBarTv.text = "上海阿法迪门禁系统"
    }


    override fun showToolbar(): Boolean {
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Process.killProcess(Process.myPid())
    }

    override fun onDestroy() {
        super.onDestroy()
        System.exit(0)
    }


}

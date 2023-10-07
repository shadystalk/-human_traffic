package com.shrifd.ls149.main.mvp.presenter

import android.graphics.*
import android.os.Handler
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.jess.arms.di.scope.ActivityScope
import com.jess.arms.utils.ArmsUtils.sp2px
import com.shrifd.ls149.R
import com.shrifd.ls149.detect.CameraSurfaceRender
import com.shrifd.ls149.detect.InferenceResult
import com.shrifd.ls149.detect.ObjectFlow
import com.shrifd.ls149.entity.FlowPeople
import com.shrifd.ls149.main.mvp.contract.MainContract
import com.wan.commonsdk.base.activity.BaseActivityPresenter
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.layout_camera.*
import java.lang.ref.WeakReference
import java.text.DecimalFormat
import java.util.*
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
@ActivityScope
class MainPresenter
@Inject
constructor(val model: MainContract.Model, var rootView: MainContract.View) :
    BaseActivityPresenter(model, rootView) {

    private var flowPeople: FlowPeople? = null

    private lateinit var mTrackResultBitmap: Bitmap
    private lateinit var mTrackResultCanvas: Canvas
    private lateinit var mTrackResultPaint: Paint
    private lateinit var mTrackResultTextPaint: Paint
    private lateinit var mHandler: MyHandler
    private lateinit var render: CameraSurfaceRender
    private lateinit var mPorterDuffXfermodeClear: PorterDuffXfermode
    private lateinit var mPorterDuffXfermodeSRC: PorterDuffXfermode

    private var objectFlow = ObjectFlow(430, 470)

    //相机的预览大小
    private val width = 640 //mRender.getWidth()
    private val height = 480 //mRender.getHeight()
    private var up = 0
    private var down = 0
    private class MyHandler(presenter: MainPresenter) : Handler() {
        private val weakReference: WeakReference<MainPresenter>
        private val decimalFormat = DecimalFormat("00.00")
        override fun handleMessage(msg: android.os.Message) {
            val presenter: MainPresenter? = weakReference.get()
            if (presenter != null) {
                if (msg.what == 0) {
                    val fps = msg.obj as Float
                    val fpsStr = decimalFormat.format(fps.toDouble())
                    presenter.rootView.showFps(fpsStr)
                } else {
                    presenter.showTrackSelectResults()
                }
            }
        }

        init {
            weakReference = WeakReference<MainPresenter>(presenter)
        }
    }

    override fun onCreate() {
        super.onCreate()
        //初始化摄像头
        initGl()
    }

    /**
     * gl
     */
    private fun initGl() {
        mHandler = MyHandler(this)
        render = CameraSurfaceRender(width, height, rootView.getGlSurfaceView(), mHandler)
        //绑定
        rootView.initGlSurfaceView(render)
        mTrackResultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444)
        mTrackResultCanvas = Canvas(mTrackResultBitmap)
        //用于画线
        mTrackResultPaint = Paint()
        mTrackResultPaint.color = -0xf91401
        mTrackResultPaint.strokeJoin = Paint.Join.ROUND
        mTrackResultPaint.strokeCap = Paint.Cap.ROUND
        mTrackResultPaint.strokeWidth = 4f
        mTrackResultPaint.style = Paint.Style.STROKE
        mTrackResultPaint.textAlign = Paint.Align.LEFT
        mTrackResultPaint.textSize = sp2px(rootView.getContext(), 10f).toFloat()
        mTrackResultPaint.typeface = Typeface.SANS_SERIF
        mTrackResultPaint.isFakeBoldText = false
        //用于文字
        mTrackResultTextPaint = Paint()
        mTrackResultTextPaint.color =
            ContextCompat.getColor(rootView.getContext(), R.color.common_red)
        mTrackResultTextPaint.strokeWidth = 2f
        mTrackResultTextPaint.textAlign = Paint.Align.LEFT
        mTrackResultTextPaint.textSize = sp2px(rootView.getContext(), 20f).toFloat()
        mTrackResultTextPaint.typeface = Typeface.SANS_SERIF
        mTrackResultTextPaint.isFakeBoldText = false
        mPorterDuffXfermodeClear = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        mPorterDuffXfermodeSRC = PorterDuffXfermode(PorterDuff.Mode.SRC)

    }

    /**
     * 显示跟踪框和计数,不显示绘制框，滥用cpu崩溃
     */
    private fun showTrackSelectResults() {
        // clear canvas
        mTrackResultPaint.xfermode = mPorterDuffXfermodeClear
        mTrackResultCanvas.drawPaint(mTrackResultPaint)
        mTrackResultPaint.xfermode = mPorterDuffXfermodeSRC

        val recognitions: ArrayList<InferenceResult.Recognition> = render.trackResult

        //人流量计数
        val flows: IntArray = objectFlow.cameraCount(recognitions)
        up += flows[0]
        down += flows[1]
        for (i in recognitions.indices) {
            val rego: InferenceResult.Recognition = recognitions[i]
            val detection: RectF = rego.location
            mTrackResultCanvas.drawRect(detection, mTrackResultPaint)
            mTrackResultCanvas.drawText(
                rego.trackId.toString(),
                detection.left + 5,
                detection.bottom - 5,
                mTrackResultTextPaint
            )
        }
        if (flows[0] != 0 || flows[1] != 0) {

            if (flowPeople == null) {
                flowPeople = FlowPeople()
            }
            flowPeople!!.toDayEnter += flows[0]
            flowPeople!!.toDayOut += flows[1]
            rootView.showFlowPeople(
                flowPeople!!.toDayEnter,
                flowPeople!!.totalEnter,
                flowPeople!!.totalOut,
                flowPeople!!.toDayOut
            )
        }
        rootView.showTrackView(up, down, mTrackResultBitmap)
    }


    override fun useEventBus(): Boolean {
        return true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        render.onResume()
        rootView.getGlSurfaceView().onResume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        render.onPause()
        rootView.getGlSurfaceView().onPause()
    }

}

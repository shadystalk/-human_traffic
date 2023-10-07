package com.shrifd.ls149.video.mvp.presenter

import android.graphics.*
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.jess.arms.di.scope.ActivityScope
import com.jess.arms.utils.ArmsUtils
import com.shrifd.ls149.detect.InferenceResult
import com.shrifd.ls149.detect.ObjectFlow
import com.shrifd.ls149.detect.VideoSurfaceRender
import com.shrifd.ls149.video.mvp.contract.VideoContract
import com.wan.commonsdk.base.activity.BaseActivityPresenter
import java.lang.ref.WeakReference
import java.text.DecimalFormat
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
class VideoPresenter
@Inject
constructor(var model: VideoContract.Model, var rootView: VideoContract.View) :
    BaseActivityPresenter(model, rootView) {

    private lateinit var mTrackResultBitmap: Bitmap
    private lateinit var mTrackResultCanvas: Canvas
    private lateinit var mTrackResultPaint: Paint
    private lateinit var mTrackResultTextPaint: Paint
    private lateinit var mHandler: MyHandler
    private lateinit var render: VideoSurfaceRender
    private lateinit var mPorterDuffXfermodeClear: PorterDuffXfermode
    private lateinit var mPorterDuffXfermodeSRC: PorterDuffXfermode
    private lateinit var objectFlow: ObjectFlow

    private var s_width = 640 //mRender.getWidth()
    private var s_height = 640 //mRender.getHeight()
    private var up = 0
    private var down = 0

    private lateinit var player: ExoPlayer

    private class MyHandler(presenter: VideoPresenter) : Handler() {
        private val weakReference: WeakReference<VideoPresenter>
        private val decimalFormat = DecimalFormat("00.00")
        override fun handleMessage(msg: android.os.Message) {
            val presenter: VideoPresenter? = weakReference.get()
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
            weakReference = WeakReference<VideoPresenter>(presenter)
        }
    }

    override fun onCreate() {
        super.onCreate()

        s_height = ArmsUtils.getScreenHeidth(rootView.getContext())

        s_width = s_height * 360 / 640
        Log.i("SSSSSSSSSSSSS", "s_width: " + s_width);
        Log.i("SSSSSSSSSSSSS", "s_height: " + s_height);

        //val width = height * 720 / 1280
        //输入图像的高度
        val thresholdHeight=s_height / 3
        objectFlow = ObjectFlow(thresholdHeight,thresholdHeight)

        val layoutParams = FrameLayout.LayoutParams(s_width, s_height)
        layoutParams.gravity = Gravity.CENTER
        rootView.getGlSurfaceView().layoutParams = layoutParams

        val canvasLayoutParams = FrameLayout.LayoutParams(s_width, s_height)
        canvasLayoutParams.gravity = Gravity.CENTER
        rootView.getCanvasView().layoutParams = canvasLayoutParams

        val lineLayoutParams = FrameLayout.LayoutParams(s_width, 2)
        lineLayoutParams.topMargin =thresholdHeight
        lineLayoutParams.leftMargin=(ArmsUtils.getScreenWidth(rootView.getContext())-s_width)/2
        rootView.getLineView().layoutParams = lineLayoutParams

        // 传入Uri、加载数据的工厂、解析数据的工厂，就能创建出MediaSource
        val videoPath = Environment.getExternalStorageDirectory().path + "/test.mp4"
        val uri = Uri.parse(videoPath)
        val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(rootView.getContext())
        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))
        player = ExoPlayer.Builder(rootView.getContext().applicationContext).build()
        player.setMediaSource(mediaSource)
        player.prepare()
        //设置播放速度和音调均为2倍速
        //player.playbackParameters = PlaybackParameters(4f, 4f)
        player.repeatMode = Player.REPEAT_MODE_ALL

        //初始化摄像头
        initGl()
    }

    /**
     * gl
     */
    private fun initGl() {
        mHandler = MyHandler(this)
        render = VideoSurfaceRender(s_width, s_height, rootView.getGlSurfaceView(), mHandler)
        render.setVideoSurfaceRenderListener(object :
            VideoSurfaceRender.IVideoSurfaceRenderListener {
            override fun startPlay() {
                getActivity().runOnUiThread {
                    val surface = Surface(render.getmSurfaceTexture())
                    player.setVideoSurface(surface)
                    player.play()
                }
            }

            override fun stopPlay() {
                player.stop()
            }

        })
        //绑定
        rootView.initGlSurfaceView(render)
        mTrackResultBitmap = Bitmap.createBitmap(s_width, s_height, Bitmap.Config.ARGB_4444)
        mTrackResultCanvas = Canvas(mTrackResultBitmap)
        //用于画线
        mTrackResultPaint = Paint()
        mTrackResultPaint.color = -0xf91401
        mTrackResultPaint.strokeJoin = Paint.Join.ROUND
        mTrackResultPaint.strokeCap = Paint.Cap.ROUND
        mTrackResultPaint.strokeWidth = 4f
        mTrackResultPaint.style = Paint.Style.STROKE
        mTrackResultPaint.textAlign = Paint.Align.LEFT
        mTrackResultPaint.textSize = ArmsUtils.sp2px(rootView.getContext(), 10f).toFloat()
        mTrackResultPaint.typeface = Typeface.SANS_SERIF
        mTrackResultPaint.isFakeBoldText = false
        //用于文字
        mTrackResultTextPaint = Paint()
        mTrackResultTextPaint.color = -0xf91401
        mTrackResultTextPaint.strokeWidth = 2f
        mTrackResultTextPaint.textAlign = Paint.Align.LEFT
        mTrackResultTextPaint.textSize = ArmsUtils.sp2px(rootView.getContext(), 12f).toFloat()
        mTrackResultTextPaint.typeface = Typeface.SANS_SERIF
        mTrackResultTextPaint.isFakeBoldText = false
        mPorterDuffXfermodeClear = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        mPorterDuffXfermodeSRC = PorterDuffXfermode(PorterDuff.Mode.SRC)

    }

    /**
     * 显示跟踪框和计数
     */
    private fun showTrackSelectResults() {
        // clear canvas
        mTrackResultPaint.xfermode = mPorterDuffXfermodeClear
        mTrackResultCanvas.drawPaint(mTrackResultPaint)
        mTrackResultPaint.xfermode = mPorterDuffXfermodeSRC
        //detect result
        // todo, mRender.getTrackResult() is slow, you can move it to a new thread
        val recognitions: ArrayList<InferenceResult.Recognition> = render.getTrackResult()
        //人流量计数
        val flows: IntArray = objectFlow.cameraCount(recognitions)
        up += flows[0]
        down += flows[1]
        for (i in recognitions.indices) {
            val rego: InferenceResult.Recognition = recognitions[i]
            val detection: RectF = rego.location
            mTrackResultCanvas.drawRect(detection, mTrackResultPaint)
            mTrackResultCanvas.drawText(
                rego.getTitle() + rego.getTrackId(),
                detection.left + 5,
                detection.bottom - 5,
                mTrackResultTextPaint
            )
        }
        //TimberUtil.i("up: $up ,down: $down")
        rootView.showTrackView(up, down, mTrackResultBitmap)
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
        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}


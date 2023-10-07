/**
 * Created by Randall on 2018/10/15
 * <p>
 * RKNN inference Camera Demo
 */

package com.shrifd.ls149.detect;

import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glFinish;
import static android.opengl.GLES20.glViewport;
import static com.shrifd.ls149.detect.PostProcess.INPUT_SIZE;
import static java.lang.Thread.sleep;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;

import com.rockchip.gdapc.demo.glhelper.TextureProgram;
import com.rockchip.gdapc.demo.glhelper.VideoTextureProgram;
import com.shrifd.ls149.util.Utils;

import java.io.IOException;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class VideoSurfaceRender implements GLSurfaceView.Renderer {
    public static final String TAG = "ssd";

    private SurfaceTexture mSurfaceTexture;

    private VideoTextureProgram mTextureProgram;     // Draw texture2D (include camera texture (GL_TEXTURE_EXTERNAL_OES) and normal GL_TEXTURE_2D texture)
    //    private LineProgram mLineProgram;           // Draw detection result
    private GLSurfaceView mGLSurfaceView;
    private int mOESTextureId = -1;    //camera texture ID

    // for inference
    private InferenceWrapper mInferenceWrapper;
    private String fileDirPath;     // file dir to store model cache
    private ImageBufferQueue mImageBufferQueue;    // intermedia between camera thread and  inference thread
    private InferenceResult mInferenceResult = new InferenceResult();  // detection result
    private int mWidth;    //surface width
    private int mHeight;    //surface height
    private Handler mMainHandler;   // ui thread handle,  update fps
    private Object cameraLock = new Object();
    private volatile boolean mStopInference = false;
    String realModelPath;

    String realReIdModelPath;
    String realLabelPath;

    private int imgWidth;
    private int imgHeight;

    IVideoSurfaceRenderListener videoSurfaceRenderListener;

    public VideoSurfaceRender(int imgWidth,int imgHeight,GLSurfaceView glSurfaceView, Handler handler) {
        this.imgWidth=imgWidth;
        this.imgHeight=imgHeight;
        mGLSurfaceView = glSurfaceView;
        mMainHandler = handler;
        fileDirPath = mGLSurfaceView.getContext().getCacheDir().getAbsolutePath();
        try {
            mInferenceResult.init();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String modelPath = "yolov5/yolov5s_relu_tk2_RK356X_i8.rknn";
        String labelPath = "yolov5/coco_80_labels_list.txt";
        String reIdModelPath = "yolov5/osnet_x0_25_imagenet.rknn";

        realModelPath = mGLSurfaceView.getContext().getCacheDir().toString() + "/" + modelPath;
        Utils.copyFileFromAssets(mGLSurfaceView.getContext(), modelPath, realModelPath);
        realLabelPath = mGLSurfaceView.getContext().getCacheDir().toString() + "/" + labelPath;
        Utils.copyFileFromAssets(mGLSurfaceView.getContext(), labelPath, realLabelPath);

        realReIdModelPath = mGLSurfaceView.getContext().getCacheDir().toString() + "/" + reIdModelPath;
        Utils.copyFileFromAssets(mGLSurfaceView.getContext(), reIdModelPath, realReIdModelPath);

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        startTrack();
        startCamera();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    private void startTrack() {
        mInferenceResult.reset();
        mImageBufferQueue = new ImageBufferQueue(3, INPUT_SIZE, INPUT_SIZE);
        mOESTextureId = TextureProgram.createOESTextureObject();
        mSurfaceTexture = new SurfaceTexture(mOESTextureId);
        mTextureProgram = new VideoTextureProgram(mGLSurfaceView.getContext());
//        mLineProgram = new LineProgram(mGLSurfaceView.getContext());

        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mGLSurfaceView.requestRender();
            }
        });

        mStopInference = false;

        mInferenceThread = new Thread(mInferenceRunnable);
        mInferenceThread.start();
    }


    @Override
    public void onDrawFrame(GL10 gl) {

        if (mStopInference) {
            return;
        }

        ImageBufferQueue.ImageBuffer imageBuffer = mImageBufferQueue.getFreeBuffer();

        if (imageBuffer == null) {
            return;
        }

        // render to offscreen
        glBindFramebuffer(GL_FRAMEBUFFER, imageBuffer.mFramebuffer);
        glViewport(0, 0, imageBuffer.mWidth, imageBuffer.mHeight);
        mTextureProgram.drawFeatureMap(mOESTextureId);
        glFinish();
        mImageBufferQueue.postBuffer(imageBuffer);

        // main screen
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, mWidth, mHeight);
        mTextureProgram.draw(mOESTextureId);

//        mLineProgram.draw(recognitions);

        mSurfaceTexture.updateTexImage();

        // update main screen
        // draw track result
        updateMainUI(1, 0);
    }

    private void updateMainUI(int type, Object data) {
        Message msg = mMainHandler.obtainMessage();
        msg.what = type;
        msg.obj = data;
        mMainHandler.sendMessage(msg);
    }

    public ArrayList<InferenceResult.Recognition> getTrackResult() {
        return mInferenceResult.getResult();
    }

    public void onPause() {

    }

    public void onResume() {

    }

    private void stopTrack() {

        mStopInference = true;
        try {
            mInferenceThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (mSurfaceTexture != null) {
            int[] t = {mOESTextureId};
            GLES20.glDeleteTextures(1, t, 0);

            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        if (mTextureProgram != null) {
            mTextureProgram.release();
            mTextureProgram = null;
        }

//        if (mLineProgram != null) {
//            mLineProgram.release();
//            mLineProgram = null;
//        }

        if (mImageBufferQueue != null) {
            mImageBufferQueue.release();
            mImageBufferQueue = null;
        }
    }

    private void startCamera() {
        if (videoSurfaceRenderListener != null) {
            videoSurfaceRenderListener.startPlay();
        }
    }

    private void stopCamera() {
        if (videoSurfaceRenderListener != null) {
            videoSurfaceRenderListener.stopPlay();
        }
    }

    private Thread mInferenceThread;
    private Runnable mInferenceRunnable = new Runnable() {
        public void run() {

            int count = 0;
            long oldTime = System.currentTimeMillis();
            long currentTime;

            mInferenceWrapper = new InferenceWrapper(imgWidth,imgHeight,realModelPath, realLabelPath,realReIdModelPath);


            while (!mStopInference) {
                ImageBufferQueue.ImageBuffer buffer = mImageBufferQueue.getReadyBuffer();

                if (buffer == null) {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                InferenceResult.OutputBuffer outputs = mInferenceWrapper.run(buffer.mTextureId);

                mInferenceResult.setResult(outputs);

                mImageBufferQueue.releaseBuffer(buffer);

                if (++count >= 30) {
                    currentTime = System.currentTimeMillis();

                    float fps = count * 1000.f / (currentTime - oldTime);

                    //Log.d(TAG, "current fps = " + fps);

                    oldTime = currentTime;
                    count = 0;
                    updateMainUI(0, fps);

                }

            }

            mInferenceWrapper.deinit();
            mInferenceWrapper = null;
        }
    };

    public void setVideoSurfaceRenderListener(IVideoSurfaceRenderListener videoSurfaceRenderListener) {
        this.videoSurfaceRenderListener = videoSurfaceRenderListener;
    }

    public SurfaceTexture getmSurfaceTexture() {
        return mSurfaceTexture;
    }

    public interface IVideoSurfaceRenderListener {
        void startPlay();

        void stopPlay();
    }

}

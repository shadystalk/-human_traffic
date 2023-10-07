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
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.rockchip.gdapc.demo.glhelper.TextureProgram;
import com.shrifd.ls149.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class CameraSurfaceRender implements GLSurfaceView.Renderer {
    public static final String TAG = "ssd";
    String realModelPath;
    String realReIdModelPath;
    String realLabelPath;
    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture;
    private TextureProgram mTextureProgram;     // Draw texture2D (include camera texture (GL_TEXTURE_EXTERNAL_OES) and normal GL_TEXTURE_2D texture)
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
    private int imgWidth;
    private int imgHeight;

    private Thread mInferenceThread;

    private Runnable mInferenceRunnable = new Runnable() {
        public void run() {

            int count = 0;
            long oldTime = System.currentTimeMillis();
            long currentTime;

            mInferenceWrapper = new InferenceWrapper(imgWidth, imgHeight, realModelPath, realLabelPath, realReIdModelPath);


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

                try {
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
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (Error e) {
                    e.printStackTrace();
                }
            }

            mInferenceWrapper.deinit();
            mInferenceWrapper = null;
        }
    };

    public CameraSurfaceRender(int imgWidth, int imgHeight, GLSurfaceView glSurfaceView, Handler handler) {

        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
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
        startCamera();
        startTrack();
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
        mTextureProgram = new TextureProgram(mGLSurfaceView.getContext());
//        mLineProgram = new LineProgram(mGLSurfaceView.getContext());

        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mGLSurfaceView.requestRender();
            }
        });

        try {
            if (mCamera != null) {
                mCamera.setPreviewTexture(mSurfaceTexture);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        stopCamera();
        stopTrack();

    }

    public void onResume() {
        startCamera();
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
        if (mCamera != null) {
            return;
        }
        try {
            synchronized (cameraLock) {
                Camera.CameraInfo camInfo = new Camera.CameraInfo();

//            int numCameras = Camera.getNumberOfCameras();
//            for (int i = 0; i < numCameras; i++) {
//                Camera.getCameraInfo(i, camInfo);
//                //if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                mCamera = Camera.open(i);
//                break;
//                //}
//            }
                mCamera = Camera.open(0);
                if (mCamera == null) {
                    return;
                }
                Camera.Parameters camParams = mCamera.getParameters();

                List<Camera.Size> sizes = camParams.getSupportedPreviewSizes();
                for (int i = 0; i < sizes.size(); i++) {
                    Camera.Size size = sizes.get(i);
                    Log.v(TAG, "Camera Supported Preview Size = " + size.width + "x" + size.height);
                }
                //camParams.setPreviewSize(1920, 1080);
                camParams.setPreviewSize(640, 480);

                camParams.setRecordingHint(true);
                mCamera.setDisplayOrientation(0);
                mCamera.setParameters(camParams);
                //int[] screenSize = ScreenUtils.getScreenSize(mGLSurfaceView.getContext(),false);
                //focusOnRect(new Rect(screenSize[0]/2-300,screenSize[1]/2-300,screenSize[0]/2+300,screenSize[1]/2+300));
                if (mSurfaceTexture != null) {
                    try {
                        mCamera.setPreviewTexture(mSurfaceTexture);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mCamera.startPreview();

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopCamera() {
        if (mCamera == null)
            return;

        synchronized (cameraLock) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }

        Log.i(TAG, "stopped camera");
    }


    public InferenceWrapper getmInferenceWrapper() {
        return mInferenceWrapper;
    }
}

package com.shrifd.ls149.detect;

import android.graphics.Bitmap;

/**
 * Created by randall on 18-4-18.
 */

public class InferenceWrapper {
    static {
        System.loadLibrary("rkssd4j");
    }

    public InferenceResult.OutputBuffer mOutputs;


    public InferenceWrapper(int width, int height, String modelPath,
                            String lablePath,
                            String reIdModelPath) {

        mOutputs = new InferenceResult.OutputBuffer();
        init(width, height, modelPath, lablePath, reIdModelPath);
    }

    public void deinit() {
        native_deinit();
        mOutputs.objs = null;
        mOutputs = null;
    }

    /*
     *  params:
     *       textureId: 纹理ID， 大小 300x300 格式 RGBA
     *  return:
     *      返回检测结果
     *      locations还需要后处理才是真正的坐标，具体参考PostProcess.java
     *      confidence, confidence还需要做expit处理才是真正的得分，具体参考PostProcess.java
     * */
    public InferenceResult.OutputBuffer run(int textureId) {
        mOutputs.objs = native_run(textureId);
        return mOutputs;
    }


    public static int create_direct_texture(int texWidth, int texHeight, int format) {
        return native_create_direct_texture(texWidth, texHeight, format);
    }

    public static boolean delete_direct_texture(int texId) {
        return native_delete_direct_texture(texId);
    }

    private native int init(int width, int height, String modelPath,
                            String labelPath,
                            String reIdModelPath);

    private native void native_deinit();

    /*
     *  descption:
     *       检测, 只适用于Android平台
     *  params:
     *       textureId:      输入图像纹理Id
     *       outputLocations:    用于保存预测框位置(xmin, ymin, xmax, ymax)(需要后处理，具体参考PostProcess.java)
     *       outputClasses:  用于保存confidence, confidence还需要做expit处理((float) (1. / (1. + Math.exp(-x)));)
     * */
    private native Obj[] native_run(int textureId);

    public native Bitmap capture( int width,int height);

    private static native int native_create_direct_texture(int texWidth, int texHeight, int format);

    private static native boolean native_delete_direct_texture(int texId);
}
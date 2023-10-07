#include <android/bitmap.h>
#include <jni.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <pthread.h>
#include <sys/syscall.h>


#include <sched.h>
#include <opencv2/imgproc/types_c.h>
#include "direct_texture.h"
#include "yolov5/Yolov5Detect.h"
#include "yolov5/Utils.h"
#include "object_tracker/track_link.h"
#include "opencv2/core.hpp"
#include "opencv2/imgproc.hpp"

jstring charToJString(JNIEnv *env, char *pat) {
    //定义java String类 strClass
    jclass strClass = (env)->FindClass("java/lang/String");
    //获取java String类方法String(byte[],String)的构造器,用于将本地byte[]数组转换为一个新String
    jmethodID ctorID = (env)->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    //建立byte数组
    jbyteArray bytes = (env)->NewByteArray((jsize) strlen(pat));
    //将char* 转换为byte数组
    (env)->SetByteArrayRegion(bytes, 0, (jsize) strlen(pat), (jbyte *) pat);
    //设置String, 保存语言类型,用于byte数组转换至String时的参数
    jstring encoding = (env)->NewStringUTF("UTF-8");
    //将byte数组转换为java String,并输出
    return (jstring) (env)->NewObject(strClass, ctorID, bytes, encoding);
}

int mat2Bitmap
        (JNIEnv *env, cv::Mat src, jobject bitmap, jboolean needPremultiplyAlpha) {
    AndroidBitmapInfo info;
    void *pixels = 0;

    try {
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        LOGD("nMatToBitmap src.dims: %d, info.height: %d,info.width: %d,src.rows: %d,src.cols: %d",
             src.dims, info.height, info.width, src.rows, src.cols);
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                  info.format == ANDROID_BITMAP_FORMAT_RGB_565);
        CV_Assert(src.dims == 2 && info.height == (uint32_t) src.rows &&
                  info.width == (uint32_t) src.cols);
        CV_Assert(src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_8UC4);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);
        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if (src.type() == CV_8UC1) {
                LOGD("nMatToBitmap: CV_8UC1 -> RGBA_8888");
                cvtColor(src, tmp, cv::COLOR_GRAY2RGBA);
            } else if (src.type() == CV_8UC3) {
                LOGD("nMatToBitmap: CV_8UC3 -> RGBA_8888");
                cvtColor(src, tmp, cv::COLOR_RGB2RGBA);
            } else if (src.type() == CV_8UC4) {
                LOGD("nMatToBitmap: CV_8UC4 -> RGBA_8888");
                if (needPremultiplyAlpha) cvtColor(src, tmp, cv::COLOR_RGBA2mRGBA);
                else src.copyTo(tmp);
            }
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            cv::Mat tmp(info.height, info.width, CV_8UC2, pixels);
            if (src.type() == CV_8UC1) {
                LOGD("nMatToBitmap: CV_8UC1 -> RGB_565");
                cvtColor(src, tmp, cv::COLOR_GRAY2BGR565);
            } else if (src.type() == CV_8UC3) {
                LOGD("nMatToBitmap: CV_8UC3 -> RGB_565");
                cvtColor(src, tmp, cv::COLOR_RGB2BGR565);
            } else if (src.type() == CV_8UC4) {
                LOGD("nMatToBitmap: CV_8UC4 -> RGB_565");
                cvtColor(src, tmp, cv::COLOR_RGBA2BGR565);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return 0;
    } catch (const cv::Exception &e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("nMatToBitmap caught cv::Exception: %s", e.what());
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("nMatToBitmap caught unknown exception (...)");
    }
    return -1;
}

inline std::string jstring_to_cpp_string(JNIEnv *env, jstring jstr) {
    // In java, a unicode char will be encoded using 2 bytes (utf16).
    // so jstring will contain characters utf16. std::string in c++ is
    // essentially a string of bytes, not characters, so if we want to
    // pass jstring from JNI to c++, we have convert utf16 to bytes.
    if (!jstr) {
        return "";
    }
    const jclass stringClass = env->GetObjectClass(jstr);
    const jmethodID getBytes =
            env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(
            jstr, getBytes, env->NewStringUTF("UTF-8"));

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte *pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    std::string ret = std::string(reinterpret_cast<char *>(pBytes), length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_shrifd_ls149_detect_InferenceWrapper_native_1create_1direct_1texture(JNIEnv *env,
                                                                              jclass clazz,
                                                                              jint tex_width,
                                                                              jint tex_height,
                                                                              jint format) {
    return (jint) gDirectTexture.createDirectTexture((int) tex_width, (int) tex_height,
                                                     (int) format);
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_shrifd_ls149_detect_InferenceWrapper_native_1delete_1direct_1texture(JNIEnv *env,
                                                                              jclass clazz,
                                                                              jint tex_id) {
    return (jboolean) gDirectTexture.deleteDirectTexture((int) tex_id);
}

extern "C"
JNIEXPORT jlong
JNICALL
Java_com_shrifd_ls149_detect_ObjectTracker_native_1create(JNIEnv *env, jobject thiz) {
    return create_tracker();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_shrifd_ls149_detect_ObjectTracker_native_1destroy(JNIEnv *env, jobject thiz,
                                                           jlong handle) {
    destroy_tracker(handle);

}

extern "C"
JNIEXPORT void JNICALL
Java_com_shrifd_ls149_detect_ObjectTracker_native_1track(JNIEnv *env, jobject thiz, jlong handle,
                                                         jint max_track_lifetime,
                                                         jint track_input_num,
                                                         jfloatArray track_input_locations,
                                                         jintArray track_input_class,
                                                         jfloatArray track_input_score,
                                                         jintArray track_output_num,
                                                         jfloatArray track_output_locations,
                                                         jintArray track_output_class,
                                                         jfloatArray track_output_score,
                                                         jintArray track_output_id, jint width,
                                                         jint height) {
    jboolean inputCopy = JNI_FALSE;
    jfloat *const c_track_input_locations = env->GetFloatArrayElements(track_input_locations,
                                                                       &inputCopy);
    jint *const c_track_input_class = env->GetIntArrayElements(track_input_class, &inputCopy);
    jfloat *const c_track_input_score = env->GetFloatArrayElements(track_input_score, &inputCopy);
    jboolean outputCopy = JNI_FALSE;

    jint *const c_track_output_num = env->GetIntArrayElements(track_output_num, &outputCopy);
    jfloat *const c_track_output_locations = env->GetFloatArrayElements(track_output_locations,
                                                                        &outputCopy);
    jint *const c_track_output_class = env->GetIntArrayElements(track_output_class, &outputCopy);
    jfloat *const c_track_output_score = env->GetFloatArrayElements(track_output_score, &inputCopy);
    jint *const c_track_output_id = env->GetIntArrayElements(track_output_id, &outputCopy);


    track(handle, (int) max_track_lifetime,
          (int) track_input_num, (float *) c_track_input_locations, (int *) c_track_input_class,
          (float *) c_track_input_score,
          (int *) c_track_output_num, (float *) c_track_output_locations,
          (int *) c_track_output_class, (float *) c_track_output_score,
          (int *) c_track_output_id, (int) width, (int) height);

    env->ReleaseFloatArrayElements(track_input_locations, c_track_input_locations, JNI_ABORT);
    env->ReleaseIntArrayElements(track_input_class, c_track_input_class, JNI_ABORT);
    env->ReleaseFloatArrayElements(track_input_score, c_track_input_score, JNI_ABORT);

    env->ReleaseIntArrayElements(track_output_num, c_track_output_num, 0);
    env->ReleaseFloatArrayElements(track_output_locations, c_track_output_locations, 0);
    env->ReleaseIntArrayElements(track_output_class, c_track_output_class, 0);
    env->ReleaseFloatArrayElements(track_output_score, c_track_output_score, 0);
    env->ReleaseIntArrayElements(track_output_id, c_track_output_id, 0);
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_shrifd_ls149_detect_InferenceWrapper_init(JNIEnv *env, jobject thiz,
                                                   jint width, jint height, jstring model_path,
                                                   jstring label_path, jstring reid_model_path) {
    std::string modelPath = jstring_to_cpp_string(env, model_path);
    std::string labelPath = jstring_to_cpp_string(env, label_path);
    std::string reIdModelPath = jstring_to_cpp_string(env, reid_model_path);

    Yolov5Detector::yolov5create(width, height, modelPath, labelPath, reIdModelPath);
    return 0;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_shrifd_ls149_detect_InferenceWrapper_native_1deinit(JNIEnv *env, jobject thiz) {
    Yolov5Detector::yolov5Destory();
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_shrifd_ls149_detect_InferenceWrapper_native_1run(JNIEnv *env, jobject thiz,
                                                          jint texture_id) {
    jobjectArray resultArray = nullptr;
    try {
        detect_result_group_t detect_result_group = Yolov5Detector::yolov5Predict(
                (int) texture_id);
        if (detect_result_group.count > 0) {
            jclass objCls = env->FindClass("com/shrifd/ls149/detect/Obj");
            jmethodID constructortorId = env->GetMethodID(objCls, "<init>", "()V");
            jfieldID nameId = env->GetFieldID(objCls, "name", "Ljava/lang/String;");
            jfieldID probsId = env->GetFieldID(objCls, "probs", "F");
            jfieldID trackId = env->GetFieldID(objCls, "trackId", "I");
            jfieldID leftId = env->GetFieldID(objCls, "left", "I");
            jfieldID topId = env->GetFieldID(objCls, "top", "I");
            jfieldID rightId = env->GetFieldID(objCls, "right", "I");
            jfieldID bottomId = env->GetFieldID(objCls, "bottom", "I");
            resultArray = env->NewObjectArray(detect_result_group.count, objCls, nullptr);
            for (int i = 0; i < detect_result_group.count; ++i) {
                jobject jObj = env->NewObject(objCls, constructortorId);
                //jstring name = charToJString(env,detect_result_group->results[i].name);
                //env->SetObjectField(jObj, nameId, name);
                //LOGE("detect_result_group  trackId=%d\n",  detect_result_group.results[i].trackId);
                env->SetIntField(jObj, trackId, detect_result_group.results[i].trackId);
                env->SetFloatField(jObj, probsId, detect_result_group.results[i].prop);
                env->SetIntField(jObj, leftId, detect_result_group.results[i].box.left);
                env->SetIntField(jObj, topId, detect_result_group.results[i].box.top);
                env->SetIntField(jObj, rightId, detect_result_group.results[i].box.right);
                env->SetIntField(jObj, bottomId, detect_result_group.results[i].box.bottom);
                env->SetObjectArrayElement(resultArray, i, jObj);
            }
        }
    } catch (...) {
        // 抛出异常
//        jclass exceptionClazz = env->FindClass("java/lang/IllegalArgumentException");
//        env->ThrowNew(exceptionClazz, "native层捕获到异常，向java层抛出");
    }
    return resultArray;


}



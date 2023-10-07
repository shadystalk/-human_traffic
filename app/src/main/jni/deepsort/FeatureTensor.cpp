/*!
    @Description : https://github.com/shaoshengsong/
    @Author      : shaoshengsong
    @Date        : 2022-09-21 04:32:26
*/

//#include "globalconfig.h"
#include "FeatureTensor.h"
#include "../yolov5/Utils.h"
#include <iostream>

FeatureTensor *FeatureTensor::instance = NULL;

FeatureTensor *FeatureTensor::getInstance() {
    if (instance == NULL) {
        instance = new FeatureTensor();
    }
    return instance;
}

/**
 * in: 1x3x256*128
 * out: 1x512
 * @param modelPath
 * @return
 */
bool FeatureTensor::init(const std::string &modelPath) {
    LOGI("try feature rknn_init!");
    const char *mp = modelPath.c_str();
    // Load model
    FILE *fp = fopen(mp, "rb");
    if (fp == NULL) {
        LOGE("feature fopen %s fail!\n", mp);
        return false;
    }
    fseek(fp, 0, SEEK_END);
    int model_len = ftell(fp);
    void *model = malloc(model_len);
    fseek(fp, 0, SEEK_SET);
    if (model_len != fread(model, 1, model_len, fp)) {
        LOGE("fread %s fail!\n", mp);
        free(model);
        fclose(fp);
        return false;
    }

    fclose(fp);

    // RKNN_FLAG_ASYNC_MASK: enable async mode to use NPU efficiently.
    int ret = rknn_init(&feature_ctx, model, model_len,
                        RKNN_FLAG_PRIOR_MEDIUM | RKNN_FLAG_ASYNC_MASK,
                        NULL);
    free(model);

    if (ret < 0) {
        LOGE("feature rknn_init fail! ret=%d\n", ret);
        return false;
    }

    ret = rknn_query(feature_ctx, RKNN_QUERY_IN_OUT_NUM, &feature_io_num, sizeof(feature_io_num));
    if (ret < 0) {
        LOGI("feature rknn_init error ret=%d\n", ret);
        return false;
    }
    LOGI("feature model input num: %d, output num: %d\n", feature_io_num.n_input,
         feature_io_num.n_output);

    rknn_tensor_attr input_attrs[feature_io_num.n_input];
    memset(input_attrs, 0, sizeof(input_attrs));
    for (int i = 0; i < feature_io_num.n_input; i++) {
        input_attrs[i].index = i;
        ret = rknn_query(feature_ctx, RKNN_QUERY_INPUT_ATTR, &(input_attrs[i]),
                         sizeof(rknn_tensor_attr));
        if (ret < 0) {
            LOGI("feature rknn_init error ret=%d\n", ret);
            return false;
        }
    }

    feature_output_attrs = new rknn_tensor_attr[feature_io_num.n_output];
    for (int i = 0; i < feature_io_num.n_output; i++) {
        feature_output_attrs[i].index = i;
        ret = rknn_query(feature_ctx, RKNN_QUERY_OUTPUT_ATTR, &(feature_output_attrs[i]),
                         sizeof(rknn_tensor_attr));
        if (ret < 0) {
            LOGI("feature rknn_init error ret=%d\n", ret);
            return false;
        }
    }
    LOGI("feature rknn_init success!");
    return true;
}

void FeatureTensor::get_detections(DETECTBOX box, float confidence, DETECTIONS &d) {
    DETECTION_ROW tmpRow;
    tmpRow.tlwh = box;//DETECTBOX(x, y, w, h);
    tmpRow.confidence = confidence;
    d.push_back(tmpRow);
}

void FeatureTensor::preprocess(cv::Mat &imageBGR, std::vector<float> &inputTensorValues,
                               size_t &inputTensorSize) {

    // pre-processing the Image
    //  step 1: Read an image in HWC BGR UINT8 format.
    //  cv::Mat imageBGR = cv::imread(imageFilepath, cv::ImreadModes::IMREAD_COLOR);

    // step 2: Resize the image.
    cv::Mat resizedImageBGR, resizedImageRGB, resizedImage, preprocessedImage;
    //LOGI("freature preprocess  resize %d, %d ", width_, height_);
    cv::resize(imageBGR, resizedImageBGR,
               cv::Size(width_, height_),
               cv::InterpolationFlags::INTER_CUBIC);

    // cv::resize(imageBGR, resizedImageBGR,
    //            cv::Size(64, 128));

    // step 3: Convert the image to HWC RGB UINT8 format.
    cv::cvtColor(resizedImageBGR, resizedImageRGB,
                 cv::ColorConversionCodes::COLOR_BGR2RGB);

    // step 4: Convert the image to HWC RGB float format by dividing each pixel by 255.
    resizedImageRGB.convertTo(resizedImage, CV_8U, 1.0 / 255);

    // step 5: Split the RGB channels from the image.
    cv::Mat channels[3];
    cv::split(resizedImage, channels);

    // step 6: Normalize each channel.
    //  Normalization per channel
    //  Normalization parameters obtained from your custom model

    channels[0] = (channels[0] - 0.485) / 0.229;
    channels[1] = (channels[1] - 0.456) / 0.224;
    channels[2] = (channels[2] - 0.406) / 0.225;

    // step 7: Merge the RGB channels back to the image.
    cv::merge(channels, 3, resizedImage);

    // step 8: Convert the image to CHW RGB float format.
    // HWC to CHW
//    cv::dnn::blobFromImage(resizedImage, preprocessedImage);
//    inputTensorSize = vectorProduct(inputDims_);
//    inputTensorValues.assign(preprocessedImage.begin<float>(),
//                             preprocessedImage.end<float>());
}

bool FeatureTensor::getRectsFeature( cv::Mat &img, DETECTIONS &d) {
    for (DETECTION_ROW &dbox: d) {
//        cv::Rect rc = cv::Rect(int(dbox.tlwh(0)), int(dbox.tlwh(1)),
//                               int(dbox.tlwh(2)), int(dbox.tlwh(3)));
//        rc.x -= (rc.height * 0.5 - rc.width) * 0.5;
//        rc.width = rc.height * 0.5;
//        rc.x = (rc.x >= 0 ? rc.x : 0);
//        rc.y = (rc.y >= 0 ? rc.y : 0);
//        rc.width = (rc.x + rc.width <= img.cols ? rc.width : (img.cols - rc.x));
//        rc.height = (rc.y + rc.height <= img.rows ? rc.height : (img.rows - rc.y));

//        cv::Mat mattmp = img.clone();
        //   auto t = GetCurrentTime();

//        cv::Mat resizedImage;
//
//        cv::resize(img, resizedImage, cv::Size(width_, height_));

        std::vector<float> inputTensorValues;
        size_t inputTensorSize;
        preprocess(img, inputTensorValues, inputTensorSize);
        //LOGI("feature preprocess  finish");
        rknn_input inputs[1];
        memset(inputs, 0, sizeof(inputs));
        inputs[0].index = 0;
        inputs[0].buf = img.data;
        inputs[0].type = RKNN_TENSOR_UINT8;
        inputs[0].size = width_ * height_ * 3;
        inputs[0].fmt = RKNN_TENSOR_NHWC;
        inputs[0].pass_through = 0;
        //阻塞状态
        int ret = rknn_inputs_set(feature_ctx, feature_io_num.n_input, inputs);

        if (ret < 0) {
            LOGE("feature rknn_input_set fail! ret=%d\n", ret);
        }
        rknn_output outputs[feature_io_num.n_output];
        memset(outputs, 0, sizeof(outputs));
        for (int i = 0; i < feature_io_num.n_output; i++) {
            outputs[i].want_float = 0;
        }

        ret = rknn_run(feature_ctx, nullptr);

        if (ret < 0) {
            LOGE("feature rknn_run fail! ret=%d\n", ret);
        }

        ret = rknn_outputs_get(feature_ctx, feature_io_num.n_output, outputs, nullptr);

        if (ret < 0) {
            LOGE("feature rknn_outputs_get fail! ret=%d\n", ret);
        }

        int8_t *output = (int8_t *) outputs[0].buf;

        for (int i = 0; i < 512; i++) //sisyphus
        {
            dbox.feature[i] = output[i];
        }
        rknn_outputs_release(feature_ctx, feature_io_num.n_output, outputs);
//        auto    reidTime = GetElapsedTime(t);
//        LOGD("Feature reidTime costs %f ms", reidTime);
    }

    return true;
}

void FeatureTensor::tobuffer(const std::vector<cv::Mat> &imgs, uint8 *buf) {
    int pos = 0;
    for (const cv::Mat &img: imgs) {
        int Lenth = img.rows * img.cols * 3;
        int nr = img.rows;
        int nc = img.cols;
        if (img.isContinuous()) {
            nr = 1;
            nc = Lenth;
        }
        for (int i = 0; i < nr; i++) {
            const uchar *inData = img.ptr<uchar>(i);
            for (int j = 0; j < nc; j++) {
                buf[pos] = *inData++;
                pos++;
            }
        } // end for
    }     // end imgs;
}

void FeatureTensor::test() {
    return;
}

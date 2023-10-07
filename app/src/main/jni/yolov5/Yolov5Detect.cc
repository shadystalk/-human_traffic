// Copyright (c) 2019 PaddlePaddle Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include "Yolov5Detect.h"
#include <algorithm>
#include <map>
#include <utility>
#include <opencv2/imgproc.hpp>
#include<map>

#include "opencv2/core/core.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"


#include "postprocess.h"

#include "Utils.h"
#include "../direct_texture.h"

namespace Yolov5Detector {


    rknn_context yolov5_ctx;

    bool yolov5_created = false;

    int yolov5_width = 0;
    int yolov5_height = 0;
    int yolov5_channel = 3;

    rknn_tensor_attr *yolov5_output_attrs;
    rknn_input_output_num yolov5_io_num;
    const float yolov5_nms_threshold = YOLOV5_NMS_THRESH;
    const float yolov5_box_conf_threshold = YOLOV5_BOX_THRESH;
    char *YOLOV5_labels[YOLOV5_OBJ_CLASS_NUM];

    tracker tracker(0.2, 100);

    int img_width;
    int img_height;


    std::queue<CacheMat> picture_cache;

    bool is_capture = false;

    int loadLabelName(const char *locationFilename, char *label[]) {
        LOGI("loadLabelName %s\n", locationFilename);
        readLines(locationFilename, label, YOLOV5_OBJ_CLASS_NUM);
        return 0;
    }


    void dump_tensor_attr(rknn_tensor_attr *attr) {
        LOGI("  index=%d, name=%s, n_dims=%d, dims=[%d, %d, %d, %d], n_elems=%d, size=%d, fmt=%s, type=%s, qnt_type=%s, "
             "zp=%d, scale=%f\n",
             attr->index, attr->name, attr->n_dims, attr->dims[0], attr->dims[1], attr->dims[2],
             attr->dims[3],
             attr->n_elems, attr->size, get_format_string(attr->fmt), get_type_string(attr->type),
             get_qnt_type_string(attr->qnt_type), attr->zp, attr->scale);
    }


    void
    yolov5create(int width, int height, const std::string &modelPath, const std::string &labelPath,
                 const std::string &reIdModelPath) {
        img_width = width;
        img_height = height;
        LOGI("try rknn_init!");
        const char *mp = modelPath.c_str();
        // Load model
        FILE *fp = fopen(mp, "rb");
        if (fp == NULL) {
            LOGE("fopen %s fail!\n", mp);
            return;
        }
        fseek(fp, 0, SEEK_END);
        int model_len = ftell(fp);
        void *model = malloc(model_len);
        fseek(fp, 0, SEEK_SET);
        if (model_len != fread(model, 1, model_len, fp)) {
            LOGE("fread %s fail!\n", mp);
            free(model);
            fclose(fp);
            return;
        }

        fclose(fp);

        // RKNN_FLAG_ASYNC_MASK: enable async mode to use NPU efficiently.
        int ret = rknn_init(&yolov5_ctx, model, model_len,
                            RKNN_FLAG_PRIOR_MEDIUM | RKNN_FLAG_ASYNC_MASK,
                            NULL);
        free(model);

        if (ret < 0) {
            LOGE("rknn_init fail! ret=%d\n", ret);
            return;
        }

        ret = rknn_query(yolov5_ctx, RKNN_QUERY_IN_OUT_NUM, &yolov5_io_num, sizeof(yolov5_io_num));
        if (ret < 0) {
            LOGI("rknn_init error ret=%d\n", ret);
            return;
        }
        LOGI("model input num: %d, output num: %d\n", yolov5_io_num.n_input,
             yolov5_io_num.n_output);
        rknn_tensor_attr input_attrs[yolov5_io_num.n_input];
        memset(input_attrs, 0, sizeof(input_attrs));
        for (int i = 0; i < yolov5_io_num.n_input; i++) {
            input_attrs[i].index = i;
            ret = rknn_query(yolov5_ctx, RKNN_QUERY_INPUT_ATTR, &(input_attrs[i]),
                             sizeof(rknn_tensor_attr));
            if (ret < 0) {
                LOGI("rknn_init error ret=%d\n", ret);
                return;
            }
            dump_tensor_attr(&(input_attrs[i]));
        }
        if (input_attrs[0].fmt == RKNN_TENSOR_NCHW) {
            LOGI("model is NCHW input fmt\n");
            yolov5_channel = input_attrs[0].dims[1];
            yolov5_width = input_attrs[0].dims[2];
            yolov5_height = input_attrs[0].dims[3];
        } else {
            LOGI("model is NHWC input fmt\n");
            yolov5_width = input_attrs[0].dims[1];
            yolov5_height = input_attrs[0].dims[2];
            yolov5_channel = input_attrs[0].dims[3];
        }
        yolov5_output_attrs = new rknn_tensor_attr[yolov5_io_num.n_output];
        for (int i = 0; i < yolov5_io_num.n_output; i++) {
            yolov5_output_attrs[i].index = i;
            ret = rknn_query(yolov5_ctx, RKNN_QUERY_OUTPUT_ATTR, &(yolov5_output_attrs[i]),
                             sizeof(rknn_tensor_attr));
            dump_tensor_attr(&(yolov5_output_attrs[i]));
        }

        loadLabelName(labelPath.c_str(), YOLOV5_labels);
        yolov5_created = true;
        LOGI("rknn_init success!");
        //初始化reId
        FeatureTensor::getInstance()->init(reIdModelPath);
    }


    detect_result_group_t yolov5Predict(int texId) {

        detect_result_group_t detect_result_group;
        detect_result_group.count = 0;

        double elapsedTime = 0;
        //Predict

        if (!yolov5_created) {
            //LOGE("run_yolov5: create hasn't successful!");
            return detect_result_group;
        }

        char *inData = gDirectTexture.requireBufferByTexId(texId);

        if (inData == nullptr) {
            LOGE("run_yolov5: invalid texture, id=%d!", texId);
            return detect_result_group;
        }
        auto t = GetCurrentTime();
        cv::Mat rgb(yolov5_width, yolov5_height, CV_8UC1, (void *) inData);

        cv::Mat rgb_copy = rgb.clone();


        cv::Mat temp_mat(img_height, img_width, CV_8UC3, (void *) inData);

        //需要拷贝
        cv::Mat temp_mat_copy = temp_mat.clone();

        // elapsedTime = GetElapsedTime(t);
        //LOGD("clone mat costs %f ms", elapsedTime);
        rknn_input inputs[1];
        memset(inputs, 0, sizeof(inputs));
        inputs[0].index = 0;
        inputs[0].buf = inData;
        inputs[0].type = RKNN_TENSOR_UINT8;
        inputs[0].size = yolov5_width * yolov5_height * yolov5_channel;
        inputs[0].fmt = RKNN_TENSOR_NHWC;
        inputs[0].pass_through = 0;

        int ret = rknn_inputs_set(yolov5_ctx, yolov5_io_num.n_input, inputs);

        gDirectTexture.releaseBufferByTexId(texId);

        if (ret < 0) {
            LOGE("rknn_input_set fail! ret=%d\n", ret);
            return detect_result_group;
        }

        rknn_output outputs[yolov5_io_num.n_output];
        memset(outputs, 0, sizeof(outputs));
        for (int i = 0; i < yolov5_io_num.n_output; i++) {
            outputs[i].want_float = 0;
        }

        ret = rknn_run(yolov5_ctx, nullptr);

        if (ret < 0) {
            LOGE("rknn_run fail! ret=%d\n", ret);
            return detect_result_group;
        }
        ret = rknn_outputs_get(yolov5_ctx, yolov5_io_num.n_output, outputs, nullptr);

        if (ret < 0) {
            LOGE("rknn_outputs_get fail! ret=%d\n", ret);
            return detect_result_group;
        }
        //post process width: 729,height 972
        float scale_w = (float) yolov5_width / img_width;
        float scale_h = (float) yolov5_height / img_height;
//        float scale_w = (float) yolov5_width / 640;
//        float scale_h = (float) yolov5_height / 480;

        std::vector<float> out_scales;
        std::vector<int32_t> out_zps;

        for (int i = 0; i < yolov5_io_num.n_output; ++i) {
            out_scales.push_back(yolov5_output_attrs[i].scale);
            out_zps.push_back(yolov5_output_attrs[i].zp);
        }


        post_process((int8_t *) outputs[0].buf, (int8_t *) outputs[1].buf,
                     (int8_t *) outputs[2].buf,
                     yolov5_height, yolov5_width,
                     yolov5_box_conf_threshold, yolov5_nms_threshold, scale_w, scale_h, out_zps,
                     out_scales,
                     &detect_result_group, YOLOV5_labels);

        rknn_outputs_release(yolov5_ctx, yolov5_io_num.n_output, outputs);
        //predictTime = GetElapsedTime(t);
        //LOGD("Detector predictTime costs %f ms", predictTime);
//        t = GetCurrentTime();
//        double reidTime = 0;

        //是否存在高评分
        float mat_max_prob = 0;
        bool is_exist_best_mat = false;

        if (detect_result_group.count > 0) {
            DETECTIONS detections;
            for (int i = 0; i < detect_result_group.count; ++i) {
                if (detect_result_group.results[i].prop > mat_max_prob) {
                    mat_max_prob = detect_result_group.results[i].prop;
                }
                int left = detect_result_group.results[i].box.left;
                int right = detect_result_group.results[i].box.right;
                int top = detect_result_group.results[i].box.top;
                int bottom = detect_result_group.results[i].box.bottom;
                int width = right - left;
                int height = bottom - top;
                //人在画面中
                if (top > 60 && bottom < img_height - 60 && left > 120 &&
                    right < img_width - 120 && detect_result_group.results[i].prop > 0.5) {
                    is_exist_best_mat = true;
                }
                //人在画面中
//                if (detect_result_group.results[i].prop > 0.5) {
//                    is_exist_best_mat = true;
//                }
                //放入特征框
                FeatureTensor::getInstance()->get_detections(
                        DETECTBOX(left, top, width, height),
                        detect_result_group.results[i].prop,
                        detections);

            }
            detect_result_group.count = 0;
            //获取特征值
            bool flag = FeatureTensor::getInstance()->getRectsFeature(rgb_copy, detections);
            //LOGE("FeatureTensor  getRectsFeature  flag=%d\n",  flag);
            //目标跟踪
            if (flag) {
                tracker.predict();
                tracker.update(detections);
                for (Track &track: tracker.tracks) {
                    if (!track.is_confirmed() || track.time_since_update > 1)
                        continue;
                    detect_result_group.results[detect_result_group.count].trackId = track.track_id;
                    detect_result_group.results[detect_result_group.count].box.left = track.to_tlwh()[0];
                    detect_result_group.results[detect_result_group.count].box.top = track.to_tlwh()[1];
                    detect_result_group.results[detect_result_group.count].box.right =
                            track.to_tlwh()[0] + track.to_tlwh()[2];
                    detect_result_group.results[detect_result_group.count].box.bottom =
                            track.to_tlwh()[1] + track.to_tlwh()[3];
                    detect_result_group.count++;
                }
            }

            if (detect_result_group.count > 0 && !is_capture && is_exist_best_mat) {
                //只保留最近的15张居中，评分高的图片
                if (picture_cache.size() >= 15) {
                    picture_cache.pop();
                }
                CacheMat cacheMat;
                cacheMat.time = GetCurrentTime();
                cacheMat.mat = temp_mat_copy;
                cacheMat.max_prob = mat_max_prob;
                picture_cache.push(cacheMat);
                LOGE("FeatureTensor  cacheMat push");

            }
        }

//        reidTime = GetElapsedTime(t);
//        LOGD("Feature reidTime costs %f ms", reidTime);
        return detect_result_group;
    }

    CacheMat lock_capture() {
        //防止线程不安全，所以在抓拍时，不会在队列里面放入图片
        is_capture = true;
        CacheMat init_mat;
        init_mat.max_prob = 0;
        CacheMat *best_mat = &init_mat;
        while (!picture_cache.empty()) {
            //取出队列第一个放进去的元素
            CacheMat next_cache_mat = picture_cache.front();
            //获取该图片的时间差
            double c = GetElapsedTime(next_cache_mat.time);
            //LOGD("Yolov5Detector::CacheMat  next_cache_mat c:  %f", c);
            //LOGD("Yolov5Detector::CacheMat  next_cache_mat mat_prob:  %f", next_cache_mat.max_prob);
            //如果该图片的置信度比栈里面的高，并且是5秒以内的图片
            if (next_cache_mat.max_prob > best_mat->max_prob && c < 5000) {
                *best_mat = next_cache_mat;
            }
            //队列移除这个元素
            picture_cache.pop();
        }
        LOGD("Yolov5Detector::CacheMat  best_mat mat_prob:  %f", best_mat->max_prob);
        return *best_mat;
    }

    void unlock_capture() {
        is_capture = false;
    }

    void yolov5Destory() {
        rknn_destroy(yolov5_ctx);
    }
}
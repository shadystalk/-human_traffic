#ifndef _RKNN_ZERO_COPY_DEMO_POSTPROCESS_H_
#define _RKNN_ZERO_COPY_DEMO_POSTPROCESS_H_

#include <stdint.h>

#define YOLOV5_OBJ_NAME_MAX_SIZE 16
#define YOLOV5_OBJ_NUMB_MAX_SIZE 64
#define YOLOV5_OBJ_CLASS_NUM     80
#define YOLOV5_PROP_BOX_SIZE     (5+YOLOV5_OBJ_CLASS_NUM)
//#define YOLOV5_NMS_THRESH        0.6
//#define YOLOV5_BOX_THRESH        0.5
#define YOLOV5_NMS_THRESH       0.45
#define YOLOV5_BOX_THRESH        0.25
#define YOLOV5_CONF_THRESHOLD    0.15

typedef struct _BOX_RECT {
    int left;
    int right;
    int top;
    int bottom;
} BOX_RECT;

typedef struct __detect_result_t {
    int trackId;
    //char name[YOLOV5_OBJ_NAME_MAX_SIZE];
    BOX_RECT box;
    float prop;
} detect_result_t;

typedef struct _detect_result_group_t {
    int id;
    int count;
    detect_result_t results[YOLOV5_OBJ_NUMB_MAX_SIZE];
} detect_result_group_t;

int post_process(int8_t *input0, int8_t *input1, int8_t *input2, int model_in_h, int model_in_w,
                 float conf_threshold, float nms_threshold, float scale_w, float scale_h,
                 std::vector<int32_t> &qnt_zps, std::vector<float> &qnt_scales,
                 detect_result_group_t *group, char *yolov5_lables[]);


void deinitPostProcess();


#endif //_RKNN_ZERO_COPY_DEMO_POSTPROCESS_H_

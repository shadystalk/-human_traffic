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

#pragma once

#include "../../../../../../../../../../../../../../FY/AndroidSdk/ndk/21.0.6113669/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/include/android/log.h" // NOLINT
#include "../../../../../../../../../../../../../../FY/AndroidSdk/ndk/21.0.6113669/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/include/c++/v1/fstream"       // NOLINT
#include "../../../../../../../../../../../../../../FY/AndroidSdk/ndk/21.0.6113669/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/include/c++/v1/string"        // NOLINT
#include "../../../../../../../../../../../../../../FY/AndroidSdk/ndk/21.0.6113669/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/include/c++/v1/vector"        // NOLINT

#define TAG "JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, TAG, __VA_ARGS__)
#ifndef MIN
#define MIN(a, b) (((a) < (b)) ? (a) : (b))
#endif
#ifndef MAX
#  define MAX(a,b)  ((a) < (b) ? (b) : (a))
#endif

int64_t ShapeProduction(const std::vector<int64_t> &shape);

template <typename T>
bool ReadFile(const std::string &path, std::vector<T> *data) {
  std::ifstream file(path, std::ifstream::binary);
  if (file) {
    file.seekg(0, file.end);
    int size = file.tellg();
    LOGD("file size=%lld\n", size);
    data->resize(size / sizeof(T));
    file.seekg(0, file.beg);
    file.read(reinterpret_cast<char *>(data->data()), size);
    file.close();
    return true;
  } else {
    LOGE("Can't read file from %s\n", path.c_str());
  }
  return false;
}

template <typename T>
bool WriteFile(const std::string &path, const std::vector<T> &data) {
  std::ofstream file{path, std::ios::binary};
  if (!file.is_open()) {
    LOGE("Can't write file to %s\n", path.c_str());
    return false;
  }
  file.write(reinterpret_cast<const char *>(data.data()),
             data.size() * sizeof(T));
  file.close();
  return true;
}

inline int64_t GetCurrentTime() {
  struct timeval time;
  gettimeofday(&time, NULL);
  return 1000000LL * (int64_t)time.tv_sec + (int64_t)time.tv_usec;
}

//inline long getCurrentTime()
//{
//  std::chrono::milliseconds ms = std::chrono::duration_cast< std::chrono::milliseconds >(
//          std::chrono::system_clock::now().time_since_epoch()
//  );
//  return ms.count() ;
//}

inline double GetElapsedTime(int64_t time) {
  return (GetCurrentTime() - time) / 1000.0f;
}


//void NHWC3ToNC3HW(const float *src, float *dst, int size);

void NHWC3ToNC3HW(const float *src, float *dst, const float *mean,
                  const float *std, int width, int height);

void NHWC1ToNC1HW(const float *src, float *dst, const float *mean,
                  const float *std, int width, int height);

unsigned char *load_data(FILE *fp, size_t ofst, size_t sz);

unsigned char *load_model(const std::string &filename, int *model_size);


char *readLine(FILE *fp, char *buffer, int *len);

int readLines(const char *fileName, char *lines[], int max_line);

void* my_memcpy(void* dst,const void* src,int n);
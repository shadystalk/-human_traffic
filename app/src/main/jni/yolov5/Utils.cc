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

#include "Utils.h"
#include <arm_neon.h>

int64_t ShapeProduction(const std::vector<int64_t> &shape) {
  int64_t res = 1;
  for (auto i : shape)
    res *= i;
  return res;
}

//void NHWC3ToNC3HW(const float *src, float *dst, int size) {
//  float *dst_c0 = dst;
//  float *dst_c1 = dst + size;
//  float *dst_c2 = dst + size * 2;
//  int i = 0;
//  for (; i < size - 3; i += 4) {
//    float32x4x3_t vin3 = vld3q_f32(src);
//    vst1q_f32(dst_c0, vin3.val[0]);
//    vst1q_f32(dst_c1, vin3.val[1]);
//    vst1q_f32(dst_c2, vin3.val[2]);
//    src += 12;
//    dst_c0 += 4;
//    dst_c1 += 4;
//    dst_c2 += 4;
//  }
//  for (; i < size; i++) {
//    *(dst_c0++) = *(src++);
//    *(dst_c1++) = *(src++);
//    *(dst_c2++) = *(src++);
//  }
//}

void NHWC3ToNC3HW(const float *src, float *dst, const float *mean,
                  const float *std, int width, int height) {
  int size = height * width;
  float32x4_t vmean0 = vdupq_n_f32(mean ? mean[0] : 0.0f);
  float32x4_t vmean1 = vdupq_n_f32(mean ? mean[1] : 0.0f);
  float32x4_t vmean2 = vdupq_n_f32(mean ? mean[2] : 0.0f);
  float32x4_t vscale0 = vdupq_n_f32(std ? (1.0f / std[0]) : 1.0f);
  float32x4_t vscale1 = vdupq_n_f32(std ? (1.0f / std[1]) : 1.0f);
  float32x4_t vscale2 = vdupq_n_f32(std ? (1.0f / std[2]) : 1.0f);
  float *dst_c0 = dst;
  float *dst_c1 = dst + size;
  float *dst_c2 = dst + size * 2;
  int i = 0;
  for (; i < size - 3; i += 4) {
    float32x4x3_t vin3 = vld3q_f32(src);
    float32x4_t vsub0 = vsubq_f32(vin3.val[0], vmean0);
    float32x4_t vsub1 = vsubq_f32(vin3.val[1], vmean1);
    float32x4_t vsub2 = vsubq_f32(vin3.val[2], vmean2);
    float32x4_t vs0 = vmulq_f32(vsub0, vscale0);
    float32x4_t vs1 = vmulq_f32(vsub1, vscale1);
    float32x4_t vs2 = vmulq_f32(vsub2, vscale2);
    vst1q_f32(dst_c0, vs0);
    vst1q_f32(dst_c1, vs1);
    vst1q_f32(dst_c2, vs2);
    src += 12;
    dst_c0 += 4;
    dst_c1 += 4;
    dst_c2 += 4;
  }
  for (; i < size; i++) {
    *(dst_c0++) = (*(src++) - mean[0]) / std[0];
    *(dst_c1++) = (*(src++) - mean[1]) / std[1];
    *(dst_c2++) = (*(src++) - mean[2]) / std[2];
  }
}

void NHWC1ToNC1HW(const float *src, float *dst, const float *mean,
                  const float *std, int width, int height) {
  int size = height * width;
  float32x4_t vmean = vdupq_n_f32(mean ? mean[0] : 0.0f);
  float32x4_t vscale = vdupq_n_f32(std ? (1.0f / std[0]) : 1.0f);
  int i = 0;
  for (; i < size - 3; i += 4) {
    float32x4_t vin = vld1q_f32(src);
    float32x4_t vsub = vsubq_f32(vin, vmean);
    float32x4_t vs = vmulq_f32(vsub, vscale);
    vst1q_f32(dst, vs);
    src += 4;
    dst += 4;
  }
  for (; i < size; i++) {
    *(dst++) = (*(src++) - mean[0]) / std[0];
  }
}


unsigned char *load_data(FILE *fp, size_t ofst, size_t sz) {
  unsigned char *data;
  int ret;

  data = NULL;

  if (NULL == fp) {
    return NULL;
  }

  ret = fseek(fp, ofst, SEEK_SET);
  if (ret != 0) {
    printf("blob seek failure.\n");
    return NULL;
  }

  data = (unsigned char *) malloc(sz);
  if (data == NULL) {
    printf("buffer malloc failure.\n");
    return NULL;
  }
  ret = fread(data, 1, sz, fp);
  return data;
}

unsigned char *load_model(const std::string &filename, int *model_size) {

  FILE *fp;
  unsigned char *data;
  const char *f = filename.c_str();
  fp = fopen(f, "rb");
  if (NULL == fp) {
    printf("Open file %s failed.\n", f);
    return NULL;
  }

  fseek(fp, 0, SEEK_END);
  int size = ftell(fp);

  data = load_data(fp, 0, size);

  fclose(fp);

  *model_size = size;
  return data;
}

char *readLine(FILE *fp, char *buffer, int *len)
{
  int ch;
  int i = 0;
  size_t buff_len = 0;

  buffer = (char *)malloc(buff_len + 1);
  if (!buffer)
    return NULL; // Out of memory

  while ((ch = fgetc(fp)) != '\n' && ch != EOF)
  {
    buff_len++;
    void *tmp = realloc(buffer, buff_len + 1);
    if (tmp == NULL)
    {
      free(buffer);
      return NULL; // Out of memory
    }
    buffer = (char *)tmp;

    buffer[i] = (char)ch;
    i++;
  }
  buffer[i] = '\0';

  *len = buff_len;

  // Detect end
  if (ch == EOF && (i == 0 || ferror(fp)))
  {
    free(buffer);
    return NULL;
  }
  return buffer;
}


int readLines(const char *fileName, char *lines[], int max_line)
{
  FILE *file = fopen(fileName, "r");
  char *s;
  int i = 0;
  int n = 0;

  if (file == NULL) {
    printf("Open %s fail!\n", fileName);
    return -1;
  }

  while ((s = readLine(file, s, &n)) != NULL)
  {
    lines[i++] = s;
    if (i >= max_line)
      break;
  }
  fclose(file);
  return i;
}


//按照字节(Byte)拷贝实现的my_memcpy
void* my_memcpy(void* dst,const void* src,int n)
{
  if (dst == NULL || src == NULL || n <= 0) {
    return NULL;//void* 一定要有返回值 void可以没有返回值
  }
  char* pdst = (char *)dst;
  char* psrc = (char *)src;
  if (psrc < pdst && pdst < psrc + n) {   //关键！如果出现内存覆盖的情况，就要从后向前copy
    pdst = pdst + n - 1;
    psrc = psrc + n - 1;
    while (n--) {
      *pdst = *psrc;
      pdst--;  psrc--;
    }
  }
  else {
    while (n--) {
      *pdst = *psrc;
      pdst++; psrc++;
    }
  }
  return dst;
}





# 人流量计数
  加入了行人重识别网络，更好的提供跟踪效果，可以直接运行在rk3568的板子上。

# 目标检测
1. 自己大约做了5000张图片，安装录像设备，抓拍公司人员信息，然后标注，训练，花了1-2个月，在公司环境下，目标检测效果很好。换一个环境，比如说地铁，工厂发现效果很不行。得出结论：图片中行人的角度和背景单一，标注不精确，影响模型训练。
2. 直接用训练好的模型，比如[RK356X/RK3588 RKNN SDK](https://www.t-firefly.com/doc/download/103.html)
# 目标跟踪

1. ByteTrack算法，要求FPS达到30以上，但是实际运行只能达到17FS，就算能达到，trackID变化的频率还是较快，不采用；
2. 使用[Torchreid](https://kaiyangzhou.github.io/deep-person-reid/index.html)行人重识别网络,提取运动特征；[DeepSort](https://github.com/shaoshengsong/DeepSORT)算法 根据运动特征和位置，大幅度提高了跟踪效果。

# 代码解析

osnet_x0_25_imagenet从[Torchreid](https://kaiyangzhou.github.io/deep-person-reid/index.html)下载，然后模型转换成RKNN，参考模型转换的文章；
yolov5s_relu_tk2_RK356X_i8使用的是[RK356X/RK3588 RKNN SDK](https://www.t-firefly.com/doc/download/103.html)



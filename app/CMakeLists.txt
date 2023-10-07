# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

project(rkssd4j)
# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(Eigen3_DIR "${CMAKE_CURRENT_SOURCE_DIR}/3rdparty/Eigen/share/eigen3/cmake")
find_package(Eigen3 REQUIRED)
include_directories(${EIGEN3_INCLUDE_DIRS})


# opencv
set(OpenCV_DIR "${CMAKE_CURRENT_SOURCE_DIR}/3rdparty/opencv/sdk/native/jni")
find_package(OpenCV REQUIRED)
message(STATUS "OpenCV libraries: ${OpenCV_LIBS}")
include_directories(${OpenCV_INCLUDE_DIRS})


set(jni_srcs
        src/main/jni/native_c_api.cc
        src/main/jni/direct_texture.cc

        src/main/jni/yolov5/Yolov5Detect.cc
        src/main/jni/yolov5/postprocess.cc
        src/main/jni/yolov5/Utils.cc

        src/main/jni/object_tracker/objects_tracker.cc
        src/main/jni/object_tracker/objects_update.cc
        src/main/jni/object_tracker/track_link.cc

        src/main/jni/deepsort/FeatureTensor.cpp
        src/main/jni/deepsort/hungarianoper.cpp
        src/main/jni/deepsort/kalmanfilter.cpp
        src/main/jni/deepsort/linear_assignment.cpp
        src/main/jni/deepsort/model.cpp
        src/main/jni/deepsort/munkres.cpp
        src/main/jni/deepsort/nn_matching.cpp
        src/main/jni/deepsort/track.cpp
        src/main/jni/deepsort/tracker.cpp
        )

add_library(rkssd4j SHARED ${jni_srcs})


find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )

set(link_libs ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/librknnrt.so
        EGL
        GLESv2
        jnigraphics
        android
        ${log-lib})

target_link_libraries( # Specifies the target library.
        rkssd4j
        ${OpenCV_LIBS}
        ${link_libs} )
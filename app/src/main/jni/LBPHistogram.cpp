//
// Created by nick on 12/6/15.
//

#include <LBPHistogram.h>
#include <opencv2/core/core.hpp>
#include <opencv2/objdetect.hpp>
#include <opencv2/imgproc.hpp>

#include <string>
#include <vector>
#include <limits>

#include <android/log.h>

#define LOG_TAG "PPFA_Native_LBPHistogram"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

using namespace std;
using namespace cv;

JNIEXPORT void JNICALL
Java_edu_mst_nsh9b3_privacypreservingfaceauthentication_AuthenticateFace_nativeCreateLBPHistorgram
        (JNIEnv *jenv, jclass, jlong _src, jlong _dst, jint radius, jint neighbors)
{
    LOGD("PPFA_Native_LBPHistogram_CreateLBPHistorgram: start");

    Mat* src = (Mat*)_src;
    Mat* dst = (Mat*)_dst;

    dst->setTo(0);

    for(int n = 0; n < neighbors; n++)
    {
        // sample points
        float x = static_cast<float>(radius * cos(2.0*CV_PI*n/static_cast<float>(neighbors)));
        float y = static_cast<float>(-radius * sin(2.0*CV_PI*n/static_cast<float>(neighbors)));
        // relative indices
        int fx = static_cast<int>(floor(x));
        int fy = static_cast<int>(floor(y));
        int cx = static_cast<int>(ceil(x));
        int cy = static_cast<int>(ceil(y));
        // fractional part
        float ty = y - fy;
        float tx = x - fx;
        // set interpolation weights
        float w1 = (1 - tx) * (1 - ty);
        float w2 =      tx  * (1 - ty);
        float w3 = (1 - tx) *      ty;
        float w4 =      tx  *      ty;

        // iterate through your data
        for(int i=radius; i < src->rows-radius;i++) {
            for(int j=radius;j < src->cols-radius;j++) {
                // calculate interpolated value
                float t = static_cast<float>(w1*src->at<unsigned char>(i+fy,j+fx) + w2*src->at<unsigned char>(i+fy,j+cx) + w3*src->at<unsigned char>(i+cy,j+fx) + w4*src->at<unsigned char>(i+cy,j+cx));
                // floating point precision, so check some machine-dependent epsilon
                dst->at<int>(i-radius,j-radius) += ((t > src->at<unsigned char>(i,j)) || (std::abs(t-src->at<unsigned char>(i,j)) < std::numeric_limits<float>::epsilon())) << n;
            }
        }
    }

    LOGD("PPFA_Native_LBPHistogram_CreateLBPHistorgram: end");
    return;
}

JNIEXPORT void JNICALL
Java_edu_mst_nsh9b3_privacypreservingfaceauthentication_AuthenticateFace_nativeSpatialHistogram
(JNIEnv *, jclass, jlong _src, jlong _result, jint numPatterns, jint grid_x, jint grid_y)
{
    LOGD("PPFA_Native_LBPHistogram_SpatialHistogram: start");

    Mat* src = (Mat*)_src;
    Mat* result = (Mat*)_result;

    int width = src->cols/grid_x;
    int height = src->rows/grid_y;

    if(src->empty())
    {
        LOGD("PPFA_Native_LBPHistogram_SpatialHistogram: empty");
        result->reshape(1, 1);
        return;
    }

    int resultRowIdx = 0;

    for(int i = 0; i < grid_y; i++) {
        for(int j = 0; j < grid_x; j++) {
            Mat src_cell = Mat_<float>(*src, Range(i*height,(i+1)*height), Range(j*width,(j+1)*width));

            // histc function
            Mat cell_hist;

            int histSize = numPatterns - 1;
            float range[] = { static_cast<float>(0), static_cast<float>(numPatterns)};
            const float* histRange {range};
            int channels[] = {0};

            calcHist(&src_cell, 1, channels, Mat(), cell_hist, 1, &histSize, &histRange, true, false);

            cell_hist.reshape(1,1);
            //

            //copy to the result matrix
            Mat result_row = result->row(resultRowIdx);
            cell_hist.reshape(1,1).convertTo(result_row, CV_32FC1);

            //increase row count in result matrix
            resultRowIdx++;
        }
    }

    result->reshape(1, 1);

    LOGD("PPFA_Native_LBPHistogram_SpatialHistogram: end");

    return;
}

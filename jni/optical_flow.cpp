#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/videostab/optical_flow.hpp>
#include <cv.h>
#include <highgui.h>
#include <vector>
#include <android/log.h>

using namespace std;
using namespace cv;

extern "C" {
JNIEXPORT void JNICALL
Java_com_wais_opticalflow_View_opticalFlow(JNIEnv* env, jobject obj, jint width, jint height, jbyteArray frame1, jbyteArray frame2, jintArray bgra )
{
//	__android_log_write(ANDROID_LOG_ERROR, "debug", "A");
	static const int MAX_CORNERS = 40;
	vector<uchar> status(MAX_CORNERS);
	static vector<float> error(MAX_CORNERS);
	static vector<Point2f> corners(MAX_CORNERS);
	static vector<Point2f> points(MAX_CORNERS);

//	__android_log_write(ANDROID_LOG_ERROR, "debug", "B");
    jbyte* _frame1  = env->GetByteArrayElements(frame1, 0);
    jbyte* _frame2  = env->GetByteArrayElements(frame2, 0);
    jint*  _bgra = env->GetIntArrayElements(bgra, 0);


    //mbgra is only used to redisplay the image for debugging purposes, this wont' be needed later
    Mat mbgra(height, width, CV_8UC4, (unsigned char *)_bgra);

    //convert the javaArrays to Mat images
    Mat mframe1(height, width, CV_8UC1, (unsigned char *)_frame1);
    Mat mframe2(height, width, CV_8UC1, (unsigned char *)_frame2);
    Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)_frame1);

//    __android_log_write(ANDROID_LOG_ERROR, "debug", "C");
	//http://opencv.willowgarage.com/documentation/cpp/imgproc_feature_detection.html#cv-goodfeaturestotrack
    goodFeaturesToTrack(
		mframe1,
		corners,
		MAX_CORNERS,
		0.1,
		3
	);
    if (corners.size() < 5)
    	return;

//    __android_log_write(ANDROID_LOG_ERROR, "debug", "D");
    //http://opencv.willowgarage.com/documentation/cpp/motion_analysis_and_object_tracking.html#cv-calcopticalflowpyrlk
    calcOpticalFlowPyrLK(
		mframe1,
		mframe2,
		corners,
		points,
		status,
		error,
		Size(15, 15),
		0
	);


//    __android_log_write(ANDROID_LOG_ERROR, "debug", "E");
    //cvtColor is used to output image and as a drawing surface for vector lines
    cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);
	for (int i = 0; i < points.size(); i++)
    {
		if (status[i] == 1)
		{
			CvPoint p1, p2;
			p1.x = (int) points[i].x;
			p1.y = (int) points[i].y;
			p2.x = (int) corners[i].x;
			p2.y = (int) corners[i].y;

			line(mbgra, p1, p2, Scalar(0,0,255,255));
		}

    }


//    __android_log_write(ANDROID_LOG_ERROR, "debug", "F");
    env->ReleaseIntArrayElements(bgra, _bgra, 0);
    env->ReleaseByteArrayElements(frame2, _frame2, 0);
    env->ReleaseByteArrayElements(frame1, _frame1, 0);
    //__android_log_write(ANDROID_LOG_ERROR, "debug", "G");

}

}

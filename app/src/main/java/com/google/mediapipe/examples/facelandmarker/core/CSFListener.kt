package com.google.mediapipe.examples.facelandmarker.core

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import com.google.mediapipe.examples.facelandmarker.FLApplication
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.google.mediapipe.examples.facelandmarker.fragment.CameraViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.abs


class CSFListener(viewModel : CameraViewModel) : FaceLandmarkerHelper.LandmarkerListener {

    private val viewmodel = viewModel
    private var flag = 0
    var realFaceWidth = 0.0

    @OptIn(DelicateCoroutinesApi::class)
    override fun onError(error: String, errorCode: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            viewmodel.changeDistance(0.0)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("RestrictedApi")
    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        resultBundle.let { faceLandmarkerResult ->
            if (faceLandmarkerResult.result != null) {
                val landmark = faceLandmarkerResult.result.faceLandmarks()[0]

                // 눈 거리 구하기
                val dx_iris = abs(landmark[469].x() - landmark[471].x()) * 0.26458332f
                val dX_iris = 11.7

                val focalLength = CameraManagerCompat.from(FLApplication.context).unwrap()
                    .getCameraCharacteristics("1").get( // 전면카메라 왜곡 보정값을 사용하면 좀 더 결과 좋음
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                    )
                val normalizedFocaleX = focalLength!![0]
                val eye_distance = normalizedFocaleX * (dX_iris / dx_iris) / 10.0

                if(eye_distance > 299 && eye_distance <= 300 && flag == 0) {
                // 이 거리일 때, 사람의 얼굴 너비를 구함
                val faceWidthOnImage = abs(landmark[127].x() - landmark[356].x()) * 0.26458332f
                 realFaceWidth = (((eye_distance * 10.0 * faceWidthOnImage) / normalizedFocaleX) / 10.0)
                Log.d("realFaceWidth","$realFaceWidth")
                    flag = 1
                }

                // calculate distance
                if(flag == 1) {
                    val dx = abs(landmark[127].x() - landmark[356].x()) * 0.26458332f
                    val dX = realFaceWidth
                    val distance = normalizedFocaleX * (dX / dx) /// 10.0
                    //Log.d("distanceWidth","${((distance * 10.0 * dx) / normalizedFocaleX) / 10.0}")

                    GlobalScope.launch(Dispatchers.Main) {
                        viewmodel.changeDistance(distance)
                        viewmodel.changeFaceWidth(realFaceWidth)
                    }
                } else {

                }

            } else {
                //Log.d("Distance","${0.0}")
                /*GlobalScope.launch(Dispatchers.Main) {
                    viewmodel.changeDistance(0.0)
                    viewmodel.changeFaceWidth(0.0)
                }*/
            }
        }
    }
}

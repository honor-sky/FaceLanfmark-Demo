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

                // calculate distance
                val dx = abs(landmark[389].x() - landmark[162].x()) * 0.8f //0.26458332f //468 - 471// 카메라 렌즈를 통해 들어온 이미지상 눈 사이 거리
                val dX = 147 // 11.7 // 일반적인 평균 눈 사이 거리
                val focalLength = CameraManagerCompat.from(FLApplication.context).unwrap()
                    .getCameraCharacteristics("1").get(
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                    )
                val normalizedFocaleX = focalLength!![0]
                val distance = normalizedFocaleX * (dX / dx) / 10.0

              //  Log.d("Distance","$distance")

                GlobalScope.launch(Dispatchers.Main) {
                    viewmodel.changeDistance(distance)
                }
            } else {
                //Log.d("Distance","${0.0}")
                GlobalScope.launch(Dispatchers.Main) {
                    viewmodel.changeDistance(0.0)
                }
            }
        }
    }
}

package com.google.mediapipe.examples.facelandmarker.core

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Build.VERSION
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import com.google.mediapipe.examples.facelandmarker.FLApplication
import com.google.mediapipe.examples.facelandmarker.FLApplication.Companion.context
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.google.mediapipe.examples.facelandmarker.fragment.CameraViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.abs


class CSFListener(viewModel : CameraViewModel) : FaceLandmarkerHelper.LandmarkerListener {

    private val viewmodel = viewModel
    private var isGetFaceWidth = false
    var realFaceWidth = 0.0



    @OptIn(DelicateCoroutinesApi::class)
    override fun onError(error: String, errorCode: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            viewmodel.changeDistance(0.0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("RestrictedApi")
    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        resultBundle.let { faceLandmarkerResult ->
            if (faceLandmarkerResult.result != null) {

                val landmark = faceLandmarkerResult.result.faceLandmarks()[0]

                val focalLengthFront = CameraManagerCompat.from(FLApplication.context).unwrap()
                    .getCameraCharacteristics("1").get( // 전면카메라 왜곡 보정값을 사용하면 좀 더 결과 좋음
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                    )

                val focalLengthBack = CameraManagerCompat.from(FLApplication.context).unwrap()
                    .getCameraCharacteristics("0").get( // 전면카메라 왜곡 보정값을 사용하면 좀 더 결과 좋음
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                    )

                val normalizedFocaleX = 3.3 // 초점 거리 (2M 내외에서의 계산에 적용되는 값)

                /* Step1. 사용자 얼굴 가로 길이 측정 */
                val dxIris = abs(landmark[469].x() - landmark[471].x()) * 0.26458332f
                val dXIris = 11.7
                val eyeDistance = normalizedFocaleX * (dXIris / dxIris) / 10.0

                if(eyeDistance > 399 && eyeDistance <= 400 && !isGetFaceWidth) {  // 299, 300
                    val faceWidthOnImage = abs(landmark[127].x() - landmark[356].x()) * 0.26458332f
                    realFaceWidth = adjustFaceSize((((eyeDistance * 10.0 * faceWidthOnImage) / normalizedFocaleX) / 10.0), 10.85, 0.81)
                    isGetFaceWidth = true
                    GlobalScope.launch(Dispatchers.Main) {
                        viewmodel.changeFaceWidth(realFaceWidth)
                        viewmodel.changeStep("step 2. 200cm까지 이동해주세요")
                    }
                }

                /* Step2. 시력 검사 거리 측정 */
                if(isGetFaceWidth) {

                    val dx = abs(landmark[127].x() - landmark[356].x()) * 0.26458332f // 픽셀을 mm 로 보정
                    val dX = realFaceWidth
                    val distance = normalizedFocaleX * (dX / dx)

                    val model = Build.MODEL
                    GlobalScope.launch(Dispatchers.Main) {
                        if(distance < 700) {
                            viewmodel.changeDistance(distance)
                        } else {
                            when {
                                model.startsWith("SM-F711N") -> {
                                    viewmodel.changeDistance(distance - 30)
                                }
                                model.startsWith("SM-S901N") -> {
                                    viewmodel.changeDistance(distance - 200)
                                }
                                model.startsWith("SM-G986N") -> {
                                    viewmodel.changeDistance(distance - 30)
                                }
                                model.startsWith("SM-N981N") -> {
                                    viewmodel.changeDistance(distance - 30)
                                }
                                else -> {
                                    if(VERSION.SDK_INT <= 33) {
                                        viewmodel.changeDistance(distance - 30)
                                    } else {
                                        viewmodel.changeDistance(distance - 200)
                                    }
                                }
                            }
                        }
                    }

                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        viewmodel.changeDistance(eyeDistance)
                    }
                }


            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    viewmodel.changeDistance(0.0)
                    viewmodel.changeFaceWidth(0.0)
                }
            }
        }
    }

    // 얼굴 가로 길이 보정
    fun adjustFaceSize(myFaceSize: Double, mean: Double, stdDev: Double, adjustmentFactor: Double = 0.5): Double {
        val zScore = (myFaceSize - mean) / stdDev
        return mean + (zScore * adjustmentFactor * stdDev)
    }

}

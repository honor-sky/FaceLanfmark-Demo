package com.google.mediapipe.examples.facelandmarker.core

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build.VERSION
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
    private var isGetFaceWidth = false
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

                val focalLength = CameraManagerCompat.from(FLApplication.context).unwrap()
                    .getCameraCharacteristics("3").get( // 전면카메라 왜곡 보정값을 사용하면 좀 더 결과 좋음
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                    )
                val normalizedFocaleX = focalLength!![0]

                /* Step1. 사용자 얼굴 가로 길이 측정 */
                val dxIris = abs(landmark[469].x() - landmark[471].x()) * 0.26458332f
                val dXIris = 11.7
                val eyeDistance = normalizedFocaleX * (dXIris / dxIris) / 10.0

                if(eyeDistance > 299 && eyeDistance <= 300 && !isGetFaceWidth) {
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
                    Log.d("distance", "$distance")

                    GlobalScope.launch(Dispatchers.Main) {
                        if(VERSION.SDK_INT <= 33) {
                            if(distance >= 700) {
                                viewmodel.changeDistance(distance - 30)
                            } else {
                                viewmodel.changeDistance(distance)
                            }
                        } else {
                            if(distance >= 700) {
                                viewmodel.changeDistance(distance - 180)
                            } else {
                                viewmodel.changeDistance(distance)
                            }
                        }
                    }

                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        viewmodel.changeDistance(eyeDistance)
                    }
                }

                // 얼굴을 기준으로 하는 이유 -> 동공 인식 + 후면 카메라 로 하면 차이가 너무 심함 (100 이상), 조금만 고개를 돌리거나 해도 측정 이상함 ( 테스트 하기도 힘듬)
                // step1. 우선 동공 인식을 사용해 40cm 거리에서 실제 얼굴 가로 길이를 잰다 -> step2. 사용자의 실제 얼굴 길이를 사용해 얼굴을 기준으로 잡고 거리를 측정한다 -> step3. 버전별로 보정을 좀 해준다
                // S22 -> 70 부터 보정 20씩 해주기 SM-S901N 14 6.1 (10 ~ 15씩 보정) => 보정값 커지면 사람들이 좀 더 멀리서 검사함 (어떻게 할지 정해야 함)
                // S20+ 5G -> 보정  없이 가기 SM-G986N (5 정도 해주면 굳) 버전13 oneUI5.1
                // Note20 N981N -> 보전 없이 가도 됨 (5 정도 해주면 굳) 버전13 oneUI5.1
                // S23 -> 70 부터 보정 20씩 해주기 SM-S901N 14 6.1
                // 오차 범위 +- 10 으로 줄일 수는 있음
                // 70부터 10 또는 20씩 차이남 14 6.1

            /*val dx_iris = abs(landmark[469].x() - landmark[471].x()) * 0.26458332f
            val dX_iris = 11.7

            val focalLength = CameraManagerCompat.from(FLApplication.context).unwrap()
                .getCameraCharacteristics("1").get( // 전면카메라 왜곡 보정값을 사용하면 좀 더 결과 좋음
                    CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                )
            val normalizedFocaleX = focalLength!![0]
            val eye_distance = normalizedFocaleX * (dX_iris / dx_iris) / 10.0

            GlobalScope.launch(Dispatchers.Main) {
                viewmodel.changeDistance(eye_distance)
                //viewmodel.changeFaceWidth(realFaceWidth)
            }*/

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

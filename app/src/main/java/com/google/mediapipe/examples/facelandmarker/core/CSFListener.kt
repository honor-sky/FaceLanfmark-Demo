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

/*
    fun normalizePixelCoordinates(x: Float, y: Float, width: Int, height: Int): Pair<Float, Float> {
        val normX = (x / width) * 2 - 1  // [-1, 1] 범위로 변환
        val normY = (y / height) * 2 - 1
        return Pair(normX, normY)
    }

    fun applyRadialDistortion(x: Float, y: Float, lensDistortion: FloatArray): Pair<Float, Float> {
        val k1 = lensDistortion[0]
        val k2 = lensDistortion[1]
        val k3 = lensDistortion[2]

        val r = x * x + y * y
        val scaleFactor = 1 + k1 * r + k2 * r * r + k3 * r * r * r

        return Pair(x * scaleFactor, y * scaleFactor)
    }

    fun applyTangentialDistortion(x: Float, y: Float, lensDistortion: FloatArray): Pair<Float, Float> {
        val p1 = lensDistortion[3]
        val p2 = lensDistortion[4]

        val r = x * x + y * y
        val xT = x + (2 * p1 * x * y + p2 * (r + 2 * x * x))
        val yT = y + (p1 * (r + 2 * y * y) + 2 * p2 * x * y)

        return Pair(xT, yT)
    }

    fun denormalizeCoordinates(x: Float, y: Float, width: Int, height: Int): Pair<Float, Float> {
        val pixelX = ((x + 1) / 2) * width
        val pixelY = ((y + 1) / 2) * height
        return Pair(pixelX, pixelY)
    }

    fun correctLensDistortion(x: Float, y: Float, width: Int, height: Int, lensDistortion: FloatArray): Pair<Float, Float> {
        // Step 1: 픽셀 좌표 -> 정규화 좌표 변환
        val (normX, normY) = normalizePixelCoordinates(x, y, width, height)

        // Step 2: 방사 왜곡 적용
        val (radialX, radialY) = applyRadialDistortion(normX, normY, lensDistortion)

        // Step 3: 접선 왜곡 적용
        val (correctedX, correctedY) = applyTangentialDistortion(radialX, radialY, lensDistortion)

        // Step 4: 정규화 좌표 -> 픽셀 좌표 변환
        return denormalizeCoordinates(correctedX, correctedY, width, height)

    } */
}

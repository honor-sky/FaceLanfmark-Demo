package com.google.mediapipe.examples.facelandmarker.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {

    private val _eyeDistance = MutableLiveData<Double>(0.0)
    val eyeDistance : LiveData<Double> = _eyeDistance

    private val _distance = MutableLiveData<Double>(0.0)
    val distance : LiveData<Double> = _distance

    private val _width = MutableLiveData<Double>(0.0)
    val width : LiveData<Double> = _width

    private val _stepText = MutableLiveData<String>("step 1. 얼굴 크기를 잽니다.\n30cm까지 이동해주세요.")
    val stepText : LiveData<String> = _stepText

    fun changeEyeDistance(value : Double){
        _eyeDistance.value = value
    }

    fun changeDistance(value : Double){
        _distance.value = value
    }

    fun changeFaceWidth(value : Double){
        _width.value = value
    }

    fun changeStep(value : String){
        _stepText.value = value
    }
}
package com.google.mediapipe.examples.facelandmarker.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {

    private val _distance = MutableLiveData<Double>(0.0)
    val distance : LiveData<Double> = _distance

    private val _width = MutableLiveData<Double>(0.0)
    val width : LiveData<Double> = _width

    fun changeDistance(value : Double){
        _distance.value = value
    }

    fun changeFaceWidth(value : Double){
        _width.value = value
    }
}
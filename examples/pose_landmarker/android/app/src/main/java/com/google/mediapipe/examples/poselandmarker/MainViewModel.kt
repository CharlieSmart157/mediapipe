/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.poselandmarker

import android.animation.ObjectAnimator
import android.util.Range
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.roundToInt

/**
 *  This ViewModel is used to store pose landmarker helper settings
 */
class MainViewModel : ViewModel() {

    private var _model = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL
    private var _delegate: Int = PoseLandmarkerHelper.DELEGATE_CPU
    private var _minPoseDetectionConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE
    private var _minPoseTrackingConfidence: Float = PoseLandmarkerHelper
        .DEFAULT_POSE_TRACKING_CONFIDENCE
    private var _minPosePresenceConfidence: Float = PoseLandmarkerHelper
        .DEFAULT_POSE_PRESENCE_CONFIDENCE

    var savedLandmarkList: List<NormalizedLandmark>? = null
    private val targetPoses = mutableListOf<List<NormalizedLandmark>>()
    private var prevPos = -1
    private var hasCompletedRep = mutableListOf(false, false, false)

    val currentDelegate: Int get() = _delegate
    val currentModel: Int get() = _model
    val currentMinPoseDetectionConfidence: Float
        get() =
            _minPoseDetectionConfidence
    val currentMinPoseTrackingConfidence: Float
        get() =
            _minPoseTrackingConfidence
    val currentMinPosePresenceConfidence: Float
        get() =
            _minPosePresenceConfidence

    private val leftReps = MutableLiveData(0)
    val currentLeftReps: LiveData<Int>
        get() = leftReps

    private val rightReps = MutableLiveData(0)
    val currentRightReps: LiveData<Int>
        get() = rightReps

    private val completionPercentile = MutableLiveData(0)
    val currentCompletionPercentile: LiveData<Int>
        get() = completionPercentile

    fun setDelegate(delegate: Int) {
        _delegate = delegate
    }

    fun setMinPoseDetectionConfidence(confidence: Float) {
        _minPoseDetectionConfidence = confidence
    }

    fun setMinPoseTrackingConfidence(confidence: Float) {
        _minPoseTrackingConfidence = confidence
    }

    fun setMinPosePresenceConfidence(confidence: Float) {
        _minPosePresenceConfidence = confidence
    }

    fun setModel(model: Int) {
        _model = model
    }

    fun savePoseResult() {
        savedLandmarkList?.let {
            targetPoses.add(savedLandmarkList!!)
            // Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    fun comparePoseResult(subjectPose: List<NormalizedLandmark>) {
        if (targetPoses.size == 3) {
            targetPoses.forEach { targetLandmarks ->
                var confidenceRating = 0

                for (i in targetLandmarks.indices) {
                    if (Range.create(
                            subjectPose[i].x().roundToTwoPlaces() - 0.03,
                            subjectPose[i].x().roundToTwoPlaces() + 0.03
                        ).contains(targetLandmarks[i].x().roundToTwoPlaces())
                        && Range.create(
                            subjectPose[i].y().roundToTwoPlaces() - 0.03,
                            subjectPose[i].y().roundToTwoPlaces() + 0.03
                        ).contains(targetLandmarks[i].y().roundToTwoPlaces())
                    ) {
                        confidenceRating++
                    }
                }
                //todo Add criteria for specifically checking specific landmarks I.E. 23-32 for leg landmarks

                if (confidenceRating > 28) {
                    when (targetLandmarks) {
                        targetPoses[0] -> {
                            if (prevPos != 0) {
                                hasCompletedRep[0] = true //Has reset to base pose
                                prevPos = 0
                                completionPercentile.value = 0
                            }
                        }

                        targetPoses[1] -> {
                            if (prevPos == 0) {
                                hasCompletedRep[1] = true //Has performed right lunge
                                hasCompletedRep[2] = false
                                prevPos = 1
                            }
                        }

                        targetPoses[2] -> {
                            if (prevPos == 0) {
                                hasCompletedRep[2] = true //Has performed left lunge
                                hasCompletedRep[1] = false
                                prevPos = 2
                            }
                        }
                    }

                    if (hasCompletedRep[0] && hasCompletedRep[1] && prevPos == 1) {
                        rightReps.value = rightReps.value!! + 1
                        hasCompletedRep = mutableListOf(false, false, false)
                        completionPercentile.value = 100
                    } else
                        if (hasCompletedRep[0] && hasCompletedRep[2] && prevPos == 2) {
                            leftReps.value = leftReps.value!! + 1
                            hasCompletedRep = mutableListOf(false, false, false)
                            completionPercentile.value = 100
                        }
                }
            }
        }
    }

    private fun Float.roundToTwoPlaces(): Double {
        return (this * 100.0).roundToInt() / 100.0
    }
}

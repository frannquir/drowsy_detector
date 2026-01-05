package com.example.myapplication.domain.detector

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt

class EyeAspectRatioDetector {
    fun calculateEAR(landmarks: List<NormalizedLandmark>, eyeIndices: List<Int>): Float {
        val points = eyeIndices.map { Point(landmarks[it].x(), landmarks[it].y()) }

        // Vertical distances
        val vertical1 = distance(points[1], points[5])
        val vertical2 = distance(points[2], points[4])

        // Horizontal distance
        val horizontal = distance(points[0], points[3])

        return (vertical1 + vertical2) / (2.0f * horizontal)
    }

    private fun distance(p1: Point, p2: Point): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    private data class Point(val x: Float, val y: Float)
}

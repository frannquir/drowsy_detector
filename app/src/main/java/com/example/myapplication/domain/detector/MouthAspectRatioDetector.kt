package com.example.myapplication.domain.detector

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt

class MouthAspectRatioDetector {
    fun calculateMAR(landmarks: List<NormalizedLandmark>, mouthIndices: List<Int>): Float {
        val points = mouthIndices.map { Point(landmarks[it].x(), landmarks[it].y()) }

        // Vertical distances
        val vertical1 = distance(points[1], points[7])
        val vertical2 = distance(points[2], points[6])
        val vertical3 = distance(points[3], points[5])

        // Horizontal distance
        val horizontal = distance(points[0], points[4])

        return (vertical1 + vertical2 + vertical3) / (3.0f * horizontal)
    }

    private fun distance(p1: Point, p2: Point): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    private data class Point(val x: Float, val y: Float)
}

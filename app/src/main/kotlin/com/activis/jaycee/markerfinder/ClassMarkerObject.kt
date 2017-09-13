/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.activis.jaycee.markerfinder

import android.graphics.Color
import android.util.Log

import org.rajawali3d.materials.Material
import org.rajawali3d.math.Quaternion
import org.rajawali3d.math.vector.Vector3
import org.rajawali3d.primitives.Line3D
import org.rajawali3d.scene.Scene

import java.util.Stack

import com.projecttango.tangosupport.TangoSupport

/**
 * Rajawali object which represents a marker.
 */
class ClassMarkerObject
/**
 * Construct marker object from a marker.
 */
(marker: TangoSupport.Marker)
{

    // 3D object for bounding box of the marker.
    private val mRect: Line3D

    // 3D object for three axes of the marker local frame.
    private val mAxisX: Line3D
    private val mAxisY: Line3D
    private val mAxisZ: Line3D

    init
    {
        // Create marker center and four corners.
        val center = Vector3(marker.translation[0], marker.translation[1], marker.translation[2])
        val cornerBottomLeft = Vector3(marker.corners3d[0][0].toDouble(), marker.corners3d[0][1].toDouble(), marker.corners3d[0][2].toDouble())
        val cornerBottomRight = Vector3(marker.corners3d[1][0].toDouble(), marker.corners3d[1][1].toDouble(), marker.corners3d[1][2].toDouble())
        val cornerTopRight = Vector3(marker.corners3d[2][0].toDouble(), marker.corners3d[2][1].toDouble(), marker.corners3d[2][2].toDouble())
        val cornerTopLeft = Vector3(marker.corners3d[3][0].toDouble(), marker.corners3d[3][1].toDouble(), marker.corners3d[3][2].toDouble())

        // Create a quaternion from marker orientation.
        val q = Quaternion(marker.orientation[3], marker.orientation[0], marker.orientation[1], marker.orientation[2])

        // Log.d(TAG, String.format("%f %f %f", center.x, center.y, center.z))
        // Calculate marker size in meters, assuming square-shape markers.
        val markerSize = cornerTopLeft.distanceTo(cornerTopRight)

        // Line width for drawing the axes and bounding rectangle.
        val axisLineWidth = 10f
        val rectLineWidth = 5f

        val points = Stack<Vector3>()
        val material = Material()

        // X axis
        val xAxis = q.multiply(Vector3(markerSize / 3, 0.0, 0.0))
        points.add(center)
        points.add(Vector3.addAndCreate(center, xAxis))
        mAxisX = Line3D(points, axisLineWidth, Color.RED)
        mAxisX.material = material

        // Y axis
        points.clear()
        val yAxis = q.multiply(Vector3(0.0, markerSize / 3, 0.0))
        points.add(center)
        points.add(Vector3.addAndCreate(center, yAxis))
        mAxisY = Line3D(points, axisLineWidth, Color.GREEN)
        mAxisY.material = material

        // Z axis
        points.clear()
        val zAxis = q.multiply(Vector3(0.0, 0.0, markerSize / 3))
        points.add(center)
        points.add(Vector3.addAndCreate(center, zAxis))
        mAxisZ = Line3D(points, axisLineWidth, Color.BLUE)
        mAxisZ.material = material
        // Log.d(TAG, String.format("ZAxis %f %f %f", mAxisZ.getPoint(0).x, mAxisZ.getPoint(0).y, mAxisZ.getPoint(0).z))

        // Rect
        points.clear()
        points.add(cornerBottomLeft)
        points.add(cornerBottomRight)
        points.add(cornerBottomRight)
        points.add(cornerTopRight)
        points.add(cornerTopRight)
        points.add(cornerTopLeft)
        points.add(cornerTopLeft)
        points.add(cornerBottomLeft)
        mRect = Line3D(points, rectLineWidth, Color.CYAN)
        mRect.material = material
    }

    /**
     * Add all 3D objects of a marker as children of the input scene.
     */
    fun addToScene(scene: Scene)
    {
        scene.addChild(mAxisX)
        scene.addChild(mAxisY)
        scene.addChild(mAxisZ)
        scene.addChild(mRect)
    }

    /**
     * Remove all 3D objects of a marker from the input scene.
     */
    fun removeFromScene(scene: Scene)
    {
        scene.removeChild(mAxisX)
        scene.removeChild(mAxisY)
        scene.removeChild(mAxisZ)
        scene.removeChild(mRect)
    }

    companion object
    {
        private val TAG = ClassMarkerObject::class.java.simpleName
    }
}

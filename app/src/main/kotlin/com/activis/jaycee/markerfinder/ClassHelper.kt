/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import android.util.Log

import com.google.atap.tangoservice.TangoPoseData

import org.rajawali3d.math.Matrix
import org.rajawali3d.math.Matrix4
import org.rajawali3d.math.Quaternion
import org.rajawali3d.math.vector.Vector3

import java.util.ArrayList
import java.util.Random

import java.lang.Math.acos
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt

internal class ClassHelper(private val activityMain: ActivityMain)
{
    private var topLeftCounter = 0
    private var topRightCounter = 0
    private var bottomLeftCounter = 0
    private var bottomRightCounter = 0

    companion object
    {
        private val TAG = ClassHelper::class.java.simpleName
        private val NUM_POINTS_PER_QUAD = 100

        fun getQuaternionToTarget(targetPosition: mVector, tangoPoseData: TangoPoseData): mQuaternion
        {
            val tangoOrientation = mQuaternion(tangoPoseData.rotation[0], tangoPoseData.rotation[1], tangoPoseData.rotation[2], tangoPoseData.rotation[3])
            tangoOrientation.normalise()

            val tangoPosition = mVector(tangoPoseData.translation[0], tangoPoseData.translation[1], tangoPoseData.translation[2])

            val tangoForwardVector = mVector(0.0, 1.0, 0.0)
            var tangoForwardFacingVector = tangoForwardVector.rotateVector(tangoOrientation)
            tangoForwardFacingVector.normalise()

            // Rotate the vector to allign with global coord system
            val rotate = mQuaternion(mVector(0.0, 1.0, 0.0), Math.PI)
            rotate.normalise()
            tangoForwardFacingVector = tangoForwardFacingVector.rotateVector(rotate)
            tangoForwardFacingVector.normalise()

            val vectorToTarget = mVector(targetPosition.x - tangoPosition.x, targetPosition.y - tangoPosition.z, -targetPosition.z - tangoPosition.y)
            vectorToTarget.normalise()

            val rotationAxis = tangoForwardFacingVector.crossProduct(vectorToTarget)
            rotationAxis.normalise()
            var rotationAngle = tangoForwardFacingVector.invDotProduct(vectorToTarget)

            val test = rotationAxis.crossProduct(tangoForwardFacingVector)
            if (test.dotProduct(vectorToTarget) > 0)
            {
                rotationAngle = -rotationAngle
            }

            val requiredQuaternion = mQuaternion(rotationAxis, rotationAngle)
            requiredQuaternion.normalise()

            return requiredQuaternion
        }

        fun getElevationAngle(targetPosition: mVector, tangoPoseData: TangoPoseData): Double
        {
            val tangoOrientation = mQuaternion(tangoPoseData.rotation[0], tangoPoseData.rotation[1], tangoPoseData.rotation[2], tangoPoseData.rotation[3])
            tangoOrientation.normalise()

            val tangoPosition = mVector(tangoPoseData.translation[0], tangoPoseData.translation[1], tangoPoseData.translation[2])

            val tangoForwardVector = mVector(0.0, 0.0, 1.0)
            var tangoForwardFacingVector = tangoForwardVector.rotateVector(tangoOrientation)
            tangoForwardFacingVector.normalise()

            // Rotate the vector to align with global coord system
            val rotate = mQuaternion(mVector(0.0, 1.0, 0.0), Math.PI)
            rotate.normalise()
            tangoForwardFacingVector = tangoForwardFacingVector.rotateVector(rotate)
            tangoForwardFacingVector.normalise()

            val vectorToTarget = mVector(targetPosition.x - tangoPosition.x, targetPosition.y - tangoPosition.z, -targetPosition.z - tangoPosition.y)
            vectorToTarget.normalise()

            var rotationAngle = tangoForwardFacingVector.invDotProduct(vectorToTarget)

            rotationAngle -= Math.PI

            return -rotationAngle
        }

        fun getXPosition(targetPosition: mVector, tangoPoseData: TangoPoseData): Double {
            val tangoOrientation = mQuaternion(tangoPoseData.rotation[0], tangoPoseData.rotation[1], tangoPoseData.rotation[2], tangoPoseData.rotation[3])
            tangoOrientation.normalise()

            val tangoPosition = mVector(tangoPoseData.translation[0], tangoPoseData.translation[1], tangoPoseData.translation[2])

            val tangoForwardVector = mVector(1.0, 0.0, 0.0)
            var tangoForwardFacingVector = tangoForwardVector.rotateVector(tangoOrientation)
            tangoForwardFacingVector.normalise()

            // Rotate the vector to align with global coord system
            val rotate = mQuaternion(mVector(0.0, 1.0, 0.0), Math.PI)
            rotate.normalise()
            tangoForwardFacingVector = tangoForwardFacingVector.rotateVector(rotate)
            tangoForwardFacingVector.normalise()

            val vectorToTarget = mVector(targetPosition.x - tangoPosition.x, targetPosition.y - tangoPosition.z, -targetPosition.z - tangoPosition.y)
            vectorToTarget.normalise()

            val rotationAngle = tangoForwardFacingVector.invDotProduct(vectorToTarget)

            val distanceToTarget = vectorToTarget.length

            return distanceToTarget * cos(rotationAngle)
        }
    }
}

internal class mVector(var x: Double, var y: Double, var z: Double)
{
    val length: Double = sqrt(this.x * this.x + this.y * this.y + this.z * this.z)

    fun normalise()
    {
        this.x /= this.length
        this.y /= this.length
        this.z /= this.length
    }

    fun dotProduct(v: mVector): Double  = this.x * v.x + this.y * v.y + this.z * v.z

    fun invDotProduct(v: mVector): Double = acos(dotProduct(v))

    fun crossProduct(v: mVector): mVector {
        // p x v
        return mVector(this.y * v.z - this.z * v.y,
                this.z * v.x - this.x * v.z,
                this.x * v.y - this.y * v.x)
    }

    fun rotateVector(q: mQuaternion): mVector
    {
        val x = (1.0 - 2.0 * q.y * q.y - 2.0 * q.z * q.z) * this.x +
                2.0 * (q.x * q.y + q.w * q.z) * this.y +
                2.0 * (q.x * q.z - q.w * q.y) * this.z
        val y = 2.0 * (q.x * q.y - q.w * q.z) * this.x +
                (1.0 - 2.0 * q.x * q.x - 2.0 * q.z * q.z) * this.y +
                2.0 * (q.y * q.z + q.w * q.x) * this.z
        val z = 2.0 * (q.x * q.z + q.w * q.y) * this.x +
                2.0 * (q.y * q.z - q.w * q.x) * this.y +
                (1.0 - 2.0 * q.x * q.x - 2.0 * q.y * q.y) * this.z

        return mVector(x, y, z)
    }
}

internal class mQuaternion
{
    var x: Double = 0.toDouble()
    var y: Double = 0.toDouble()
    var z: Double = 0.toDouble()
    var w: Double = 0.toDouble()

    private var length: Double = 0.toDouble()

    constructor(v: mVector, angle: Double)
    {
        this.x = v.x * sin(angle / 2)
        this.y = v.y * sin(angle / 2)
        this.z = v.z * sin(angle / 2)
        this.w = cos(angle / 2)

        this.length = sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w)
    }

    constructor(x: Double, y: Double, z: Double, w: Double)
    {
        this.x = x
        this.y = y
        this.z = z
        this.w = w

        this.length = sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w)
    }

    fun normalise()
    {
        this.x /= this.length
        this.y /= this.length
        this.z /= this.length
        this.w /= this.length
    }

    val inverse: Quaternion
        get() = Quaternion(-this.x / this.length, this.y / this.length, -this.z / this.length, this.w / this.length)

    fun multiply(q: Quaternion): Quaternion
    {
        // is p * q (in that order)
        return Quaternion(this.w * q.x + this.x * q.w - this.y * q.z + this.z * q.y,
                this.w * q.y + this.x * q.z + this.y * q.w - this.z * q.x,
                this.w * q.z - this.x * q.y + this.y * q.x + this.z * q.w,
                this.w * q.w - this.x * q.x - this.y * q.y - this.z * q.z)
    }
}


/**
 * Convenient class for calculating transformations from the Tango world to the OpenGL world,
 * using Rajawali specific classes and conventions.
 */
internal object ClassScenePoseCalculator
{
    private val TAG = ClassScenePoseCalculator::class.java.simpleName

    /**
     * Transformation from the Tango Area Description or Start of Service coordinate frames
     * to the OpenGL coordinate frame.
     * NOTE: Rajawali uses column-major for matrices.
     */
    val OPENGL_T_TANGO_WORLD = Matrix4(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0))

    /**
     * Transformation from the Tango RGB camera coordinate frame to the OpenGL camera frame.
     */
    val COLOR_CAMERA_T_OPENGL_CAMERA = Matrix4(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 1.0))

    /**
     * Transformation for device rotation on 270 degrees.
     */
    val ROTATION_270_T_DEFAULT = Matrix4(doubleArrayOf(0.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0))

    /**
     * Transformation for device rotation on 180 degrees.
     */
    val ROTATION_180_T_DEFAULT = Matrix4(doubleArrayOf(-1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0))

    /**
     * Transformation for device rotation on 90 degrees.
     */
    val ROTATION_90_T_DEFAULT = Matrix4(doubleArrayOf(0.0, -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0))

    /**
     * Transformation for device rotation on default orientation.
     */
    val ROTATION_0_T_DEFAULT = Matrix4(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0))

    val DEPTH_CAMERA_T_OPENGL_CAMERA = Matrix4(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 1.0))

    /**
     * Up vector in the Tango start of Service and Area Description frame.
     */
    val TANGO_WORLD_UP = Vector3(0.0, 0.0, 1.0)

    /**
     * Converts from TangoPoseData to a Matrix4 for transformations.
     */
    fun tangoPoseToMatrix(tangoPose: TangoPoseData): Matrix4 {
        val v = Vector3(tangoPose.translation[0],
                tangoPose.translation[1], tangoPose.translation[2])
        val q = Quaternion(tangoPose.rotation[3], tangoPose.rotation[0],
                tangoPose.rotation[1], tangoPose.rotation[2])
        // NOTE: Rajawali quaternions use a left-hand rotation around the axis convention.
        q.conjugate()
        val m = Matrix4()
        m.setAll(v, Vector3(1.0, 1.0, 1.0), q)
        return m
    }

    /**
     * Converts a transform in Matrix4 format to TangoPoseData.
     */
    fun matrixToTangoPose(transform: Matrix4): TangoPoseData {
        // Get translation and rotation components from the transformation matrix.
        val p = transform.translation
        val q = Quaternion()
        q.fromMatrix(transform)

        val tangoPose = TangoPoseData()
        tangoPose.translation = DoubleArray(3)
        val t = tangoPose.translation
        t[0] = p.x
        t[1] = p.y
        t[2] = p.z
        tangoPose.rotation = DoubleArray(4)
        val r = tangoPose.rotation
        r[0] = q.x
        r[1] = q.y
        r[2] = q.z
        r[3] = q.w

        return tangoPose
    }

    /**
     * Helper method to extract a Pose object from a transformation matrix taking into account
     * Rajawali conventions.
     */
    fun matrixToPose(m: Matrix4): Pose {
        // Get translation and rotation components from the transformation matrix.
        val p = m.translation
        val q = Quaternion()
        q.fromMatrix(m)

        // NOTE: Rajawali quaternions use a left-hand rotation around the axis convention.
        q.conjugate()

        return Pose(p, q)
    }

    /**
     * Given a pose in start of service or area description frame calculate the corresponding
     * position and orientation for a 3D object in the Rajawali world.
     */
    fun toOpenGLPose(tangoPose: TangoPoseData): Pose {
        val startServiceTDevice = tangoPoseToMatrix(tangoPose)

        // Get device pose in OpenGL world frame.
        val openglWorldTDevice = OPENGL_T_TANGO_WORLD.clone().multiply(startServiceTDevice)

        return matrixToPose(openglWorldTDevice)
    }

    /**
     * Given a pose in start of service or area description frame and a screen rotaion calculate
     * the corresponding position and orientation for a 3D object in the Rajawali world.
     *
     * @param tangoPose     The input Tango Pose in start service or area description frame.
     * @param rotationIndex The screen rotation index, the index is following Android rotation enum.
     * see Android documentation for detail:
     * http://developer.android.com/reference/android/view/Surface.html#ROTATION_0 // NO_LINT
     */
    fun toOpenGLPoseWithScreenRotation(tangoPose: TangoPoseData, rotationIndex: Int): Pose {
        val startServiceTDevice = tangoPoseToMatrix(tangoPose)

        // Get device pose in OpenGL world frame.
        val openglWorldTDevice = OPENGL_T_TANGO_WORLD.clone().multiply(startServiceTDevice)

        when (rotationIndex) {
            0 -> openglWorldTDevice.multiply(ROTATION_0_T_DEFAULT)
            1 -> openglWorldTDevice.multiply(ROTATION_90_T_DEFAULT)
            2 -> openglWorldTDevice.multiply(ROTATION_180_T_DEFAULT)
            3 -> openglWorldTDevice.multiply(ROTATION_270_T_DEFAULT)
            else -> openglWorldTDevice.multiply(ROTATION_0_T_DEFAULT)
        }

        return matrixToPose(openglWorldTDevice)
    }

    /**
     * Use Tango camera intrinsics to calculate the projection Matrix for the Rajawali scene.
     */
    fun calculateProjectionMatrix(width: Int, height: Int, fx: Double, fy: Double,
                                  cx: Double, cy: Double): Matrix4 {
        // Uses frustumM to create a projection matrix taking into account calibrated camera
        // intrinsic parameter.
        // Reference: http://ksimek.github.io/2013/06/03/calibrated_cameras_in_opengl/
        val near = 0.1
        val far = 100.0

        val xScale = near / fx
        val yScale = near / fy
        val xOffset = (cx - width / 2.0) * xScale
        // Color camera's coordinates has y pointing downwards so we negate this term.
        val yOffset = -(cy - height / 2.0) * yScale

        val m = DoubleArray(16)
        Matrix.frustumM(m, 0,
                xScale * -width / 2.0 - xOffset,
                xScale * width / 2.0 - xOffset,
                yScale * -height / 2.0 - yOffset,
                yScale * height / 2.0 - yOffset,
                near, far)
        return Matrix4(m)
    }

    /**
     * Given the device pose in start of service frame, calculate the corresponding
     * position and orientation for a OpenGL Scene Camera in the Rajawali world.
     */
    fun toOpenGlCameraPose(devicePose: TangoPoseData, extrinsics: DeviceExtrinsics): Pose {
        val startServiceTdevice = tangoPoseToMatrix(devicePose)

        // Get device pose in OpenGL world frame.
        val openglTDevice = OPENGL_T_TANGO_WORLD.clone().multiply(startServiceTdevice)

        // Get OpenGL camera pose in OpenGL world frame.
        val openglWorldTOpenglCamera = openglTDevice.multiply(extrinsics.deviceTColorCamera).multiply(COLOR_CAMERA_T_OPENGL_CAMERA)

        return matrixToPose(openglWorldTOpenglCamera)
    }

    /**
     * Given the device pose in start of service frame, calculate the position and orientation of
     * the depth sensor in OpenGL coordinate frame.
     */
    fun toDepthCameraOpenGlPose(devicePose: TangoPoseData,
                                extrinsics: DeviceExtrinsics): Pose {
        val startServiceTdevice = tangoPoseToMatrix(devicePose)

        // Get device pose in OpenGL world frame.
        val openglTDevice = OPENGL_T_TANGO_WORLD.clone().multiply(startServiceTdevice)

        // Get OpenGL camera pose in OpenGL world frame.
        val openglWorldTOpenglCamera = openglTDevice.multiply(extrinsics.deviceTDepthCamera)

        return matrixToPose(openglWorldTOpenglCamera)
    }

    /**
     * Given a point and a normal in depth camera frame and the device pose in start of service
     * frame at the time the point and normal were acquired, calculate a Pose object which
     * represents the position and orientation of the fitted plane with its Y vector pointing
     * up in the gravity vector, represented in the Tango start of service frame.
     *
     * @param point     Point in depth frame where the plane has been detected.
     * @param normal    Normal of the detected plane.
     * @param tangoPose Device pose with respect to start of service at the time the plane was
     * fitted.
     */
    fun planeFitToTangoWorldPose(
            point: DoubleArray, normal: DoubleArray, tangoPose: TangoPoseData, extrinsics: DeviceExtrinsics): TangoPoseData {
        val startServiceTdevice = tangoPoseToMatrix(tangoPose)

        // Calculate the UP vector in the depth frame at the provided measurement pose.
        val depthUp = TANGO_WORLD_UP.clone()
        startServiceTdevice.clone().multiply(extrinsics.deviceTDepthCamera)
                .inverse().rotateVector(depthUp)

        // Calculate the transform in depth frame corresponding to the plane fitting information.
        val depthTplane = matrixFromPointNormalUp(point, normal, depthUp)

        // Convert to OpenGL frame.
        val tangoWorldTplane = startServiceTdevice.multiply(extrinsics.deviceTDepthCamera).multiply(depthTplane)

        return matrixToTangoPose(tangoWorldTplane)
    }

    /**
     * Calculates a transformation matrix based on a point, a normal and the up gravity vector.
     * The coordinate frame of the target transformation will be Z forward, X left, Y up.
     */
    fun matrixFromPointNormalUp(point: DoubleArray, normal: DoubleArray, up: Vector3): Matrix4 {
        val zAxis = Vector3(normal)
        zAxis.normalize()
        val xAxis = Vector3()
        xAxis.crossAndSet(up, zAxis)
        xAxis.normalize()
        val yAxis = Vector3()
        yAxis.crossAndSet(xAxis, zAxis)
        yAxis.normalize()

        val rot = DoubleArray(16)

        rot[Matrix4.M00] = xAxis.x
        rot[Matrix4.M10] = xAxis.y
        rot[Matrix4.M20] = xAxis.z

        rot[Matrix4.M01] = yAxis.x
        rot[Matrix4.M11] = yAxis.y
        rot[Matrix4.M21] = yAxis.z

        rot[Matrix4.M02] = zAxis.x
        rot[Matrix4.M12] = zAxis.y
        rot[Matrix4.M22] = zAxis.z

        rot[Matrix4.M33] = 1.0

        val m = Matrix4(rot)
        m.setTranslation(point[0], point[1], point[2])

        return m
    }

    /**
     * Converts a point, represented as a Vector3 from it's initial refrence frame to
     * the OpenGl world refrence frame. This allows various points to be depicted in
     * the OpenGl rendering.
     */
    fun getPointInEngineFrame(
            inPoint: Vector3,
            deviceTPointFramePose: TangoPoseData,
            startServiceTDevicePose: TangoPoseData): Vector3 {
        val startServiceTDeviceMatrix = tangoPoseToMatrix(startServiceTDevicePose)
        val deviceTPointFrameMatrix = tangoPoseToMatrix(deviceTPointFramePose)
        val startServiceTDepthMatrix = startServiceTDeviceMatrix.multiply(deviceTPointFrameMatrix)

        // Convert the depth point to a Matrix.
        val inPointMatrix = Matrix4()
        inPointMatrix.setToTranslation(inPoint)

        // Transform Point from depth frame to start of service frame to OpenGl world frame.
        val startServicePointMatrix = startServiceTDepthMatrix.multiply(inPointMatrix)
        val openGlWorldPointMatrix = OPENGL_T_TANGO_WORLD.clone().multiply(startServicePointMatrix)
        return matrixToPose(openGlWorldPointMatrix).position
    }
}

/**
 * Avoid instantiating the class since it will only be used statically.
 */

internal class Pose(val position: Vector3, val orientation: Quaternion)
{
    override fun toString(): String = "p:$position,q:$orientation"
}

internal class DeviceExtrinsics(imuTDevicePose: TangoPoseData, imuTColorCameraPose: TangoPoseData,
                                imuTDepthCameraPose: TangoPoseData)
{
    // Transformation from the position of the depth camera to the device frame.
    val deviceTDepthCamera: Matrix4

    // Transformation from the position of the color Camera to the device frame.
    val deviceTColorCamera: Matrix4

    init
    {
        val deviceTImu = ClassScenePoseCalculator.tangoPoseToMatrix(imuTDevicePose).inverse()
        val imuTColorCamera = ClassScenePoseCalculator.tangoPoseToMatrix(imuTColorCameraPose)
        val imuTDepthCamera = ClassScenePoseCalculator.tangoPoseToMatrix(imuTDepthCameraPose)
        deviceTDepthCamera = deviceTImu.clone().multiply(imuTDepthCamera)
        deviceTColorCamera = deviceTImu.multiply(imuTColorCamera)
    }
}

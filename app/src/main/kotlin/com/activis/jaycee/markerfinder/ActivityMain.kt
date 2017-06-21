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

import com.google.atap.tangoservice.Tango
import com.google.atap.tangoservice.TangoCameraIntrinsics
import com.google.atap.tangoservice.TangoConfig
import com.google.atap.tangoservice.TangoCoordinateFramePair
import com.google.atap.tangoservice.TangoErrorException
import com.google.atap.tangoservice.TangoInvalidException
import com.google.atap.tangoservice.TangoOutOfDateException
import com.google.atap.tangoservice.experimental.TangoImageBuffer

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast

import org.rajawali3d.view.SurfaceView

import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean

import com.projecttango.tangosupport.TangoSupport

/**
 * This is a simple example that shows how to use the Tango APIs to detect markers within camera
 * images.
 *
 *
 * Note that it is important to include the KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION configuration
 * parameter in order to achieve best results synchronizing the Rajawali virtual world with the
 * RGB camera.
 *
 *
 * If you're looking for a more stripped down example that doesn't use a rendering library like
 * Rajawali, see java_hello_video_example.
 */
class ActivityMain : Activity()
{
    internal var surfaceView: SurfaceView? = null
    internal var renderer: ClassRenderer? = null
    internal var tango: Tango? = null
    private var config: TangoConfig? = null
    internal var isConnected = false
    internal var cameraPoseTimestamp = 0.0
    @Volatile internal var currentImageBuffer: TangoImageBuffer? = null

    // Texture rendering related fields.
    // NOTE: Naming indicates which thread is in charge of updating this variable.
    internal var connectedTextureIdGlThread = INVALID_TEXTURE_ID
    internal val isFrameAvailableTangoThread = AtomicBoolean(false)
    internal var rgbTimestampGlThread: Double = 0.toDouble()

    internal var displayRotation = 0

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView = findViewById(R.id.surfaceview) as SurfaceView
        renderer = ClassRenderer(this)

        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
        displayManager?.registerDisplayListener(object : DisplayManager.DisplayListener
        {
            override fun onDisplayAdded(displayId: Int) {}

            override fun onDisplayChanged(displayId: Int)
            {
                synchronized(this@ActivityMain)
                {
                    setDisplayRotation()
                }
            }

            override fun onDisplayRemoved(displayId: Int) {}
        }, null)

        setupRenderer()
    }

    override fun onStart()
    {
        super.onStart()

        // Set render mode to RENDERMODE_CONTINUOUSLY to force getting onDraw callbacks until
        // the Tango service is properly set up and we start getting onFrameAvailable callbacks.
        surfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        // Check and request camera permission at run time.
        if (checkAndRequestPermissions())
        {
            bindTangoService()
        }
    }

    public override fun onStop()
    {
        super.onStop()

        // Synchronize against disconnecting while the service is being used in the OpenGL thread or
        // in the UI thread.
        // NOTE: DO NOT lock against this same object in the Tango callback thread. Tango.disconnect
        // will block here until all Tango callback calls are finished. If you lock against this
        // object in a Tango callback thread it will cause a deadlock.
        synchronized(this@ActivityMain)
        {
            try
            {
                // mTango may be null if the app is closed before permissions are granted.
                if (tango != null)
                {
                    tango!!.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
                    tango!!.disconnect()
                }
                // We need to invalidate the connected texture ID so that we cause a
                // re-connection in the OpenGL thread after resume.
                connectedTextureIdGlThread = INVALID_TEXTURE_ID
                tango = null
                isConnected = false
            }
            catch (e: TangoErrorException)
            {
                Log.e(TAG, getString(R.string.exception_tango_error), e)
            }
        }
    }

    /**
     * Initialize Tango Service as a normal Android Service.
     */
    private fun bindTangoService()
    {
        // Since we call mTango.disconnect() in onStop, this will unbind Tango Service, so every
        // time onStart gets called we should create a new Tango object.
        tango = Tango(this@ActivityMain, Runnable
        // Pass in a Runnable to be called from UI thread when Tango is ready. This Runnable
        // will be running on a new thread.
        // When Tango is ready, we can call Tango functions safely here only when there
        // are no UI thread changes involved.
        {
            // Synchronize against disconnecting while the service is being used in the
            // OpenGL thread or in the UI thread.
            synchronized(this@ActivityMain)
            {
                try
                {
                    config = setupTangoConfig(tango!!)
                    tango!!.connect(config!!)
                    startupTango()
                    TangoSupport.initialize(tango!!)
                    isConnected = true
                    setDisplayRotation()
                }
                catch (e: TangoOutOfDateException)
                {
                    Log.e(TAG, getString(R.string.exception_out_of_date), e)
                    showsToastAndFinishOnUiThread(R.string.exception_out_of_date)
                }
                catch (e: TangoErrorException)
                {
                    Log.e(TAG, getString(R.string.exception_tango_error), e)
                    showsToastAndFinishOnUiThread(R.string.exception_tango_error)
                }
                catch (e: TangoInvalidException)
                {
                    Log.e(TAG, getString(R.string.exception_tango_invalid), e)
                    showsToastAndFinishOnUiThread(R.string.exception_tango_invalid)
                }
            }
        })
    }

    /**
     * Sets up the Tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private fun setupTangoConfig(tango: Tango): TangoConfig
    {
        // Use default configuration for Tango Service, plus color camera, low latency
        // IMU integration and drift correction.
        val config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT)
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true)
        // NOTE: Low latency integration is necessary to achieve a precise alignment of
        // virtual objects with the RBG image and produce a good AR effect.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true)
        // Drift correction allows motion tracking to recover after it loses tracking.
        // The drift-corrected pose is available through the frame pair with
        // base frame AREA_DESCRIPTION and target frame DEVICE.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true)

        return config
    }

    /**
     * Set up the callback listeners for the Tango Service and obtain other parameters required
     * after Tango connection.
     * Listen to updates from the RGB camera.
     */
    private fun startupTango()
    {
        // No need to add any coordinate frame pairs since we aren't using pose data from callbacks.
        val framePairs = ArrayList<TangoCoordinateFramePair>()

        tango!!.connectListener(framePairs, ClassTangoUpdateCallback(this@ActivityMain))
        tango!!.experimentalConnectOnFrameListener(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, object : Tango.OnFrameAvailableListener
        {
            override fun onFrameAvailable(tangoImageBuffer: TangoImageBuffer, i: Int)
            {
                currentImageBuffer = copyImageBuffer(tangoImageBuffer)
            }

            internal fun copyImageBuffer(imageBuffer: TangoImageBuffer): TangoImageBuffer
            {
                val clone = ByteBuffer.allocateDirect(imageBuffer.data.capacity())
                imageBuffer.data.rewind()
                clone.put(imageBuffer.data)
                imageBuffer.data.rewind()
                clone.flip()
                return TangoImageBuffer(imageBuffer.width, imageBuffer.height,
                        imageBuffer.stride, imageBuffer.frameNumber, imageBuffer.timestamp,
                        imageBuffer.format, clone)
            }
        })
    }

    /**
     * Connects the view and renderer to the color camara and callbacks.
     */
    private fun setupRenderer()
    {
        // Register a Rajawali Scene Frame Callback to update the scene camera pose whenever a new
        // RGB frame is rendered.
        // (@see https://github.com/Rajawali/Rajawali/wiki/Scene-Frame-Callbacks)
        renderer!!.currentScene.registerFrameCallback(ClassRajawaliFrameCallback(this@ActivityMain))

        surfaceView!!.setSurfaceRenderer(renderer)
    }

    /**
     * Set the color camera background texture rotation and save the camera to display rotation.
     */
    private fun setDisplayRotation()
    {
        val display = windowManager.defaultDisplay
        displayRotation = display.rotation

        // We also need to update the camera texture UV coordinates. This must be run in the OpenGL
        // thread.
        surfaceView!!.queueEvent {
            if (isConnected)
            {
                renderer!!.updateColorCameraTextureUvGlThread(displayRotation)
            }
        }
    }

    /**
     * Check to see we have the necessary permissions for this app, and ask for them if we don't.

     * @return True if we have the necessary permissions, false if we haven't.
     */
    private fun checkAndRequestPermissions(): Boolean
    {
        if (!hasCameraPermission())
        {
            requestCameraPermission()
            return false
        }
        return true
    }

    /**
     * Check to see we have the necessary permissions for this app.
     */
    private fun hasCameraPermission(): Boolean
    {
        return ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request the necessary permissions for this app.
     */
    private fun requestCameraPermission()
    {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION))
        {
            showRequestPermissionRationale()
        }
        else
        {
            ActivityCompat.requestPermissions(this, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE)
        }
    }

    /**
     * If the user has declined the permission before, we have to explain that the app needs this
     * permission.
     */
    private fun showRequestPermissionRationale()
    {
        val dialog = AlertDialog.Builder(this)
                .setMessage("Java Marker Detection Example requires camera permission")
                .setPositiveButton("Ok") { dialogInterface, i ->
                    ActivityCompat.requestPermissions(this@ActivityMain,
                            arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE)
                }
                .create()
        dialog.show()
    }

    /**
     * Result for requesting camera permission.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray)
    {
        if (hasCameraPermission())
        {
            bindTangoService()
        }
        else
        {
            Toast.makeText(this, "Java Marker Detection Example requires camera permission", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Display toast on UI thread.

     * @param resId The resource id of the string resource to use. Can be formatted text.
     */
    private fun showsToastAndFinishOnUiThread(resId: Int)
    {
        runOnUiThread {
            Toast.makeText(this@ActivityMain, getString(resId), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    companion object
    {
        private val TAG = ActivityMain::class.java.simpleName
        private val INVALID_TEXTURE_ID = 0

        private val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private val CAMERA_PERMISSION_CODE = 0

        /**
         * Use Tango camera intrinsics to calculate the projection matrix for the Rajawali scene.

         * @param intrinsics camera instrinsics for computing the project matrix.
         */
        internal fun projectionMatrixFromCameraIntrinsics(intrinsics: TangoCameraIntrinsics): FloatArray
        {
            val cx = intrinsics.cx.toFloat()
            val cy = intrinsics.cy.toFloat()
            val width = intrinsics.width.toFloat()
            val height = intrinsics.height.toFloat()
            val fx = intrinsics.fx.toFloat()
            val fy = intrinsics.fy.toFloat()

            // Uses frustumM to create a projection matrix taking into account calibrated camera
            // intrinsic parameter.
            // Reference: http://ksimek.github.io/2013/06/03/calibrated_cameras_in_opengl/
            val near = 0.1f
            val far = 100f

            val xScale = near / fx
            val yScale = near / fy
            val xOffset = (cx - width / 2f) * xScale
            // Color camera's coordinates has y pointing downwards so we negate this term.
            val yOffset = -(cy - height / 2f) * yScale

            val m = FloatArray(16)
            Matrix.frustumM(m, 0,
                    xScale * (-width) / 2f - xOffset,
                    xScale * width / 2f - xOffset,
                    yScale * (-height) / 2f - yOffset,
                    yScale * height / 2f - yOffset,
                    near, far)
            return m
        }
    }
}
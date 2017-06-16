package com.activis.jaycee.markerfinder

import android.opengl.Matrix
import android.util.Log
import com.google.atap.tangoservice.TangoCameraIntrinsics
import com.google.atap.tangoservice.TangoErrorException
import com.google.atap.tangoservice.TangoPoseData
import com.projecttango.tangosupport.TangoSupport
import org.rajawali3d.scene.ASceneFrameCallback

class ClassRajawaliFrameCallback(val activityMain: ActivityMain): ASceneFrameCallback()
{
    private val TAG: String = "RajawaliFrameCallback"

    override fun onPreFrame(sceneTime: Long, deltaTime: Double)
    {
        try
        {
            synchronized(activityMain)
            {
                /* Don't execute if we're not connected */
                if(!activityMain.tangoIsConnected)
                {
                    return
                }

                /* Set up scene camera to match camera intrinsics */
                if(activityMain.renderer.isSceneCameraConfigured)
                {
                    val cameraIntrinsics: TangoCameraIntrinsics = TangoSupport.getCameraIntrinsicsBasedOnDisplayRotation(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, activityMain.displayRotation)
                    activityMain.renderer.setProjectionMatrix(projectionMatrixFromCameraIntrinsics(cameraIntrinsics))
                }

                /* Connect the camera texture to the OpenGL Texture if necessary
                   When the OpenGL context is recycled, Rajawali may regenerate the texture with a different ID.
                */
                if(activityMain.connectedGLThreadID != activityMain.renderer.getTextureId())
                {
                    activityMain.tango?.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, activityMain.renderer.getTextureId())
                    activityMain.connectedGLThreadID = activityMain.renderer.getTextureId()
                    Log.d(TAG, "Connected to texture ID: " + activityMain.renderer.getTextureId())
                }

                /* Update the frame if there's a new RGB frame */
                if (activityMain.frameIsAvailable.compareAndSet(true, false))
                {
                    activityMain.RGBTimestamp = activityMain.tango!!.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
                }

                /* If new frame was rendered, update the camera pose */
                if (activityMain.RGBTimestamp > activityMain.cameraPoseTimestamp)
                {
                    /* Calculate the camera color pose at the camera frame update time in OpenGL engine. */
                    val poseData = TangoSupport.getPoseAtTime(activityMain.RGBTimestamp,
                            TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                            TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            activityMain.displayRotation)
                    if (poseData.statusCode == TangoPoseData.POSE_VALID)
                    {
                        activityMain.renderer.updateRenderCameraPose(poseData)
                        activityMain.cameraPoseTimestamp = poseData.timestamp

                        /* Detect markers in new RGB frame */
                        val oglTcolorPose = TangoSupport.getPoseAtTime(activityMain.tangoImageBuffer!!.timestamp,
                                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                                TangoSupport.ROTATION_IGNORED)

                        val markerParam: TangoSupport.MarkerParam = TangoSupport.MarkerParam()
                        markerParam.markerSize = 0.14
                        markerParam.type = TangoSupport.TANGO_MARKER_ARTAG

                        try
                        {
                            val markerList: List<TangoSupport.Marker> = TangoSupport.detectMarkers(activityMain.tangoImageBuffer, TangoCameraIntrinsics.TANGO_CAMERA_COLOR, oglTcolorPose.translation, oglTcolorPose.rotation, markerParam)
                            activityMain.renderer.updateMarker(markerList)
                            //Log.d(TAG, String.format("Found marker: %d", markerList.size))
                        }
                        catch(e: TangoErrorException)
                        {
                            Log.e(TAG, "Marker error: ", e)
                        }
                    }

                    else
                    {
                        Log.w(TAG, "Can't get device pose at time: " + activityMain.RGBTimestamp)
                    }
                }
            }
        }
        catch(e: TangoErrorException)
        {
            Log.e(TAG, "TangoErrorException: " + e)
        }
        catch(t: Throwable)
        {
            Log.e(TAG, "Error on OpenGL thread: " + t)
        }
    }

    override fun onPostFrame(sceneTime: Long, deltaTime: Double) { }

    override fun onPreDraw(sceneTime: Long, deltaTime: Double) { }

    override fun callPreFrame(): Boolean { return true }

    /* Use Tango camera intrinsics to calculate the projection matrix for the Rajawali scene */
    private fun projectionMatrixFromCameraIntrinsics(intrinsics: TangoCameraIntrinsics): FloatArray
    {
        val cx = intrinsics.cx.toFloat()
        val cy = intrinsics.cy.toFloat()
        val width = intrinsics.width.toFloat()
        val height = intrinsics.height.toFloat()
        val fx = intrinsics.fx.toFloat()
        val fy = intrinsics.fy.toFloat()

        val near = 0.1f
        val far = 100f

        val xScale = near / fx
        val yScale = near / fy
        val xOffset = (cx - width / 2.0f) * xScale
        // Color camera's coordinates has y pointing downwards so we negate this term.
        val yOffset = -(cy - height / 2.0f) * yScale

        val m = FloatArray(16)
        Matrix.frustumM(m, 0,
                xScale * -width / 2.0f - xOffset,
                xScale * width / 2.0f - xOffset,
                yScale * -height / 2.0f - yOffset,
                yScale * height / 2.0f - yOffset,
                near, far)
        return m
    }
}
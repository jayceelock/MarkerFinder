package com.activis.jaycee.markerfinder

import android.opengl.Matrix
import android.util.Log
import com.google.atap.tangoservice.TangoCameraIntrinsics
import com.google.atap.tangoservice.TangoErrorException
import com.google.atap.tangoservice.TangoException
import com.google.atap.tangoservice.TangoPoseData
import com.projecttango.tangosupport.TangoSupport
import org.rajawali3d.scene.ASceneFrameCallback

class ClassRajawaliFrameCallback(val activityMain: ActivityMain): ASceneFrameCallback()
{
    private val TAG: String = "RajawaliFrameCallback"

    override fun onPreFrame(sceneTime: Long, deltaTime: Double)
    {
        // NOTE: This is called from the OpenGL render thread after all the renderer
        // onRender callbacks have a chance to run and before scene objects are rendered
        // into the scene.

        // Prevent concurrent access to {@code mIsFrameAvailableTangoThread} from the Tango
        // callback thread and service disconnection from an onPause event.
        try
        {
            synchronized(activityMain)
            {
                // Don't execute tango API actions if we're not connected to the service.
                if (!activityMain.isConnected)
                {
                    return
                }

                // Set up scene camera projection to match RGB camera intrinsics.
                if (!activityMain.renderer.isSceneCameraConfigured)
                {
                    val intrinsics = TangoSupport.getCameraIntrinsicsBasedOnDisplayRotation(
                            TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                            activityMain.displayRotation)
                    activityMain.renderer.setProjectionMatrix(ActivityMain.projectionMatrixFromCameraIntrinsics(intrinsics))
                }
                // Connect the camera texture to the OpenGL Texture if necessary
                // NOTE: When the OpenGL context is recycled, Rajawali may regenerate the
                // texture with a different ID.
                if (activityMain.connectedTextureIdGlThread != activityMain.renderer.textureId)
                {
                    activityMain.tango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, activityMain.renderer.textureId)
                    activityMain.connectedTextureIdGlThread = activityMain.renderer.textureId
                    Log.d(ClassRajawaliFrameCallback.TAG, "connected to texture id: " + activityMain.renderer.textureId)
                }

                // If there is a new RGB camera frame available, update the texture
                // with it.
                if (activityMain.isFrameAvailableTangoThread.compareAndSet(true, false))
                {
                    activityMain.rgbTimestampGlThread = activityMain.tango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
                }

                // If a new RGB frame has been rendered, update the camera pose to match.
                if (activityMain.rgbTimestampGlThread > activityMain.cameraPoseTimestamp)
                {
                    // Calculate the camera color pose at the camera frame update time in
                    // OpenGL engine.
                    //
                    // When drift correction mode is enabled in config file, we must query
                    // the device with respect to Area Description pose in order to use the
                    // drift corrected pose.
                    //
                    // Note that if you don't want to use the drift corrected pose, the
                    // normal device with respect to start of service pose is available.
                    val lastFramePose = TangoSupport.getPoseAtTime(
                            activityMain.rgbTimestampGlThread,
                            TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                            TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            activityMain.displayRotation)
                    if (lastFramePose.statusCode == TangoPoseData.POSE_VALID)
                    {
                        // Update the camera pose from the renderer
                        activityMain.renderer.updateRenderCameraPose(lastFramePose)
                        activityMain.cameraPoseTimestamp = lastFramePose.timestamp

                        // Detect markers within the current image buffer.
                        val param = TangoSupport.MarkerParam()
                        param.type = TangoSupport.TANGO_MARKER_ARTAG
                        param.markerSize = 0.1395

                        val worldTcamera = TangoSupport.getPoseAtTime(
                                activityMain.currentImageBuffer.timestamp,
                                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                                TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                                TangoSupport.ROTATION_IGNORED)
                        try
                        {
                            val markerList = TangoSupport.detectMarkers(
                                    activityMain.currentImageBuffer,
                                    TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                    worldTcamera.translation,
                                    worldTcamera.rotation,
                                    param)

                            activityMain.renderer.updateMarkers(markerList)
                        }
                        catch (e: TangoException)
                        {
                            e.printStackTrace()
                        }

                    }
                    else
                    {
                        // When the pose status is not valid, it indicates the tracking has
                        // been lost. In this case, we simply stop rendering.
                        //
                        // This is also the place to display UI to suggest the user walk
                        // to recover tracking.
                        Log.w(ClassRajawaliFrameCallback.TAG, "Can't get device pose at time: " + activityMain.rgbTimestampGlThread)
                    }
                }
            }

            // Avoid crashing the application due to unhandled exceptions.
        }
        catch (e: TangoErrorException)
        {
            Log.e(ClassRajawaliFrameCallback.TAG, "Tango API call error within the OpenGL render thread", e)
        }
        catch (t: Throwable)
        {
            Log.e(ClassRajawaliFrameCallback.TAG, "Exception on the OpenGL thread", t)
        }
    }

    override fun onPreDraw(sceneTime: Long, deltaTime: Double) { }

    override fun onPostFrame(sceneTime: Long, deltaTime: Double) { }

    override fun callPreFrame(): Boolean = true

    companion object
    {
        private val TAG = ClassRajawaliFrameCallback::class.java.simpleName
    }
}
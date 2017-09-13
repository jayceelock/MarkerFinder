package com.activis.jaycee.markerfinder

import android.opengl.GLSurfaceView
import com.google.atap.tangoservice.Tango
import com.google.atap.tangoservice.TangoCameraIntrinsics
import com.google.atap.tangoservice.TangoPoseData

class ClassTangoUpdateCallback(val activityMain: ActivityMain): Tango.TangoUpdateCallback()
{
    private val TAG: String = "TangoUpdateCallback"

    override fun onPoseAvailable(pose: TangoPoseData)
    {
        activityMain.runnableSoundGenerator.setTangoPose(pose)
    }

    override fun onFrameAvailable(cameraId: Int)
    {
        // Check if the frame available is for the camera we want and update its frame
        // on the view.
        if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
        {
            // Now that we are receiving onFrameAvailable callbacks, we can switch
            // to RENDERMODE_WHEN_DIRTY to drive the render loop from this callback.
            // This will result in a frame rate of approximately 30FPS, in synchrony with
            // the RGB camera driver.
            // If you need to render at a higher rate (i.e., if you want to render complex
            // animations smoothly) you  can use RENDERMODE_CONTINUOUSLY throughout the
            // application lifecycle.
            if (activityMain.surfaceView.renderMode != GLSurfaceView.RENDERMODE_WHEN_DIRTY)
            {
                activityMain.surfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            }

            // Mark a camera frame as available for rendering in the OpenGL thread.
            activityMain.isFrameAvailableTangoThread.set(true)
            // Trigger a Rajawali render to update the scene with the new RGB data.
            activityMain.surfaceView.requestRender()
        }
    }
}
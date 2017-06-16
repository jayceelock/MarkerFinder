package com.activis.jaycee.markerfinder

import android.opengl.GLSurfaceView
import com.google.atap.tangoservice.Tango
import com.google.atap.tangoservice.TangoCameraIntrinsics
import com.google.atap.tangoservice.experimental.TangoImageBuffer
import java.nio.ByteBuffer

class ClassTangoUpdateCallback(val activityMain: ActivityMain): Tango.TangoUpdateCallback(), Tango.OnFrameAvailableListener
{
    private val TAG: String = "TangoUpdateCallback"

    override fun onFrameAvailable(imageBuffer: TangoImageBuffer, cameraId: Int)
    {
        activityMain.tangoImageBuffer = imageBuffer
    }

    override fun onFrameAvailable(cameraId: Int)
    {
        if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
        {
            if (activityMain.surfaceView.renderMode != GLSurfaceView.RENDERMODE_WHEN_DIRTY)
            {
                activityMain.surfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            }
            activityMain.frameIsAvailable.set(true)
            activityMain.surfaceView.requestRender()
        }
    }
}
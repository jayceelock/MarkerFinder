package com.activis.jaycee.markerfinder

import android.opengl.GLSurfaceView
import com.google.atap.tangoservice.Tango
import com.google.atap.tangoservice.TangoCameraIntrinsics

class ClassTangoUpdateCallback(activityMain: ActivityMain): Tango.TangoUpdateCallback()
{
    private val activityMain: ActivityMain = activityMain

    override fun onFrameAvailable(cameraId: Int)
    {
        //super.onFrameAvailable(cameraId)
        if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
        {
            if (activityMain.surfaceView?.renderMode !== GLSurfaceView.RENDERMODE_WHEN_DIRTY)
            {
                activityMain.surfaceView?.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            }
            activityMain.frameIsAvailable.set(true)
            activityMain.surfaceView?.requestRender()
        }
    }
}
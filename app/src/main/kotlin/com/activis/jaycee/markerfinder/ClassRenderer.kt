package com.activis.jaycee.markerfinder

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import com.google.atap.tangoservice.TangoPoseData
import com.projecttango.tangosupport.TangoSupport
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.textures.ATexture
import org.rajawali3d.materials.textures.StreamingTexture
import org.rajawali3d.math.Matrix4
import org.rajawali3d.math.Quaternion
import org.rajawali3d.primitives.ScreenQuad
import org.rajawali3d.renderer.Renderer
import javax.microedition.khronos.opengles.GL10

class ClassRenderer(context: Context) : Renderer(context)
{
    internal val TAG: String = "ClassRenderer.class"

    internal var screenBackgroundQuad: ScreenQuad? = null
    internal var cameraTexture: ATexture? = null
    internal var textureCoords: FloatArray = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f)

    internal var isSceneCameraConfigured: Boolean = false

    override fun initScene()
    {
        val cameraMaterial: Material = Material()
        cameraMaterial.colorInfluence = 0.0f

        if(screenBackgroundQuad == null)
        {
            Log.d(TAG, "Creating screen quad");
            screenBackgroundQuad = ScreenQuad()
            screenBackgroundQuad?.geometry?.setTextureCoords(textureCoords)
        }

        val tmp: StreamingTexture.ISurfaceListener? = null
        cameraTexture = StreamingTexture("camera", tmp)
        try
        {
            Log.d(TAG, "Setting texture")
            cameraMaterial.addTexture(cameraTexture)
            screenBackgroundQuad?.material = cameraMaterial
        }
        catch (e: ATexture.TextureException)
        {
            Log.e(TAG, "Texture exception: ", e)
        }
        currentScene.addChild(screenBackgroundQuad)
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) { }

    override fun onTouchEvent(event: MotionEvent?) { }

    /* Override this method to mark the camera for re-configuration (set proper
     * projection matrix) since it will be reset by Rajawali on surface changes.
     */
    override fun onRenderSurfaceSizeChanged(gl: GL10?, width: Int, height: Int)
    {
        super.onRenderSurfaceSizeChanged(gl, width, height)
        isSceneCameraConfigured = false
    }

    /* Handle screen orientation changes.
    Run in OpenGL thread.
    */
    fun updateColorCameraTextureUvGlThread(rotation: Int)
    {
        if (screenBackgroundQuad == null)
        {
            screenBackgroundQuad = ScreenQuad()
        }

        val textureCoords: FloatArray = TangoSupport.getVideoOverlayUVBasedOnDisplayRotation(textureCoords, rotation)
        screenBackgroundQuad?.geometry?.setTextureCoords(textureCoords, true)
        screenBackgroundQuad?.geometry?.reload()
    }

    /* Sets the projection matrix for the scene camera to match the parameters of the color camera */
    fun setProjectionMatrix(matrixFloats: FloatArray)
    {
        currentCamera.projectionMatrix = Matrix4(matrixFloats)
    }

    /* Returns the Texture ID on which the camera content is rendered.
       This must be called from the OpenGL render thread
     */
    fun getTextureId(): Int
    {
        if(cameraTexture == null)
        {
            return -1
        }
        else
        {
            return cameraTexture!!.textureId
        }
    }

    /* Update the scene camera based on the provided pose in Tango start of service frame.
       The camera pose should match the pose of the camera color at the time of the last rendered
       RGB frame, which can be retrieved with this.getTimestamp();
       This must be called from the OpenGL render thread
     */
    fun updateRenderCameraPose(cameraPose: TangoPoseData)
    {
        val rotation: FloatArray = cameraPose.rotationAsFloats
        val translation: FloatArray = cameraPose.translationAsFloats
        val quaternion: Quaternion = Quaternion(rotation[3].toDouble(), rotation[0].toDouble(), rotation[1].toDouble(), rotation[2].toDouble())

        /* Conjugating the Quaternion is needed because Rajawali uses left-handed convention for quaternions. */
        currentCamera.setRotation(quaternion.conjugate())
        currentCamera.setPosition(translation[0].toDouble(), translation[1].toDouble(), translation[2].toDouble())
    }
}
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
import org.rajawali3d.math.vector.Vector3
import org.rajawali3d.primitives.ScreenQuad
import org.rajawali3d.renderer.Renderer
import javax.microedition.khronos.opengles.GL10

class ClassRenderer(context: Context) : Renderer(context)
{
    internal val TAG: String = "ClassRenderer.class"

    internal var screenBackgroundQuad: ScreenQuad
    internal var cameraTexture: ATexture
    internal var textureCoords0: FloatArray = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f)

    internal var markerObjectHash: MutableMap<String, ClassMarkerObject>

    internal var isSceneCameraConfigured: Boolean = false

    init
    {
        markerObjectHash = HashMap()
        screenBackgroundQuad = ScreenQuad()

        val tmp: StreamingTexture.ISurfaceListener? = null
        cameraTexture = StreamingTexture("camera", tmp)
    }

    override fun initScene()
    {
        val cameraMaterial: Material = Material()
        cameraMaterial.colorInfluence = 0.0f

        Log.d(TAG, "Creating screen quad")
        screenBackgroundQuad.geometry.setTextureCoords(textureCoords0)

        try
        {
            Log.d(TAG, "Setting texture")
            cameraMaterial.addTexture(cameraTexture)
            screenBackgroundQuad.material = cameraMaterial
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
    override fun onRenderSurfaceSizeChanged(gl: GL10, width: Int, height: Int)
    {
        super.onRenderSurfaceSizeChanged(gl, width, height)
        isSceneCameraConfigured = false
    }

    /* Handle screen orientation changes.
       Run in OpenGL thread.
    */
    fun updateColorCameraTextureUvGlThread(rotation: Int)
    {
        val textureCoords: FloatArray = TangoSupport.getVideoOverlayUVBasedOnDisplayRotation(textureCoords0, rotation)
        screenBackgroundQuad.geometry.setTextureCoords(textureCoords, true)
        screenBackgroundQuad.geometry.reload()
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
        return cameraTexture.textureId
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

    fun updateMarker(markerList: List<TangoSupport.Marker>)
    {
        if(markerList.isNotEmpty())
        {
            for(marker in markerList)
            {
                val tangoPose: TangoPoseData = TangoSupport.getPoseAtTime(0.0,
                        TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.ROTATION_IGNORED)
                //val markerPose: TangoPoseData = TangoPoseData()
                //markerPose.translation = marker.translation
                //markerPose.rotation = marker.orientation

                val transformMatrix: Matrix4 = tangoPoseToMatrix(tangoPose)
                // Log.d(TAG, String.format("%d %d %d %d", transformMatrix.doubleValues))
                // TangoSupport.doubleTransformPose(transformMatrix.doubleValues, marker.translation, marker.orientation, marker.translation, marker.orientation)

                val  markerContent: String = marker.content
                if(markerContent !in markerObjectHash)
                {
                    val obj: ClassMarkerObject = ClassMarkerObject(marker)
                    obj.transformObject()
                    obj.makeMarker()
                    currentScene.addChild(obj.markerObject)

                    markerObjectHash[markerContent] = obj
                }
                else
                {
                    val obj: ClassMarkerObject? = markerObjectHash[markerContent]
                    obj?.updatePosition(marker)
                }
            }
        }
    }

    fun tangoPoseToMatrix(tangoPose: TangoPoseData): Matrix4
    {
        var v: Vector3 = Vector3(tangoPose.translation[0], tangoPose.translation[1], tangoPose.translation[2])
        var q: Quaternion = Quaternion(tangoPose.rotation[3], tangoPose.rotation[0], tangoPose.rotation[2], tangoPose.rotation[3])
        q.conjugate()

        var m: Matrix4 = Matrix4()
        m.setAll(v, Vector3(1.0, 1.0, 1.0), q)

        return m
    }
}
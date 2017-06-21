package com.activis.jaycee.markerfinder

import android.graphics.Color
import android.util.Log
import com.projecttango.tangosupport.TangoSupport
import org.rajawali3d.Object3D
import org.rajawali3d.materials.Material
import org.rajawali3d.math.Quaternion
import org.rajawali3d.math.vector.Vector3
import org.rajawali3d.primitives.Line3D
import org.rajawali3d.primitives.Sphere
import org.rajawali3d.scene.Scene
import java.util.Stack

class ClassMarkerObject(val marker: TangoSupport.Marker)
{
    private val TAG: String = "MarkerObject"

    //private var markerCentre: Sphere
    private var markerBorder: Line3D

    private var xAxis: Line3D
    private var yAxis: Line3D
    private var zAxis: Line3D

    private var markerSize: Double = 0.0

    internal var markerObject: Object3D

    private val material: Material

    init
    {
        material = Material()
        material.color = Color.RED

        //val markerCentreCoords: Vector3 = Vector3(marker.translation[0], marker.translation[1], marker.translation[2])
        //val cornerCoordStack: Stack<Vector3> = makeMarkerBorder(marker)

        markerBorder = Line3D(makeMarkerBorder(marker), 10.0f, Color.CYAN)
        markerBorder.material = material

        xAxis = makeAxis('x', marker)
        yAxis = makeAxis('y', marker)
        zAxis = makeAxis('z', marker)

        //markerCentre = Sphere(0.1f, 24, 24)
        //markerCentre.position = markerCentreCoords
        //markerCentre.material = material

        markerObject = Object3D()
    }

    fun makeAxis(wAxis: Char, marker: TangoSupport.Marker): Line3D
    {
        val markerCentreCoords: Vector3 = Vector3(marker.translation[0], marker.translation[1], marker.translation[2])
        //Log.d(TAG, String.format("%f %f %f", markerCentreCoords.x, markerCentreCoords.y, markerCentreCoords.z));

        val q: Quaternion = Quaternion(marker.orientation[3], marker.orientation[0], marker.orientation[1], marker.orientation[2])
        //val markerSize: Double = 0.1395
        val stack: Stack<Vector3> = Stack()

        if(wAxis == 'x')
        {
            val axis: Vector3 = q.multiply(Vector3(markerSize / 3, 0.0, 0.0))
            stack.add(markerCentreCoords)
            stack.add(Vector3.addAndCreate(markerCentreCoords, axis))
        }
        if(wAxis == 'y')
        {
            val axis: Vector3 = q.multiply(Vector3(0.0, markerSize / 3, 0.0))
            stack.add(markerCentreCoords)
            stack.add(Vector3.addAndCreate(markerCentreCoords, axis))
        }
        if(wAxis == 'z')
        {
            val axis: Vector3 = q.multiply(Vector3(0.0, 0.0, markerSize / 3))
            stack.add(markerCentreCoords)
            stack.add(Vector3.addAndCreate(markerCentreCoords, axis))
        }

        val axisLine: Line3D = Line3D(stack, 10.0f, Color.GREEN)
        axisLine.material = Material()

        return axisLine
    }

    fun makeMarkerBorder(marker: TangoSupport.Marker): Stack<Vector3>
    {
        val markerCornerBottomLeft: Vector3 = Vector3(marker.corners3d[0][0].toDouble(), marker.corners3d[0][1].toDouble(), marker.corners3d[0][2].toDouble())
        val markerCornerBottomRight: Vector3 = Vector3(marker.corners3d[1][0].toDouble(), marker.corners3d[1][1].toDouble(), marker.corners3d[1][2].toDouble())
        val markerCornerTopRight: Vector3 = Vector3(marker.corners3d[2][0].toDouble(), marker.corners3d[2][1].toDouble(), marker.corners3d[2][2].toDouble())
        val markerCornerTopLeft: Vector3 = Vector3(marker.corners3d[3][0].toDouble(), marker.corners3d[3][1].toDouble(), marker.corners3d[3][2].toDouble())

        val cornerCoordStack: Stack<Vector3> = Stack()
        cornerCoordStack.add(markerCornerBottomLeft)
        cornerCoordStack.add(markerCornerBottomRight)
        cornerCoordStack.add(markerCornerBottomRight)
        cornerCoordStack.add(markerCornerTopRight)
        cornerCoordStack.add(markerCornerTopRight)
        cornerCoordStack.add(markerCornerTopLeft)
        cornerCoordStack.add(markerCornerTopLeft)
        cornerCoordStack.add(markerCornerBottomLeft)

        markerSize = markerCornerTopLeft.distanceTo(markerCornerTopRight)

        return cornerCoordStack
    }

    fun transformObject()
    {
        //markerBorder.scale = Vector3(1/markerSize, 1/markerSize, 1/markerSize)
        //xAxis.scale = Vector3(1/markerSize, 1/markerSize, 1/markerSize)
        yAxis.scale = Vector3(1/markerSize, 1/markerSize, 1/markerSize)
        // zAxis.scale = Vector3(1/markerSize, 1/markerSize, 1/markerSize)
        //markerCentre.position = Vector3(markerCentre.x/0.14, markerCentre.y/0.14, markerCentre.z/0.14)
    }

    fun makeMarker()
    {
        markerObject.addChild(markerBorder)
        // markerObject.addChild(markerCentre)
        markerObject.addChild(xAxis)
        markerObject.addChild(yAxis)
        markerObject.addChild(zAxis)
    }

    fun updatePosition(marker: TangoSupport.Marker)
    {
        /* Remove old marker objects before update */
        markerObject.removeChild(markerBorder)
        // markerObject.removeChild(markerCentre)
        markerObject.removeChild(xAxis)
        markerObject.removeChild(yAxis)
        markerObject.removeChild(zAxis)

        /* Update individual components' positions */
        // markerBorder = Line3D(makeMarkerBorder(marker), 10.0f)
        // markerBorder.material = material
        // markerCentre.position = Vector3(marker.translation[0], marker.translation[1], marker.translation[2])

        /* Transform their position and place back on parent object */
        transformObject()
        makeMarker()
    }
}
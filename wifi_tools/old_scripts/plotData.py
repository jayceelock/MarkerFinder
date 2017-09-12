import pyqtgraph as pg
import json
import numpy as np
import pyqtgraph.opengl as gl
from pyqtgraph.Qt import QtCore, QtGui

class plot2D():

    def __init__(self):
        self.app = pg.mkQApp()
        self.view = pg.GraphicsLayoutWidget()
        self.view.show()

        self.window = self.view.addPlot()
        self.c1 = self.window.plot()

    def update(self):
        print 'updating'
        filep = open('data.txt', 'r')
        # data = json.load(filep)['backingArray']
        data = json.load(filep)['byteBuffer']['block']['array']
        filep.close()
        
        self.c1.setData(data)

    def execute(self):
        timer = pg.QtCore.QTimer()
        timer.timeout.connect(self.update)
        timer.start(200)

        self.app.exec_()
    
class plot3D():

    def __init__(self):
        self.app = QtGui.QApplication([])
        self.w = gl.GLViewWidget()
        self.w.opts['distance'] = 20
        self.w.show()

        self.g = gl.GLGridItem()
        self.w.addItem(self.g)

        zeros = np.zeros((400))
        zeros2 = np.zeros((400, 400))
        zeros3 = np.zeros((400, 3))
        self.sp = gl.GLScatterPlotItem(pos = zeros3, color=(255, 255, 255, .3), size=0.3, pxMode=False)
        self.w.addItem(self.sp)

    def update(self):
        print 'Updating'
        filep = open('data.txt', 'r')
        # data = json.load(filep)['backingArray']
        data = json.load(filep)['byteBuffer']['block']['array']
        filep.close()

        data = np.array(data)
        points = np.zeros((len(data) / 3, 3))
        for i in range(3):
            points[:, i] = data[i::3]

        self.sp.setData(pos = points)

    def execute(self):
        timer = pg.QtCore.QTimer()
        timer.timeout.connect(self.update)
        timer.start(200)

        self.app.exec_()

app = plot3D()
app.execute()

#!/usr/bin/python

# Global import
import json
import atexit
from threading import Thread
from Queue import Queue

# Socket imports
import socket
import asyncore

# Plotting imports
import pyqtgraph as pg
import pyqtgraph.opengl as gl
from pyqtgraph.Qt import QtCore, QtGui
import numpy as np

# Socket reading section
class ReadSocket:

    def __init__(self):

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.bind(('10.18.13.26', 6666))
        self.sock.listen(5)

    def runServer(self, q_out):
        print 'Server set up and listening...'

        while True:
            clientSock, addr = self.sock.accept()
            data = ''
            part = None
            while part != '':
                part = clientSock.recv(128).decode().encode('ascii', 'ignore')
                data += part
            ind = data[::-1].index('}')
            ind = len(data) - ind
            data = data[:ind]

            q_out.put(data)

            # filep = open('data.txt', 'w')
            # filep.write(data[:ind])
            # filep.close()
            # print self.jsonDataString
            print 'Data received and saved'

    def handleExit(self):
        print 'Closing Socket...'
        self.sock.close()

class plot3D:

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

        self.timer = pg.QtCore.QTimer()

        self.q_in = None

    def update(self):
        print 'Updating'
        data = json.loads(self.q_in.get())['byteBuffer']['block']['array']

        data = np.array(data)
        points = np.zeros((len(data) / 3, 3))
        for i in range(3):
            points[:, i] = data[i::3]

        self.sp.setData(pos = points)

    def execute(self, q_in):
        self.timer.timeout.connect(self.update)
        self.timer.start(200)
        self.q_in = q_in

        self.app.exec_()

    def handleExit(self):
        print 'Exiting graphing tools...'
        QtGui.QApplication.quit()
        self.timer.stop()
        self.app.closeAllWindows()

if __name__ == '__main__':

    q = Queue()

    # Setting up socket reading process
    readSocket = ReadSocket()
    t = Thread(target = readSocket.runServer, args = (q, ))
    t.daemon = True
    t.start()

    # Setting up plotting process
    app = plot3D()
    app.execute(q)

    atexit.register(readSocket.handleExit())
    atexit.register(app.handleExit())

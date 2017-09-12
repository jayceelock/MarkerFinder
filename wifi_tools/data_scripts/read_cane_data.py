#! /usr/bin/python

import re
import math
import numpy as np
import matplotlib.pyplot as plt

class TangoData:

    def __init__(self):

        self.data_labels = ['timestamp', 'x', 'y', 'z', 'qx', 'qy', 'qz', 'qw', 'distance', 'vibration', 'gain', 'audio_pitch', 'roll', 'pitch', 'yaw']
        self.file_name = '../u1/2016-12-12_11:10:24.csv'
        
        # Initialise the data dictionary
        self.data = {'timestamp': {'axis-title': 'Time [ms]', 'data': []}, 
                     'x': {'axis-title': 'X Position [m]', 'data': []}, 
                     'y': {'axis-title': 'Y Position [m]', 'data': []},
                     'z': {'axis-title': 'Z Position [m]', 'data': []}, 
                     'qx': {'axis-title': 'Q_x Rotation [quaternion]', 'data': []},
                     'qy': {'axis-title': 'Q_y Rotation [quaternion]', 'data': []},
                     'qz': {'axis-title': 'Q_z Rotation [quaternion]', 'data': []},
                     'qw': {'axis-title': 'Q_w Rotation [quaternion]', 'data': []},
                     'gain': {'axis-title': 'Audio Gain [%]', 'data': []},
                     'audio_pitch': {'axis-title': 'Audio Pitch [Hz]', 'data': []},
                     'distance': {'axis-title': 'Distance to Obstacle [m]', 'data': []},
                     'vibration': {'axis-title': 'Vibration Duty Cycle [%]', 'data': []},
                     'roll': {'axis-title': 'Roll Angle [deg]', 'data': []},
                     'pitch': {'axis-title': 'Pitch Angle [deg]', 'data': []},
                     'yaw': {'axis-title': 'Yaw Angle [deg]', 'data': []},
                     }

        self.populate_data(self.file_name)

    def populate_data(self, file_name):

        reader = open(file_name, 'r')

        for line in reader:
            strln = re.sub(r'[^\x20-\x7e]', '', line).split(',')
            intln = [float(strln[x]) for x in range(len(strln) - 1)]        # Strip away trailing comma

            for i in range(len(intln)):
                if intln[0] != 0:
                    # if self.data_labels[i] == 'distance' and intln[i] > 10 and i > 0:     # Fix the 10 000 placeholder that the Tango gives 
                        # intln[i] = intln[i - 1]
                    # self.data_dict[self.data_labels[i]].append(intln[i])
                    self.data[self.data_labels[i]]['data'].append(intln[i])

        labels, angles = self.get_euler_angles()
        i = 0
        for label in labels:
            self.data[label]['data'] = angles[i]
            i += 1

    def get_trajectory_len(self, data, time):
        
        len_tot = 0
        delta = 0

        for i in range(len(data) - 1):
            delta = np.abs(data[i] - data[i + 1])
            len_tot += delta

        return len_tot

    def get_curvature(self, data):

        gradients = -np.gradient(data, edge_order=2)
        radii = []

        for i in range(len(gradients) - 1):
            c1 = data[i] - gradients[i] * 1
            c2 = data[i + 1] - gradients[i + 1] * (1)

            x = (c2 - c1) / (gradients[i] - gradients[i + 1])
            y = gradients[i] * x + c1

            r = math.sqrt(x**2 + y**2)
            radii.append(r)

        return radii

    def get_data(self):

        return self.data

    def get_rot_matrices(self):

        q = [self.data['qx']['data'], self.data['qy']['data'], self.data['qz']['data'], self.data['qw']['data']]

        r = np.zeros((3, 3, len(q[0])))
        
        for i in range(len(q[0])):
            r[0, 0, i] = 1 - 2 * q[1][i] ** 2 - 2 * q[2][i] ** 2
            r[0, 1, i] = 2 * q[0][i] * q[1][i] - 2 * q[2][i] * q[3][i]
            r[0, 2, i] = 2 * q[0][i] * q[2][i] + 2 * q[1][i] * q[3][i]
            r[1, 0, i] = 2 * q[0][i] * q[1][i] + 2 * q[2][i] * q[3][i]
            r[1, 1, i] = 1 - 2 * q[0][i] ** 2 - 2 * q[2][i] ** 2
            r[1, 2, i] = 2 * q[1][i] * q[2][i] - 2 * q[0][i] * q[3][i]
            r[2, 0, i] = 2 * q[0][i] * q[2][i] - 2 * q[1][i] * q[3][i]
            r[2, 1, i] = 2 * q[1][i] * q[2][i] + 2 * q[0][i] * q[3][i]
            r[2, 2, i] = 1 - 2 * q[0][i] ** 2 - 2 * q[1][i] ** 2

        return r

    def get_euler_angles(self):
        
        q = [self.data['qx']['data'], self.data['qy']['data'], self.data['qz']['data'], self.data['qw']['data']]

        a = np.zeros((3, len(q[0])))

        for i in range(len(q[0])):
            a[0, i] = math.degrees(math.atan2(2 * (q[3][i] * q[0][i] + q[1][i] * q[2][i]), 1 - 2 * (q[0][i] ** 2 + q[1][i] ** 2)))
            a[1, i] = math.degrees(math.asin(2 * (q[3][i] * q[1][i] - q[2][i] * q[0][i])))
            a[2, i] = math.degrees(math.atan2(2 * (q[3][i] * q[2][i] + q[0][i] * q[1][i]), 1 - 2 * (q[1][i] ** 2 + q[2][i] ** 2)))

        return ['roll', 'pitch', 'yaw'], a

    def show(self, label, xaxis='timestamp'):

        x = self.data[xaxis]
        y = self.data[label]

        plt.plot(x['data'], y['data'])

        plt.grid()
        plt.xlabel(x['axis-title'])
        plt.ylabel(y['axis-title'])

        plt.show()

if __name__ == '__main__':
    
    tango_data = TangoData()
    data = tango_data.get_data()
    tango_data.get_curvature(data['x']['data'][1:])

    angles = tango_data.get_euler_angles()
    
    tango_data.show('x')


    # plt.plot(data['timestamp'], angles[2, :])
    # plt.show()

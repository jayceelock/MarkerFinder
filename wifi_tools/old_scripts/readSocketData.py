import socket
import json

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.bind(('10.18.13.26', 6666))
sock.listen(5)

print 'Server set up and listening...'

while True:
    clientSock, addr = sock.accept()
    data = ''
    part = None
    while part != '':
        part = clientSock.recv(128).decode().encode('ascii', 'ignore')
        data += part
    ind = data[::-1].index('}')
    ind = len(data) - ind
    filep = open('data.txt', 'w')
    filep.write(data[:ind])
    filep.close()
    # json.dump(data[:ind], open('data.txt', 'w'))

    print 'Data received and saved'

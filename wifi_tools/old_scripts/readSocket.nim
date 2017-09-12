import net, json, system, times, posix

const port: int = 6666

var
  is_running: bool = true
  socket: Socket = newSocket()

proc c_signal(sig: cint, handler: proc (a: cint) {.noconv.}) {.importc: "signal", header: "<signal.h>".}

proc at_exit(sig: cint) {.noconv.} =
  close(socket)
  is_running = false
  echo "Signal. Port closed."
  quit()

c_signal(SIGINT, at_exit)

proc getFileName(): string = 
  
  var fileName = getDateStr()

  fileName = fileName & "_" & getClockStr()

  return fileName & ".csv"

proc main() = 

  var 
    clientSocket: Socket = newSocket()

    readLineSuccess: int = 0
    clientAddr: string = ""
    readString: string = ""
    part: string = " "

    file = open(getFileName(), fmWrite)

  socket.bindAddr(Port(port))
  socket.listen()

  while is_running:
    socket.acceptAddr(clientSocket, clientAddr)
    echo "Connected from ", clientAddr

    readString = ""
    part = "0"

    while part != "":
      readLineSuccess = recv(clientSocket, part, size=1024)
      readString = readString & part

    file.write(readString)
    echo "Wrote ", readString

  file.close()

main()

This project was created with the goal of reading all available OBD data from all available OBD sources. The initial implementation of the code has been tested against a wifi OBD2 adapter plugged into a Nissan Xterra (green, dirty) as well as against obdsim (https://icculus.org/obdgpslogger/obdsim.html). The data will be written to local storage in either text or the original compressed format, pushed to an Amazon s3 bucket, and then analyzed via various projects within the Hadoop ecosystem (see other repos).

To run tests:
mvn clean verify (-Pserial) (-Psocket) (-Dtest.serial.port=<serialport>) (-Dtest.socket.host=<sockethost>) (-Dtest.socket.port=<socketport>)

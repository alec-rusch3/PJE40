# PJE40

To run the MPJ code

get ip of all machines to be used
ipconfig in cmd, or search ip in google

edit machines file located in 825804\build\classes, add all the ip one row each
the first ip is the machine the display will be on

locate the mpj.bat file located in 825804\
change the following line:
  cd /D E:\program\825804\build\classes

to the directory program is saved in

run the batch file, then enter mpjdaemon
repeat above step on every machine being used

run the batch file again on any pc then run program using following command
 
 mpjrun -np x -dev niodev MPJ.BarnesHutMPJ

change x to desired number of threads

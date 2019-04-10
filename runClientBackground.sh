#!/bin/bash
if [ $# -eq 1 ]; then 
	serverip=$1

else
	echo "Server ip was not supplied.. Kindly enter the server ip --- "
	read serverip
fi

printf "Please enter the amount of time (in hours) you need to synchronize for\n Common Examples....
		0.0166667 for 1 minutes
		0.16667 for 10 minutes
		0.5 for 30 minutes\n "
printf "Enter the time and press [Enter] "
read time

nohup java -jar target/simple-clock-synchronization-1.0-SNAPSHOT-client.jar $serverip $time > clientTime.log & 

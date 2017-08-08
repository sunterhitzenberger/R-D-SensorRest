#!/bin/bash
# check if restart system file exists
{
if [-f ./RestartGateway];
then
	echo "restart gateway"
	pkill -f 'java.*SensorRest*'
	pkill -f 'java.*gateway*'
fi
}

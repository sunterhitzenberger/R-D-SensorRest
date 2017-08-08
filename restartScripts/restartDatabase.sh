#!/bin/bash
# check if restart system file exists
{
if [-f ./RestartDatabase];
then
	echo "restart database"
	pkill -f 'java.*h2-latest'
fi
}
#!/bin/bash
# check if restart system file exists
{
if [-f ./RestartSystem];
then
	echo "restart system"
	shutdown -r 0
fi
}
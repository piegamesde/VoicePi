#!/bin/bash

if ping -c 1 google.com >> /dev/null 2>&1; then
	echo "You are online"
else
	echo "You are not online"
fi


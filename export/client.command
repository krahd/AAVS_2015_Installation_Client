#!/bin/sh
cd -- "$(dirname "$BASH_SOURCE")"
while true 
do
	java -jar Client.jar -D ./Client_lib
done

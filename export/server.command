#!/bin/sh
cd -- "$(dirname "$BASH_SOURCE")"
while true 
do
	java -jar Server.jar -D ./Server_lib
done

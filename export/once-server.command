#!/bin/sh
cd -- "$(dirname "$BASH_SOURCE")"
java -jar Server.jar -D ./Server_lib

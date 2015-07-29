#!/bin/sh
cd -- "$(dirname "$BASH_SOURCE")"
java -jar Client.jar -D ./Client_lib

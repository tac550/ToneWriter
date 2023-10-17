#!/bin/bash
tail --pid="$1" -f /dev/null
unzip -o "$2" -d "$3"
"$3/lin/ToneWriter.sh"
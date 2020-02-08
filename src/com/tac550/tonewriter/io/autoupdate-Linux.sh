#!/bin/bash
tail --pid="$1" -f /dev/null
rm -rf "$3/ToneWriter.app"
unzip -o "$2" -d "$3"
"$3/lin/ToneWriter.sh"
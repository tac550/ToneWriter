#!/usr/bin/env bash
caffeinate -w "$1"
rm -rf "$3/ToneWriter.app"
unzip -o "$2" -d "$3"
open "$3/ToneWriter.app"
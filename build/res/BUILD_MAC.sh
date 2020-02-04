#!/usr/bin/env bash
mkdir ../mac/ & \
rm -rf ../mac/ToneWriter.app & \
rm -rf ../mac/ToneWriter.app.zip & \
rm -rf java-runtime & \
jlink --no-header-files --no-man-pages --compress=2 --strip-debug --module-path javafx-jmods-11.0.2 \
--add-modules java.xml,java.scripting,java.desktop,jdk.unsupported,javafx.controls,javafx.fxml,javafx.web,\
java.naming,jdk.charsets,jdk.crypto.ec,java.sql --output java-runtime/Contents/Home/jre && \
jar2app ToneWriter.jar -n "ToneWriter" -d "ToneWriter" -i TWIcon.icns -b com.tac550.tonewriter \
-v 0.5 -s 0.5 ../mac/ToneWriter.app -r java-runtime && \
cp -a '../../Built-in Tones' '../mac/ToneWriter.app/Contents/Built-in Tones' && \
cp -a '../../licenses' '../mac/ToneWriter.app/Contents/licenses' && \
cp -a '../../lilypond' '../mac/ToneWriter.app/Contents/lilypond' && \
cd ../mac/ && \
zip -r ToneWriter0.5.app.zip ToneWriter.app

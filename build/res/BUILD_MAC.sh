#!/usr/bin/env bash
rm -rf ../mac/ToneWriter.app & \
rm -rf ../mac/ToneWriter.app.zip & \
rm -rf java-runtime & \
/Library/Java/JavaVirtualMachines/jdk-11.0.2.jdk/Contents/Home/bin/jlink \
--no-header-files --no-man-pages --compress=2 --strip-debug --module-path javafx-jmods-11.0.2 \
--add-modules java.xml,java.scripting,java.desktop,jdk.unsupported,javafx.controls,javafx.fxml,\
java.naming,jdk.charsets,jdk.crypto.ec,java.sql --output java-runtime/Contents/Home/jre && \
jar2app ToneWriter.jar -n "ToneWriter" -d "ToneWriter" -i TWIcon.icns -b com.tac550.tonewriter \
-v 0.4 -s 0.4 ../mac/ToneWriter.app -r java-runtime && \
cp -a '../../Built-in Tones' '../mac/ToneWriter.app/Contents/Built-in Tones' && \
cp -a '../../licenses' '../mac/ToneWriter.app/Contents/licenses' && \
cd ../mac/ && \
zip -r ToneWriter.app.zip ToneWriter.app

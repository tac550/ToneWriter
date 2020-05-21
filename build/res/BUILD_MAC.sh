#!/usr/bin/env bash
mkdir ../mac/ & \
rm -rf ../mac/ToneWriter.app & \
rm -rf ../mac/ToneWriter*.app.zip & \
rm -rf java-runtime & \
jlink --no-header-files --no-man-pages --compress=2 --strip-debug --module-path javafx-jmods* \
--add-modules java.xml,java.scripting,java.desktop,java.management,jdk.unsupported,javafx.controls,javafx.fxml,javafx.web,\
java.naming,jdk.charsets,jdk.crypto.ec,java.sql --output java-runtime/Contents/Home/jre && \
jar2app ToneWriter.jar -n "ToneWriter" -d "ToneWriter" -i TWIcon.icns -b com.tac550.tonewriter -j "-Xms256m -Xmx4096m" \
-v 0.6 -s 0.6 ../mac/ToneWriter.app -r java-runtime && \
cp -a '../../Built-in Tones' '../mac/ToneWriter.app/Contents/Built-in Tones' && \
cp -a '../../licenses' '../mac/ToneWriter.app/Contents/licenses' && \
cp -a '../../lilypond' '../mac/ToneWriter.app/Contents/lilypond' && \
cd ../mac/ && \
zip -r ToneWriter0.6.app.zip ToneWriter.app

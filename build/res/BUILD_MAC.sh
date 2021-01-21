#!/usr/bin/env bash
mkdir ../mac/ & \
rm -rf ../mac/ToneWriter.app & \
rm -rf ../mac/ToneWriter*.app.zip & \
rm -rf java-runtime & \
jlink --no-header-files --no-man-pages --compress=2 --strip-debug --module-path javafx-jmods* \
--add-modules java.xml,java.scripting,java.desktop,java.management,jdk.unsupported,javafx.controls,javafx.fxml,javafx.web,\
java.naming,jdk.charsets,jdk.crypto.ec,java.sql --output java-runtime/ && \
jpackage --name ToneWriter --input jar/ --main-jar tonewriter.jar --java-options "--enable-preview -Xms256m -Xmx4096m -Duser.dir=\$ROOTDIR/Contents" \
--icon TWIcon.icns --runtime-image java-runtime/ --type app-image --app-version 0.9 --dest ../mac/ && \
cp -a '../../Built-in Tones' '../mac/ToneWriter.app/Contents/Built-in Tones' && \
cp -a '../../licenses' '../mac/ToneWriter.app/Contents/licenses' && \
cp -a '../../lilypond' '../mac/ToneWriter.app/Contents/lilypond' && \
cd ../mac/ && \
zip -r ToneWriter0.9.app.zip ToneWriter.app

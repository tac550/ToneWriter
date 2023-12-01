#!/bin/bash
rm -rf ../lin && \
mkdir -p ../lin && \
mkdir -p ../lin/jar/ && \
jlink --no-header-files --no-man-pages --compress=2 --strip-debug --module-path javafx-jmods* \
--add-modules java.xml,java.scripting,java.desktop,java.management,jdk.unsupported,javafx.controls,javafx.fxml,javafx.web,\
java.naming,jdk.charsets,jdk.crypto.ec,java.sql --output ../lin/java-runtime && \
cp jar/tonewriter.jar ../lin/jar/ && \
cp -r "../../Built-in Tones" "../lin/Built-in Tones" && \
cp -a "../../lilypond" "../lin/lilypond" && \
cp -r ../../licenses ../lin/licenses && \
cp ToneWriter.sh ../lin && \
cd .. && \
zip -r lin/ToneWriter1.4.0-Linux.zip lin/*

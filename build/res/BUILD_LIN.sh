rm -rf ../lin & \
mkdir -p ../lin && \
mkdir -p ../lin/jar && \
jlink --no-header-files --no-man-pages --compress=2 --strip-debug --module-path javafx-jmods-11.0.2 --add-modules java.xml,java.scripting,java.desktop,jdk.unsupported,javafx.controls,javafx.fxml,java.naming,jdk.charsets,jdk.crypto.ec,java.sql --output ../lin/java-runtime && \
cp tonewriter.jar ../lin/jar/tonewriter.jar && \
cp -r "../../Built-in Tones" "../lin/Built-in Tones" && \
cp -r ../../licenses ../lin/licenses && \
cp ToneWriter.sh ../lin && \
zip -r -9 ../lin/ToneWriter0.5-Linux.zip ../lin/*
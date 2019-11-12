rmdir /s /q java-runtime & ^
jlink --no-header-files --no-man-pages --compress=2 --strip-debug --module-path javafx-jmods* ^
--add-modules java.xml,java.scripting,java.desktop,jdk.unsupported,javafx.controls,javafx.fxml,^
java.naming,jdk.charsets,jdk.crypto.ec,java.sql --output java-runtime && ^
launch4jc TWBuild_WIN.xml && ^
rd /s /q "..\win\Built-in Tones" & ^
rd /s /q "..\win\licenses" & ^
xcopy /s /i /y "..\..\Built-in Tones" "..\win\Built-in Tones" && ^
xcopy /s /i /y "..\..\licenses" "..\win\licenses" && ^
xcopy /s /i /y "..\..\lilypond" "..\win\lilypond" && ^
xcopy /s /i /y "java-runtime" "..\win\java-runtime" && ^
makensis nsi_WIN.nsi
pause
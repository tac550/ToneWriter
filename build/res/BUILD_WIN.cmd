where launch4j.jar > path.txt && ^
rd /s /q java-runtime & ^
rd /s /q ..\win\ & ^
mkdir ..\win\ & ^
jlink --no-header-files --no-man-pages --compress=2 --strip-debug --module-path javafx-jmods* ^
--add-modules java.xml,java.scripting,java.desktop,java.management,jdk.unsupported,javafx.controls,javafx.fxml,javafx.web,^
java.naming,jdk.charsets,jdk.crypto.ec,java.sql --output java-runtime && ^
for /f "delims=" %%x in (path.txt) do set jar_path=%%x
java -jar "%jar_path%" TWBuild_WIN.xml & ^
del path.txt & ^
rd /s /q "..\win\Built-in Tones" & ^
rd /s /q "..\win\licenses" & ^
rd /s /q "..\win\lilypond" & ^
xcopy /s /i /y "..\..\Built-in Tones" "..\win\Built-in Tones" && ^
xcopy /s /i /y "..\..\licenses" "..\win\licenses" && ^
xcopy /s /i /y "..\..\lilypond" "..\win\lilypond" && ^
xcopy /s /i /y "java-runtime" "..\win\java-runtime" && ^
makensis nsi_WIN.nsi
pause
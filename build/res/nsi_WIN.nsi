; Script generated with the Venis Install Wizard

; Define your application name
!define APPNAME "ToneWriter"
!define APPNAMEANDVERSION "ToneWriter 0.5"

; Main Install settings
Name "${APPNAMEANDVERSION}"
InstallDir "$PROGRAMFILES\ToneWriter"
InstallDirRegKey HKLM "Software\${APPNAME}" ""
OutFile "..\win\ToneWriter0.5_Setup.exe"

; Modern interface settings
!include "MUI.nsh"
; File association
!include "FileAssociation.nsh"

!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN "$INSTDIR\ToneWriter.exe"

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

; Set languages (first is default language)
!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_RESERVEFILE_LANGDLL

Section "ToneWriter" Section1

	; Set Section properties
	SectionIn RO
	SetOverwrite on

	; Set Section Files and Shortcuts
	SetOutPath "$INSTDIR\"
	File "..\win\ToneWriter.exe"
	SetOutPath "$INSTDIR\licenses\"
	File "..\win\licenses\third-party-licenses.txt"
	CreateShortCut "$DESKTOP\ToneWriter.lnk" "$INSTDIR\ToneWriter.exe"
	CreateDirectory "$SMPROGRAMS\ToneWriter"
	CreateShortCut "$SMPROGRAMS\ToneWriter\ToneWriter.lnk" "$INSTDIR\ToneWriter.exe"
	CreateShortCut "$SMPROGRAMS\ToneWriter\Uninstall ToneWriter.lnk" "$INSTDIR\uninstall.exe"
	; Delete any existing Java runtime
    RMDir /r "$INSTDIR\java-runtime\"
    ; Copy new Java runtime
    SetOutPath "$INSTDIR\java-runtime\"
    File /nonfatal /a /r "..\win\java-runtime\"

	${registerExtension} "$INSTDIR\ToneWriter.exe" ".tone" "TONE File"

SectionEnd

Section "Built-in Tones" Section2

	; Set Section properties
	SetOverwrite on

	; Delete any existing builtins
	RMDir /r "$INSTDIR\Built-in Tones\"

	; Set Section Files and Shortcuts
	SetOutPath "$INSTDIR\Built-in Tones\"
	File /nonfatal /a /r "..\win\Built-in Tones\"

SectionEnd

Section -FinishSection

	WriteRegStr HKLM "Software\${APPNAME}" "" "$INSTDIR"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${APPNAME}"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$INSTDIR\uninstall.exe"
	WriteUninstaller "$INSTDIR\uninstall.exe"

SectionEnd

; Modern install component descriptions
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
	!insertmacro MUI_DESCRIPTION_TEXT ${Section1} "The ToneWriter application itself."
	!insertmacro MUI_DESCRIPTION_TEXT ${Section2} "Built-in tone data to get you started engraving sheet music quickly. Includes stichera melodies from the L’vov/Bakhmetev Common Chant and B. Ledkovsky Kievan Chant systems."
!insertmacro MUI_FUNCTION_DESCRIPTION_END

;Uninstall section
Section Uninstall

	;Remove from registry...
	DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
	DeleteRegKey HKLM "SOFTWARE\${APPNAME}"

	; Delete self
	Delete "$INSTDIR\uninstall.exe"

	; Delete Shortcuts
	Delete "$DESKTOP\ToneWriter.lnk"
	Delete "$SMPROGRAMS\ToneWriter\ToneWriter.lnk"
	Delete "$SMPROGRAMS\ToneWriter\Uninstall ToneWriter.lnk"
	; OLD
	Delete "$SMPROGRAMS\ToneWriter\Uninstall.lnk"

	; Clean up ToneWriter
	Delete "$INSTDIR\ToneWriter.exe"
	Delete "$INSTDIR\licenses\third-party-licenses.txt"

	; Clean up Built-in Tones
	RMDir /r "$INSTDIR\Built-in Tones\"
	; Clean up Java runtime
	RMDir /r "$INSTDIR\java-runtime\"

	; Remove remaining directories
	RMDir "$SMPROGRAMS\ToneWriter"
	RMDir "$INSTDIR\licenses\"
	RMDir "$INSTDIR\"

	${unregisterExtension} ".tone" "TONE File"

SectionEnd

; eof
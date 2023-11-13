# ToneWriter

[Project Site](https://github.com/tac550/ToneWriter) | [Download](https://github.com/tac550/ToneWriter/releases)

ToneWriter is an application which makes it easy to create sheet music for repeating chant melodies like those used in Orthodox Christian worship. This type of music is typically unmetered and features long stretches of recitative. For these reasons and others, it is a struggle to set this music in Western staff notation using conventional digital engraving software like Finale and MuseScore. ToneWriter makes it much easier to engrave this chant music on desktop computers. See the example below:

ToneWriter lets you assign chords to syllables...
![image](https://github.com/tac550/ToneWriter/assets/9357248/15e542da-3eed-4c72-9262-9481d4f83e28)
...to produce sheet music with a single click:
![image](https://github.com/tac550/ToneWriter/assets/9357248/cae57fe9-7913-44fa-b488-032999560e13)

ToneWriter works by abstracting the chant melodies and harmonizations away from the particular text being used. The user defines how to map any given melody to any given text, and ToneWriter automatically generates PDF sheet music. Users may define their own chant melodies or use any of those that come built into the software. Users then enter the text to be used, which is automatically broken into syllables. Mapping the chant melodies to each line of text is as simple as clicking on the syllable(s) to which each chord will apply. The application outputs printable sheet music in PDF format, with all the stylistic peculiarities of this type of music automatically applied.

## Contributing

Please feel free to fork this repo and send in pull requests! I'm happy to accept anything that makes the project better (from new features to improving the overall structure to fixing my broken code). If you want to contribute but are not sure what to do, I would suggest working on improving our built-in tones if you are familiar with them ([this](https://oca.org/liturgics/learning-the-tones) is a useful resource for Ledkovsky Kievan Chant and L'vov/Bakhmetev Common Chant stichera), or adding new ones if your own musical tradition is not yet represented. By default, built-in tones can not be overwritten within the application, but this can be enabled by running with the VM argument "-DdeveloperMode=true".

### Wishlist

 - I'm looking for someone to help make the app better-suited to Byzantine and other monophonic chant systems. At the moment things are designed primarily to work with SATB voicing. Someone more knowledgeable about other chant styles would do a better job of adding support for them than I would.

## Building / Debugging

Place official LilyPond binaries in a directory named `lilypond` at the root of the project.
The binary gets included automatically by the packaging scripts for distribution.

I'm building ToneWriter against Java 21 on all platforms. Recommended IDE is IntelliJ IDEA. I use Gluon SceneBuilder to edit the .fxml interface files. This project uses Maven for dependency management and includes an IDEA run configuration for debugging.

## Packaging

### Windows

dependencies: [launch4j 3.14 or later](http://launch4j.sourceforge.net/) (make sure its install directory is on the PATH) and [NSIS](https://sourceforge.net/projects/nsis/) (make sure its install directory is on the PATH). The JDK 21 bin directory must also be on the PATH. The JavaFX SDK 21 jmods directory must be located at build/res to be able to build the portable Java runtime. Those are available [here](https://gluonhq.com/products/javafx/).

To build a Windows installer executable as you see in release binaries, run the "package" Maven build phase and then run build/res/BUILD_WIN.cmd. The installer will be placed in build/win.

### macOS

dependencies: Your JDK 21 bin directory must be on the PATH. The JavaFX SDK 21 jmods directory must be located in the build/res directory to be able to build the portable Java runtime. Those are available [here](https://gluonhq.com/products/javafx/).

To build a .app as you see in release binaries, first remove the exclusions for the "autoupdate-macOS.sh" and "tryfix-LilyPond-macOS.sh" resource files in pom.xml. Then run the "package" Maven build phase and then run build/res/BUILD_MAC.sh. The .app will be placed in build/mac along with a zipped copy.

### Linux

dependencies: Your JDK 21 bin directory must be on the PATH. The JavaFX SDK 21 jmods directory must be located in the build/res directory to be able to build the portable Java runtime. Those are available [here](https://gluonhq.com/products/javafx/).

First remove the exclusion for the "autoupdate-Linux.sh" resource file in pom.xml. Then run the "package" Maven build phase and then run build/res/BUILD_LIN.sh. The resulting ToneWriter.sh script runs the packaged application.

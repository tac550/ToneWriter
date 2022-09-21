# ToneWriter

[Project Site](https://github.com/tac550/ToneWriter) | [Download](https://github.com/tac550/ToneWriter/releases)

ToneWriter is a JavaFX desktop application designed to make it easy to create sheet music for various (and user-definable) chant melodies like those used in Orthodox Christian worship. This type of music is typically unmetered and features repeating melodies and long stretches of recitative. These characteristics allow a wide variety of texts to be set to the music, but are unusual in the wider musical world, and thus are not well-supported by conventional WYSIWYG engravers like Finale and MuseScore. LilyPond provides the control needed to engrave this kind of music a little more easily, but the task is still laborious and the learning curve is steep.

ToneWriter attempts to solve this problem by abstracting the chant melodies themselves away from the particular text being used. The user can then easily define how to map the melody to any given text and automatically generate PDF sheet music in a graphical interface. Users may create their own chant data (commmonly called "tones" in church parlance) or use a tone that comes built into the software. Users then enter the text to be used, which is automatically broken into syllables. Mapping the chant melodies to each line of text is as simple as clicking on the syllable(s) to which each chord will apply. The application outputs printable sheet music in PDF format, with all the stylistic particularities of this type of music automatically applied.

## Contributing

Please feel free to fork this repo and send in pull requests! I'm happy to accept anything that makes the project better (from new features to improving the overall structure to fixing my broken code). If you want to contribute but are not sure what to do, I would suggest working on improving our built-in tones if you are familiar with them ([this](https://oca.org/liturgics/learning-the-tones) is a useful resource for Ledkovsky Kievan Chant and L'vov/Bakhmetev Common Chant stichera), or adding new ones if your own musical tradition is not yet represented. By default, built-in tones can not be overwritten within the application, but this can be enabled by running with the VM argument "-DdeveloperMode=true".

### Wishlist

 - I'm looking for someone to help make the app better-suited to Byzantine and other less-polyphonic tone systems. At the moment things are designed primarily to work with SATB voicing because that is what I'm most familiar with. Someone more knowledgeable about other chant styles would do a better job of adding support for them than I would.

## Building / Debugging

You need to have [LilyPond](http://lilypond.org/) installed for most features of ToneWriter to work. Version 2.20.x or greater is required.

I'm building ToneWriter against Java 18 on all platforms. This repo contains project files for both Eclipse and IDEA. I use Gluon SceneBuilder to edit the .fxml interface files. You also need to download the JavaFX SDK from Gluon and point to it properly in your VM options in order to debug/run the application. My VM options look like this: "--module-path ${PATH_TO_FX} --add-modules=javafx.controls,javafx.fxml,javafx.web -Xms256m -Xmx4096m". Make sure PATH_TO_FX is defined as the "lib" directory within the JavaFX SDK.

## Packaging

### Windows

dependencies: [launch4j 3.14 or later](http://launch4j.sourceforge.net/) (make sure its install directory is on the PATH) and [NSIS](https://sourceforge.net/projects/nsis/) (make sure its install directory is on the PATH). The JDK 18 bin directory must also be on the PATH. The JavaFX SDK 18 jmods directory must be located at build/res to be able to build the portable Java runtime. Those are available [here](https://gluonhq.com/products/javafx/).

To build a Windows installer executable as you see in release binaries, export a runnable JAR file named "ToneWriter.jar" (whose main class is MainApp) to build/res and run build/res/BUILD_WIN.cmd. An artifact definition for ToneWriter.jar is included with the IDEA project files. If not using IDEA's artifact builder, be sure to configure the compiler to exclude the .sh script files found in the classpath. The installer will be placed in build/win.

### macOS

dependencies: Your JDK 18 bin directory must be on the PATH. The JavaFX SDK 18 jmods directory must be located in the build/res directory to be able to build the portable Java runtime. Those are available [here](https://gluonhq.com/products/javafx/).

To build a .app as you see in release binaries, export a runnable JAR file named "ToneWriter.jar" (whose main class is MainApp) to build/res and run build/res/BUILD_MAC.sh. An artifact definition for ToneWriter.jar is included with the IDEA project files. Before building it, be sure to remove the compiler exclusions for any script files in IDEA's Settings panel. The .app will be placed in build/mac along with a zipped copy.

### Linux

dependencies: Your JDK 18 bin directory must be on the PATH. The JavaFX SDK 18 jmods directory must be located in the build/res directory to be able to build the portable Java runtime. Those are available [here](https://gluonhq.com/products/javafx/).

Export a runnable JAR file named "ToneWriter.jar" (whose main class is MainApp) to build/res and run build/res/BUILD_LIN.sh. An artifact definition for ToneWriter.jar is included with the IDEA project files. Before building it, be sure to remove the compiler exclusions for any script files in IDEA's Settings panel. The resulting ToneWriter.sh script runs the packaged application.

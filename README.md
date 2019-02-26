# ToneWriter

ToneWriter is a JavaFX desktop application designed to make it easy to render sheet music for arbitrary chant melodies like those used in Orthodox Christian worship. This type of music is typically unmetered and relatively repetitive, with repeating melodies and long stretches of recitative. These characteristics allow a wide variety of texts to be set to the music, but are unusual in the wider musical world, and thus are not well-supported by conventional WYSIWYG engravers like Finale and MuseScore. LilyPond gives us the control we need to engrave this kind of music a little more easily, but the task is still laborious and the learning curve is steep.

ToneWriter attempts to solve this problem by abstracting the chant melodies themselves away from the particular text being used, allowing the user to easily define how to map the melody to any given text, and doing all of this in a graphical interface. Users may create their own chant data (commmonly called "tones" in church parlance) or use a tone that comes built into the software. Users then enter the text to be used, which is automatically broken into syllables. Mapping the chant melodies to each line of text is as simple as clicking on the syllable(s) to which each chord will apply. The rest is done completely automatically.

## Contributing

Please feel free to fork this repo and send in pull requests! I'm happy to accept anything that makes the project better (from new features to improving the overall structure to fixing my broken code). I only ask that you be considerate of the style conventions you see in the code, and if any of them seem detrimental to you, let me know and we'll see if we should change them unilaterally. If you want to contribute but are not sure what to do, I would suggest working on improving our built-in tones if you are familiar with them ([this](https://oca.org/liturgics/learning-the-tones) is a useful resource for Ledkovsky Kievan Chant and L'vov/Bakhmetev Common Chant stichera), or adding new ones if your own musical tradition is not yet represented.

### Wishlist

 - I'm looking for someone to help make the app better-suited to Byzantine and other less-polyphonic tone systems. At the moment things are designed primarily to work with SATB voicing because that is what I'm most familiar with. Someone more knowledgeable about other chant styles would do a better job of adding support for them than I would.

## Building

You need to have [LilyPond](http://lilypond.org/) installed for most features of ToneWriter to work. On Windows the latest stable version (2.18.x at time of writing) should be fine. On MacOS, however, you need at least a recent version of 2.19 because older versions have bugs rendering PNG files for our chord previews. Also, if you use MacOS, make sure you engrave something with LilyPond (such as the sample file it comes with) BEFORE running ToneWriter. For some reason the first time LilyPond engraves anything it takes a very long time and this can really clog your system when we try rendering a couple dozen chord previews at once.

I'm building ToneWriter against Java 8 which is the minimum supported version. I don't see why things shouldn't work on newer versions but I have not tested extensively. This repo contains project files for both Eclipse and Idea, so importing the project into either of those IDEs should be easy. I recommend Gluon SceneBuilder for editing the .fxml interface files.

### Windows

To build a Windows installer executable as you see in release binaries, export a runnable JAR file named "ToneWriter.jar" (which executes the main() function in MainApp.java) to build/res and run build/res/BUILD_WIN.cmd. The installer will be placed in build/win.

There are 2 dependencies: [launch4j](http://launch4j.sourceforge.net/) (make sure launch4jc.exe is on the PATH) and [NSIS](https://sourceforge.net/projects/nsis/) (make sure makensis.exe is on the PATH)

### MacOS

To build a .app as you see in release binaries, export a runnable JAR file named "ToneWriter.jar" (which executes the main() function in MainApp.java) to build/res and run build/res/BUILD_MAC.sh. The .app will be placed in build/mac along with a zipped copy.

There is 1 dependency: jar2app, installed as described [here](https://github.com/Jorl17/jar2app).

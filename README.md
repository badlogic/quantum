Quantum
=======
![screenshot](https://raw.github.com/yndi/quantum/master/screen.png "Game process")

### [Download](http://libgdx.badlogicgames.com/downloads/quantum.zip)

Quantum is a realtime strategy in the spirit of Galcon and Eufloria. You are in control of creatures that orbit planets. You can colonize new planets by moving creatures to them. To create new Creatures you have to build trees on the planets. The new born creatures will then inherit certain properties from their home planet. The goal of the game is it to eliminate all enemy creatures and overtake all their planets.

The game was originally inspired by Dyson/Eufloria. I got in contact with the authors back then, offering to implement multiplayer. They politely refused and allowed me to write this clone with multiplayer.

**Warning: the code is 6 years old and does not represent best practices concerning OpenGL or writting a game in general**

That being said, you may still find it interesting as it has the following features:

* Sccriptable bots using BeanShell. See [dat/scripts/simplebot.bsh](dat/scripts/simplebot.bsh) which contains all of the AI code :)
* Multiplayer using variable lock-step simulation. Works very well, even on high-latency networks.
* A level editor.
* Terrible rendering engine, do not imitate this!

## Building
You will need to install

* A JDK
* Maven (`brew install maven`, `apt-get install maven`, or manual installation on Windows)
* Make sure javac and Maven are in your `PATH`

Once you are ready, do this in the root directory:

```
mvn clean install
```

This will create a file `quantum.zip` in the `target/` directory. It's composed of the main game jar `quantum.jar`, contains all 3rd party jars in `lib/` and all game data in `dat/`.

You can also import the project as a Maven project into Eclipse, IntelliJ IDEA or NetBeans.

## Running
Extract the zip created in the build step, then in the root directory:

```
java -jar quantum.jar
```

Alternatively you can double click the JAR in your OS' file explorer.

You can also run the game from within your IDE. Simply set the class `Quantum` as the main class.



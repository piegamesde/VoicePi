# PiControl
An application made to control the Raspberry Pi with voice commands. Whilst targeting the Pi, the cross-platform nature of Java should make it run on about every system.

## Features
- Built-in and set-up voice recognition (STT) and speech synthesizer (TTS)
- No external dependencies (except for Java 8)
- Because of this, it should run out of the box without any problems
- Support for custom STT and TTS other than the pre-installed ones that is easy to set up. It is even compatible to shell piping.
- Module-based system for easy configuration and extension
- Context based commands allow for advanced communication with the modules

## Caveeats
- All possible commands must be registered at loading time
- The built-in speech recognizer (Sphinx) works offline and thus has a lower recognition quality. It needs an internet connection to compile the language model though.
- Modules have to be written in Java. No simple scripting, sorry. But if you ask nicely, I'll consider adding in a Lua API.

## Alternatives

- Jasper
- Blather

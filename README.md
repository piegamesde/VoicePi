# VoicePi - Voice control for your Raspberry Pi (And any other device)
An application made to control the Raspberry Pi with voice commands. Whilst targeting the Pi, the cross-platform nature of Java should make it run on about every system. Its primary goals are the ability to run out of the box without any problems and the ease of configuring the wanted functionality.

## Features
- Built-in and already configured voice recognition (STT) and speech synthesizer (TTS)
- Context based commands make advanced communication with the application possible
- No external dependencies (except Java 8)
- Because of this, it should run out of the box without any problems on any system
- Support for custom STT and TTS other than the pre-installed ones that is easy to set up. It is even compatible to shell piping.
- Module-based system for easy configuration and extension

## Caveeats
- All possible commands must be registered at loading time
- The built-in speech recognizer (Sphinx) works offline and thus has a lower recognition quality. It needs an internet connection to compile the language model though (but only the first time you run with that configuration)
- Modules (and other extensions) have to be written in Java. No simple scripting, sorry. But in most of the cases you won't need it at all

## Getting started

See the [wiki](https://github.com/piegamesde/VoicePi/wiki)

## Alternatives

- [Jasper](https://github.com/jasperproject/jasper-client)
- [Blather](https://github.com/ajbogh/blather)

Both alternatives are written in Python. Jasper has support for a lot more different TTS and STT engines and modules like VoicePi, but no context-based communication. Blather is a lot simpler and more restricted, but is easier to set up than Jasper and is still powerful enough for many use-cases.

## Features to come

- Support for more STT and TTS engines, especially the online ones from the big players (Google, Amazon, etc.). Technically you can already configure it, but it hasn't been tested yet.
- Mixing STT and TTS engines. This will allow for using multiple input and output sources and even switch between them depending on the context: Sphinx for the activation command, Google for the rest. (WIP)
- More examples, more pre-configured things for you to plug in. On the list: More music systems (Spotify, Gnome-music, ...)
- Control through the command line (WIP)
- A proper log system
- Passive modules which monitor things in the background and activate themselves if something happens. This is a cool feature Jasper has and VoicePi should get it too one day. (WIP)
- Make it less resource intensive and run faster on the Raspberry Pi

## Contributing

### Without coding
Use the application and report any issues and feature suggestion you have. Tell us how you use it and how it can be improved. If you configured a module in a way you think it might be helpful to others upload it in the Examples section TODO.

### With coding
Clone this repository and import it into Eclipse. Grab one issue you absolutely want fixed or the feature you want the most and start working. Before you start, you should read the wiki carefully: at the bottom of most articles there is some developer documentation.

If you find a piece of undocumented code that isn't obvious to you, please add a comment telling what it does once you figured out.

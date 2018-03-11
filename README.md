# VoicePi
An application made to control the Raspberry Pi with voice commands. Whilst targeting the Pi, the cross-platform nature of Java should make it run on about every system.

## Features
- Built-in and set-up voice recognition (STT) and speech synthesizer (TTS)
- Context based commands make advanced communication with the application possible
- No external dependencies (except Java 8)
- Because of this, it should run out of the box without any problems
- Support for custom STT and TTS other than the pre-installed ones that is easy to set up. It is even compatible to shell piping.
- Module-based system for easy configuration and extension

## Caveeats
- All possible commands must be registered at loading time
- The built-in speech recognizer (Sphinx) works offline and thus has a lower recognition quality. It needs an internet connection to compile the language model though
- Modules have to be written in Java. No simple scripting, sorry. But in most of the cases you won't need it at all

## Getting started

See the wiki TODO link

## Alternatives

- Jasper
- Blather

Both alternatives are written in Python. Jasper has support for a lot more different TTS and STT engines and modules like VoicePi, but no context-based communication. Blather is a lot simpler and more restricted, but is easier to set up than Jasper and probably is more than enough.

## Contributing

### Without coding
Use the application and report any issues and feature suggestion you have. Tell us how you use it and how it can be improved. If you configured a module in a way you think it might be helpful to others upload it in the Examples section TODO.

### With coding
Clone this repository and import it into Eclipse. Grab one issue you absolutely want fixed or the feature you want the most and start working. Before you start, you should read the wiki carefully: at the bottom of most articles there is some developer documentation.

If you find a piece of undocumented code that isn't obvious to you, please add a comment telling what it does once you figured out.

## Features to come
It would be nice to be able to use the STT and TTS APIs from Google, Amazon & Co. There are probably a number of users that don't really care about offline services and who would profit a lot from the increased quality. In the end, VoicePi should support pretty much all the engines Jasper already does.

We also need an STT module that uses a different STT module depending on the current state. That way, the activation command can still be done locally.

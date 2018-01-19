package de.piegames.picontrol.tts;

import java.io.IOException;
import javax.sound.sampled.AudioInputStream;

/** TODO */
public class CommandSpeechEngine extends SpeechEngine {

	public CommandSpeechEngine() {
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		try {
			Runtime.getRuntime().exec(new String[] { "espeak", "\"" + text + "\"" });
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}

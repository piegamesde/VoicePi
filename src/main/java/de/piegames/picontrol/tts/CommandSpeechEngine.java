package de.piegames.picontrol.tts;

import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import de.piegames.picontrol.PiControl;

/** TODO */
public class CommandSpeechEngine extends SpeechEngine {

	public CommandSpeechEngine(PiControl control) {
		super(control);
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		try {
			return AudioSystem.getAudioInputStream(Runtime.getRuntime().exec(new String[] { "espeak", "--stdout", "\"" + text + "\"" }).getInputStream());
		} catch (IOException | UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
		return null;
	}
}

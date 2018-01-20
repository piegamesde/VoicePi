package de.piegames.picontrol.tts;

import javax.sound.sampled.AudioInputStream;
import de.piegames.picontrol.PiControl;

public class MutedSpeechEngine extends SpeechEngine {

	public MutedSpeechEngine(PiControl control) {
		super(control);
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		return null;
	}
}
package de.piegames.picontrol.tts;

import javax.sound.sampled.AudioInputStream;

public class MutedSpeechEngine extends SpeechEngine {

	public MutedSpeechEngine() {
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		return null;
	}
}
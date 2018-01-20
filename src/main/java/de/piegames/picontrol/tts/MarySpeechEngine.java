package de.piegames.picontrol.tts;

import javax.sound.sampled.AudioInputStream;
import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;

public class MarySpeechEngine extends SpeechEngine {

	protected LocalMaryInterface mary;

	public MarySpeechEngine() throws MaryConfigurationException {
		mary = new LocalMaryInterface();
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		try {
			return mary.generateAudio(text);
		} catch (SynthesisException e) {
			log.info("Could not generate audio for text '" + text + "'", e);
			return null;
		}
	}
}

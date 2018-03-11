package de.piegames.voicepi.tts;

import javax.sound.sampled.AudioInputStream;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;

public class MutedSpeechEngine extends SpeechEngine {

	public MutedSpeechEngine(VoicePi control, JsonObject config) {
		super(control, config);
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		return null;
	}
}
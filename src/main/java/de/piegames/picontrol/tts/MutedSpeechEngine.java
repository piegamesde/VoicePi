package de.piegames.picontrol.tts;

import javax.sound.sampled.AudioInputStream;
import com.google.gson.JsonObject;
import de.piegames.picontrol.PiControl;

public class MutedSpeechEngine extends SpeechEngine {

	public MutedSpeechEngine(PiControl control, JsonObject config) {
		super(control, config);
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		return null;
	}
}
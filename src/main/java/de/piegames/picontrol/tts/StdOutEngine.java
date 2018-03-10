package de.piegames.picontrol.tts;

import javax.sound.sampled.AudioInputStream;
import com.google.gson.JsonObject;
import de.piegames.picontrol.PiControl;

public class StdOutEngine extends SpeechEngine {

	public StdOutEngine(PiControl control, JsonObject config) {
		super(control, config);
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		System.out.println(text);
		return null;
	}
}

package de.piegames.picontrol.tts;

import javax.sound.sampled.AudioInputStream;
import de.piegames.picontrol.PiControl;


public class StdOutEngine extends SpeechEngine {

	public StdOutEngine(PiControl control) {
		super(control);
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		System.out.println(text);
		return null;
	}

}

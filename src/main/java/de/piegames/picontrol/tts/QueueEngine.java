package de.piegames.picontrol.tts;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sound.sampled.AudioInputStream;
import de.piegames.picontrol.PiControl;

public class QueueEngine extends SpeechEngine {

	public final BlockingQueue<String> spoken = new LinkedBlockingQueue<>();

	public QueueEngine(PiControl control) {
		super(control);
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		spoken.add(text);
		return null;
	}
}
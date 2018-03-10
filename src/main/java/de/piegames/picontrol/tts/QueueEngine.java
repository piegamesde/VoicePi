package de.piegames.picontrol.tts;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sound.sampled.AudioInputStream;
import com.google.gson.JsonObject;
import de.piegames.picontrol.PiControl;

public class QueueEngine extends SpeechEngine {

	public final BlockingQueue<String> spoken = new LinkedBlockingQueue<>();

	public QueueEngine(PiControl control, JsonObject config) {
		super(control, config);
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		spoken.add(text);
		return null;
	}
}
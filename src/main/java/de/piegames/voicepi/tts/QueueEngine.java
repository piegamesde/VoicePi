package de.piegames.voicepi.tts;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sound.sampled.AudioInputStream;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;

public class QueueEngine extends SpeechEngine {

	public final BlockingQueue<String> spoken = new LinkedBlockingQueue<>();

	public QueueEngine(VoicePi control, JsonObject config) {
		super(control, config);
	}

	public QueueEngine(VoicePi control) {
		this(control, null);
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		spoken.add(text);
		return null;
	}
}
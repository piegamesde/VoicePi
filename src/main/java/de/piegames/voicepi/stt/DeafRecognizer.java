package de.piegames.voicepi.stt;

import java.util.Collections;
import java.util.List;
import com.google.gson.JsonObject;

public class DeafRecognizer extends SpeechRecognizer {

	public DeafRecognizer() {
		this(null);
	}

	public DeafRecognizer(JsonObject config) {
		super(config);
	}

	@Override
	public void run() {
		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<String> transcribe() {
		return Collections.emptyList();
	}

	@Override
	public boolean transcriptionSupported() {
		return true;
	}
}

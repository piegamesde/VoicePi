package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.Set;
import com.google.gson.JsonObject;

public class DeafRecognizer extends SpeechRecognizer {

	public DeafRecognizer() {
		super(null);
	}

	public DeafRecognizer(JsonObject config) {
		super(config);
	}

	@Override
	public void load(Set<String> commands) throws IOException {
	}

	@Override
	public void run() {
		try {
			new Object().wait();
		} catch (InterruptedException e) {
			return;
		}
	}
}

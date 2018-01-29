package de.piegames.picontrol.stt;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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
	public Collection<String> nextCommand() throws Exception {
		Thread.sleep(100);
		return Collections.emptySet();
	}
}

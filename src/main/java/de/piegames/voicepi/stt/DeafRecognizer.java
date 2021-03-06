package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;

public class DeafRecognizer extends SpeechRecognizer {

	public DeafRecognizer(VoicePi control) {
		this(control, null);
	}

	public DeafRecognizer(VoicePi control, JsonObject config) {
		super(control, config);
	}

	@Override
	public void load(BlockingQueue<Collection<String>> commandsSpoken, Set<String> commands) throws IOException {
		this.commandsSpoken = commandsSpoken;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// try {
		// throw new InternalError("FIXME: This has to be implemented");
		// This does not work and is buggy
		// new Object().wait();
		// } catch (InterruptedException e) {
		// return;
		// }
	}
}

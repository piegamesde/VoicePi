package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.google.gson.JsonObject;

public class QueueRecognizer extends SpeechRecognizer {

	public final BlockingQueue<Collection<String>> spoken = new LinkedBlockingQueue<>();

	public QueueRecognizer(JsonObject config) {
		super(config);
	}

	public QueueRecognizer() {
		this(null);
	}

	public void commandSpoken(String command) {
		spoken.add(Arrays.asList(command));
	}

	public void commandSpoken(Collection<String> command) {
		spoken.add(command);
	}

	@Override
	public void load(Set<String> commands) throws IOException {
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted())
			try {
				commandsSpoken.add(spoken.take());
			} catch (InterruptedException e) {
				break;
			}
	}
}

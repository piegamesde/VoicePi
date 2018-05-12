package de.piegames.voicepi.stt;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.google.gson.JsonObject;

public class QueueRecognizer extends SpeechRecognizer {

	public final BlockingQueue<Collection<String>> spoken = new LinkedBlockingQueue<>();

	public QueueRecognizer(JsonObject config) {
		super(config);
	}

	public QueueRecognizer() {
		super(null);
	}

	@Override
	public void commandSpoken(String command) {
		spoken.add(Arrays.asList(command));
	}

	@Override
	public void commandSpoken(Collection<String> command) {
		spoken.add(command);
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted())
			try {
				super.commandSpoken(spoken.take());
			} catch (InterruptedException e) {
				break;
			}
	}

	@Override
	public void startRecognition() {
		spoken.clear();
		super.startRecognition();
	}

	@Override
	public boolean transcriptionSupported() {
		return false;
	}
}

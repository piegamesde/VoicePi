package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;

public class QueueRecognizer extends SpeechRecognizer {

	public final BlockingQueue<Collection<String>> spoken = new LinkedBlockingQueue<>();

	public QueueRecognizer(VoicePi control, JsonObject config) {
		super(control, config);
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
	public void load(BlockingQueue<Collection<String>> commandsSpoken, Set<String> commands) throws IOException {
		this.commandsSpoken = commandsSpoken;
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
	public boolean transcriptionSupported() {
		return false;
	}
}

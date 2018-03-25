package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;

public class StdInRecognizer extends SpeechRecognizer {

	protected Scanner scanner;

	public StdInRecognizer(VoicePi control, JsonObject config) {
		super(control, config);
	}

	@Override
	public void load(BlockingQueue<Collection<String>> commandsSpoken, Set<String> commands) throws IOException {
		this.commandsSpoken = commandsSpoken;
		scanner = new Scanner(System.in);
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted())
			commandSpoken(scanner.nextLine());
	}

	@Override
	public void stopRecognition() {
		thread.interrupt();
		Thread.yield();
		// It was either this, or active waiting in the run() method. I opted for the shorter solution
		thread.stop();
	}
}

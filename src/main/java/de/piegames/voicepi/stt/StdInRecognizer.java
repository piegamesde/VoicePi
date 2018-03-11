package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import com.google.gson.JsonObject;

public class StdInRecognizer extends SpeechRecognizer {

	protected Scanner scanner;

	public StdInRecognizer(JsonObject config) {
		super(config);
	}

	@Override
	public void load(Set<String> commands) throws IOException {
		scanner = new Scanner(System.in);
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted())
			commandsSpoken.add(Arrays.asList(scanner.nextLine()));
	}

	@Override
	public void stopRecognition() {
		thread.interrupt();
		Thread.yield();
		// It was either this, or active waiting in the run() method. I opted for the shorter solution
		thread.stop();
	}
}

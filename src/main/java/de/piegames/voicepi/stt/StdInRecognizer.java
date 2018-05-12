package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.io.input.CloseShieldInputStream;
import com.google.gson.JsonObject;
import de.piegames.voicepi.Settings;
import de.piegames.voicepi.audio.Audio;
import de.piegames.voicepi.state.VoiceState;

public class StdInRecognizer extends SpeechRecognizer {

	protected volatile boolean			ignoreExceptions	= false;
	protected Scanner					scanner;
	protected InterruptibleInputStream	in;

	public StdInRecognizer(JsonObject config) {
		super(config);
	}

	public StdInRecognizer() {
		super(null);
	}

	@Override
	public void load(Audio audio, VoiceState stateMachine, Settings settings, BlockingQueue<Collection<String>> commandsSpoken, Set<String> commands) throws IOException {
		super.load(audio, stateMachine, settings, commandsSpoken, commands);
		scanner = new Scanner(in = new InterruptibleInputStream(new CloseShieldInputStream(System.in)));
	}

	@Override
	public void run() {
		try {
			while (!Thread.interrupted())
				commandSpoken(scanner.nextLine());
		} catch (RuntimeException e) {
			if (Thread.interrupted() || ignoreExceptions)
				return;// Ignore since it will throw an exception when interrupted
			log.warn("Exception while reading", e);
		}
	}

	@Override
	public void stopRecognition() {
		ignoreExceptions = true;
		thread.interrupt();
		scanner.close();
		super.stopRecognition();
	}

	@Override
	public boolean transcriptionSupported() {
		return false;
	}
}

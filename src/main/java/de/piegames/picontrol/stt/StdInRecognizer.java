package de.piegames.picontrol.stt;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
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
	public Collection<String> nextCommand() throws Exception {
		return Arrays.asList(scanner.nextLine());
	}
}

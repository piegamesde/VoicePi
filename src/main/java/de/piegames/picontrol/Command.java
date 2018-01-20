package de.piegames.picontrol;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.logging.LogFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.piegames.picontrol.tts.SpeechEngine;

/**
 * This class represents a shell command that can be executed including all options to start and handle the process. The data is parsed from a JsonElement that
 * holds all the settings as specified in TODO.
 */
public class Command {

	protected String[]		cmd;
	protected String[]		env;
	protected File			dir;
	protected boolean		waitFor;
	protected SpeechEngine	tts;
	protected String		sayBeforeExecuting;

	public Command(JsonElement element, SpeechEngine tts, File moduleHome) {
		if (element.isJsonObject()) {
			JsonObject elem = element.getAsJsonObject();
			if (elem.has("await-termination"))
				waitFor = elem.getAsJsonPrimitive("await-termination").getAsBoolean();
			if (elem.has("stdout-to-tts") && elem.getAsJsonPrimitive("stdout-to-tts").getAsBoolean())
				this.tts = Objects.requireNonNull(tts);
			processCMD(elem.get("command"));
			if (elem.has("environment"))
				processENV(elem.get("environment"));
			if (elem.has("launch-dir"))
				dir = new File(elem.getAsJsonPrimitive().getAsString());
			if (elem.has("say-before-executing")) {
				sayBeforeExecuting = elem.getAsJsonPrimitive("say-before-executing").getAsString();
				this.tts = Objects.requireNonNull(tts);
			}
		} else {
			processCMD(element);
		}
		if (dir == null)
			dir = moduleHome;
		else if (!dir.isAbsolute())
			dir = new File(moduleHome, dir.getPath());
	}

	protected void processCMD(JsonElement element) {
		if (element.isJsonPrimitive()) {
			// String command = element.getAsString().trim();
			// int index = command.indexOf(' ');
			// cmd = new String[] { command.substring(0, index), command.substring(index + 1, command.length()) };
			cmd = new String[] { element.getAsString() };
		} else if (element.isJsonArray()) {
			cmd = new String[element.getAsJsonArray().size()];
			for (int i = 0; i < cmd.length; i++)
				cmd[i] = element.getAsJsonArray().get(i).getAsString();
		}
	}

	protected void processENV(JsonElement element) {
		if (element.isJsonPrimitive())
			env = element.getAsString().split(";");
		else if (element.isJsonArray()) {
			env = new String[element.getAsJsonArray().size()];
			for (int i = 0; i < env.length; i++)
				env[i] = element.getAsJsonArray().get(i).getAsString();
		} else {
			env = element.getAsJsonObject().entrySet().stream()
					.map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList()).toArray(new String[0]);
		}
	}

	public void execute() throws IOException, InterruptedException {
		if (sayBeforeExecuting != null)
			tts.speakAndWait(sayBeforeExecuting);
		LogFactory.getLog(getClass()).debug("Executing " + Arrays.toString(cmd) + ", " + Arrays.toString(env) + " at " + dir);
		Process process = cmd.length > 1 ? Runtime.getRuntime().exec(cmd, env, dir) : Runtime.getRuntime().exec(cmd[0], env, dir);
		if (tts != null) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					tts.speakAndWait(line);
				}
			}
		}

		if (waitFor)
			process.waitFor();
	}
}
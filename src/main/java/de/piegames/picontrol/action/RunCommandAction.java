package de.piegames.picontrol.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import com.google.gson.JsonObject;
import de.piegames.picontrol.PiControl;

/**
 * This class represents a shell command that can be executed including all options to start and handle the process. The data is parsed from a JsonElement that
 * holds all the settings as specified in TODO.
 */
public class RunCommandAction extends Action {

	protected boolean		waitFor;
	protected boolean		tts;
	protected String		sayBeforeExecuting;
	protected RunCommand	command;

	public RunCommandAction(JsonObject element) {
		super(ActionType.RUN_COMMAND, element);
		// TODO Make config params constants
		JsonObject elem = element.getAsJsonObject();
		if (elem.has("await-termination"))
			waitFor = elem.getAsJsonPrimitive("await-termination").getAsBoolean();
		if (elem.has("stdout-to-tts") && elem.getAsJsonPrimitive("stdout-to-tts").getAsBoolean())
			tts = true;

		command = new RunCommand(elem.get("command"));

		if (elem.has("say-before-executing"))
			sayBeforeExecuting = elem.getAsJsonPrimitive("say-before-executing").getAsString();
	}

	@Override
	public void execute(PiControl control) throws IOException, InterruptedException {
		if (sayBeforeExecuting != null)
			control.getTTS().speakAndWait(sayBeforeExecuting);
		Process process = command.execute();
		if (tts || waitFor)
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (tts)
						control.getTTS().speakAndWait(line);
					else
						log.debug(">" + line);
				}
			}

		if (waitFor)
			process.waitFor();
	}
}
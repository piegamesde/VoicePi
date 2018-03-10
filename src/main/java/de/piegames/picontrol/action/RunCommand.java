package de.piegames.picontrol.action;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RunCommand {

	protected final Log	log	= LogFactory.getLog(getClass());

	protected String[]	cmd;
	protected String[]	env;
	protected File		dir;

	public RunCommand(JsonElement command) {
		if (command.isJsonObject()) {
			JsonObject elem = command.getAsJsonObject();
			if (elem.has("environment"))
				processENV(elem.get("environment"));
			if (elem.has("launch-dir"))
				dir = new File(elem.getAsJsonPrimitive().getAsString());
			processCMD(elem.get("run"));
		} else {
			processCMD(command);
		}
	}

	protected void processCMD(JsonElement element) {
		if (element.isJsonPrimitive()) {
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

	public Process execute() throws IOException {
		log.debug("Executing " + Arrays.toString(cmd) + (env != null ? (", " + Arrays.toString(env)) : "") + (dir != null ? " at directory " + dir : ""));
		Process process = cmd.length > 1 ? Runtime.getRuntime().exec(cmd, env, dir) : Runtime.getRuntime().exec(cmd[0], env, dir);
		return process;
	}
}
package de.piegames.picontrol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ShellModule extends Module {

	protected Map<String, String> commands;

	public ShellModule() throws RuntimeException, IOException {
		JsonObject config = new JsonParser().parse(Files.newBufferedReader(Paths.get("module-config.json"))).getAsJsonObject();
		config.getAsJsonArray("submodules").forEach(element -> loadSubmodule(element.getAsString()));
	}

	protected void loadSubmodule(String path) {
		Properties commands = new Properties();
		try {
			commands.load(Files.newBufferedReader(Paths.get(path + ".properties")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		commands.forEach((key, value) -> commands.put(String.valueOf(key), String.valueOf(value)));
	}

	@Override
	public Set<String> listCommands() {
		return commands.keySet();
	}

	@Override
	public void commandSpoken(String command) {
		try {
			Runtime.getRuntime().exec(commands.get(command));
		} catch (IOException e) {
			LOG.warn("Could not execute command '" + command + "'", e);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
}
package de.piegames.voicepi.module;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import de.piegames.voicepi.state.ContextState;

public class ApplicationModule extends SimpleModule {

	private Set<String> exit = new HashSet<>(), reload = new HashSet<>();
	// Other possible commands: pause/resume, mute/unmute, ...

	public ApplicationModule(VoicePi control, String name, JsonObject config) throws RuntimeException {
		super(control, name, config);
		config.getAsJsonArray("exit-commands").forEach(element -> exit.add(element.getAsString()));
		config.getAsJsonArray("reload-commands").forEach(element -> reload.add(element.getAsString()));

		log.debug("Commands to exit the application: " + Arrays.toString(exit.toArray()));
		log.debug("Commands to reload the modules of the application: " + Arrays.toString(reload.toArray()));
		int commandCount = reload.size();
		reload.removeAll(exit);
		if (reload.size() < commandCount)
			log.warn((commandCount - reload.size()) + " reload commands are also registered als exit commands, removing them.");
	}

	@Override
	public Set<String> listCommands() {
		Set<String> commands = new HashSet<>();
		commands.addAll(exit);
		commands.addAll(reload);
		return commands;
	}

	@Override
	public void onCommandSpoken(ContextState currentState, String command) {
		if (exit.contains(command))
			control.exitApplication();
		else if (reload.contains(command))
			control.reload();
		else
			log.warn("Command " + command + " is not registered");
	}
}
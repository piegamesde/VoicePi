package de.piegames.picontrol.module;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.gson.JsonObject;
import de.piegames.picontrol.PiControl;
import de.piegames.picontrol.state.ContextState;

public class ApplicationModule extends Module {

	private Set<String> exit = new HashSet<>(), reload = new HashSet<>();
	// Other possible commands: pause/resume, mute/unmute, ...

	public ApplicationModule(PiControl control, String name, JsonObject config) throws RuntimeException {
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
	public MutableValueGraph<ContextState<Module>, Set<String>> listCommands(ContextState<Module> root) {
		MutableValueGraph<ContextState<Module>, Set<String>> ret = ValueGraphBuilder.directed().build();
		Set<String> commands = new HashSet<>();
		commands.addAll(exit);
		commands.addAll(reload);
		ContextState<Module> node = new ContextState<>(this, "end");
		ret.putEdgeValue(root, node, commands);
		return ret;
	}

	@Override
	public void onCommandSpoken(ContextState<Module> currentState, String command) {
		if (exit.contains(command))
			control.exitApplication();
		else if (reload.contains(command))
			control.reload();
		else
			log.warn("Command " + command + " is not registered");
	}
}
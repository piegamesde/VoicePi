package de.piegames.picontrol.module;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import de.piegames.picontrol.PiControl;
import de.piegames.picontrol.state.ContextState;

public class ShellModule extends Module {

	protected Map<String, String> commands = new HashMap<>();

	public ShellModule(PiControl control, String name, Path base) throws RuntimeException {
		super(control, name, base);
		config.getAsJsonObject("commands").entrySet().stream()
				.forEach(e -> commands.put(e.getKey(), e.getValue().getAsString()));
	}

	@Override
	public MutableValueGraph<ContextState<Module>, Set<String>> listCommands(ContextState<Module> root) {
		MutableValueGraph<ContextState<Module>, Set<String>> ret = ValueGraphBuilder.directed().build();
		ContextState<Module> node = new ContextState<>(this, "end");
		ret.putEdgeValue(root, node, Collections.unmodifiableSet(commands.keySet()));
		return ret;
	}

	@Override
	public void commandSpoken(ContextState<Module> currentState, String command) {
		try {
			Runtime.getRuntime().exec(commands.get(command));
		} catch (IOException e) {
			log.warn("Could not execute command '" + command + "'", e);
		} catch (NullPointerException e) {
			log.warn("Command " + command + " is not registered");
		}
	}
}
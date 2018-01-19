package de.piegames.picontrol.module;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import de.piegames.picontrol.PiControl;
import de.piegames.picontrol.state.ContextState;

public class ShellModule extends Module {

	protected Map<String, String> commands;

	public ShellModule(PiControl control, String name, Path base) throws RuntimeException {
		super(control, name, base);
		config.getAsJsonObject("commands").entrySet().stream().forEach(e -> commands.put(e.getKey(), e.getValue().getAsString()));
	}

	@Override
	public MutableValueGraph<ContextState, Set<String>> listCommands(ContextState root) {
		MutableValueGraph<ContextState, Set<String>> ret = ValueGraphBuilder.directed().build();
		ContextState node = new ContextState(this, "end");
		ret.addNode(node);
		ret.putEdgeValue(root, node, Collections.unmodifiableSet(commands.keySet()));
		return ret;
	}

	@Override
	public void commandSpoken(ContextState currentState, String command) {
		try {
			Runtime.getRuntime().exec(commands.get(command));
		} catch (IOException e) {
			log.warn("Could not execute command '" + command + "'", e);
		} catch (NullPointerException e) {
			log.warn("Command " + command + " is not registered");
		}
	}
}
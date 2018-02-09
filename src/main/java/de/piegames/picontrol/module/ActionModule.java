package de.piegames.picontrol.module;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.gson.JsonObject;
import de.piegames.picontrol.PiControl;
import de.piegames.picontrol.action.Action;
import de.piegames.picontrol.state.ContextState;

public class ActionModule extends Module {

	protected Map<String, Action> commands = new HashMap<>();

	public ActionModule(PiControl control, String name, JsonObject config) throws RuntimeException {
		super(control, name, config);
		config.getAsJsonObject("commands").entrySet().stream()
				.forEach(e -> commands.put(e.getKey(), Action.fromJson(e.getValue().getAsJsonObject(), control)));
	}

	@Override
	public MutableValueGraph<ContextState<Module>, Set<String>> listCommands(ContextState<Module> root) {
		MutableValueGraph<ContextState<Module>, Set<String>> ret = ValueGraphBuilder.directed().build();
		ContextState<Module> node = new ContextState<>(this, "end");
		ret.putEdgeValue(root, node, Collections.unmodifiableSet(commands.keySet()));
		return ret;
	}

	@Override
	public void onCommandSpoken(ContextState<Module> currentState, String command) {
		try {
			commands.get(command).execute();
		} catch (IOException e) {
			log.warn("Could not execute command '" + command + "'", e);
		} catch (NullPointerException e) {
			log.warn("Command " + command + " is not registered");
		} catch (InterruptedException e) {
			log.warn("Could not wait for process to finish, it will continue running in background");
		}
	}
}
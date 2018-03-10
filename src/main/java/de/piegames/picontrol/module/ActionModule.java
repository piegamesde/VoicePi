package de.piegames.picontrol.module;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.base.Optional;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.gson.JsonObject;
import de.piegames.picontrol.PiControl;
import de.piegames.picontrol.action.Action;
import de.piegames.picontrol.state.ContextState;

public class ActionModule extends Module {

	// protected Map<Edge<String, String>, Map<String, Action>> commands = new HashMap<>();
	protected MutableValueGraph<String, Map<String, Action>> commands = ValueGraphBuilder.directed().build();

	@SuppressWarnings("deprecation")
	public ActionModule(PiControl control, String name, JsonObject config) throws RuntimeException {
		super(control, name, config);
		config.getAsJsonObject("commands").entrySet()
				.stream()
				.forEach(e -> {
					JsonObject value = e.getValue().getAsJsonObject();
					String end = "end";
					if (value.has("next-state"))
						end = value.getAsJsonPrimitive("next-state").getAsString();
					putCommand("root", end, e.getKey(), Action.fromJson(value));
				});
		config.entrySet()
				.stream()
				.filter(e -> e.getKey().startsWith("commands-"))
				.forEach(e -> {
					JsonObject value = e.getValue().getAsJsonObject();
					String end = "end";
					if (value.has("next-state"))
						end = value.getAsJsonPrimitive("next-state").getAsString();
					putCommand(e.getKey().substring("commands-".length()), end, e.getKey(), Action.fromJson(value));
				});
	}

	protected void putCommand(String start, String end, String command, Action value) {
		if (!commands.hasEdgeConnecting("root", end))
			commands.edgeValue("root", end).orElseGet(() -> {
				HashMap<String, Action> ret = new HashMap<>();
				commands.putEdgeValue(start, end, ret);
				return ret;
			}).put(command, value);
	}

	@Override
	public MutableValueGraph<ContextState<Module>, Set<String>> listCommands(ContextState<Module> root) {
		Map<String, ContextState<Module>> states = commands
				.nodes()
				.stream()
				.filter(s -> !s.equals("root"))
				.collect(Collectors.toMap(s -> s, s -> new ContextState<>(this, s)));
		states.put("root", root);

		MutableValueGraph<ContextState<Module>, Set<String>> ret = ValueGraphBuilder.directed().build();
		// ret.putEdgeValue(root, node, Collections.unmodifiableSet(startCommands.keySet()));
		for (EndpointPair<String> edge : commands.edges()) {
			ret.putEdgeValue(states.get(edge.source()), states.get(edge.target()), commands.edgeValue(edge.source(), edge.target()).get().keySet());
		}
		return ret;
	}

	@Override
	public void onCommandSpoken(ContextState<Module> currentState, String command) {
		try {
			for (String nextState : commands.successors(currentState.name)) {
				Optional<Action> action = Optional.fromNullable(commands.edgeValue(currentState.name, nextState).get().get(command));
				if (action.isPresent()) {
					action.get().execute(control);
					return;
				}
			}
			log.warn("Command " + command + " is not registered for state " + currentState.name);
		} catch (IOException e) {
			log.warn("Could not execute command '" + command + "'", e);
		} catch (InterruptedException e) {
			log.warn("Could not wait for process to finish, it will continue running in background");
		}
	}
}
package de.piegames.voicepi.module;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.google.common.base.Optional;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import de.piegames.voicepi.action.Action;
import de.piegames.voicepi.state.CommandSet;
import de.piegames.voicepi.state.ContextState;

public class ActionModule extends Module {

	// protected Map<Edge<String, String>, Map<String, Action>> commands = new HashMap<>();
	protected MutableValueGraph<String, Map<String, Action>> commands = ValueGraphBuilder.directed().build();

	public ActionModule(VoicePi control, String name, JsonObject config) throws RuntimeException {
		super(control, name, config);
		putCommands(config.getAsJsonObject("commands"), "root");
		config.entrySet()
				.stream()
				.filter(e -> e.getKey().startsWith("commands-"))
				.forEach(e -> putCommands(e.getValue().getAsJsonObject(), e.getKey().substring("commands-".length())));
	}

	protected void putCommands(JsonObject state, String stateName) {
		state.entrySet()
				.stream()
				.forEach(e -> {
					JsonObject value = e.getValue().getAsJsonObject();
					String end = "end";
					if (value.has("next-state"))
						end = value.getAsJsonPrimitive("next-state").getAsString();
					putCommand(stateName, end, e.getKey(), value);
				});
	}

	protected void putCommand(String start, String end, String command, JsonObject value) {
		// Action action = VoicePi.GSON.fromJson(value, Action.class);
		Action action = null;
		try {
			action = VoicePi.GSON.fromJson(value, Action.class);
		} catch (RuntimeException e) {
			log.warn("Cannot create action for command '" + command + "', ignoring", e);
			return;
		}
		commands.edgeValue(start, end).orElseGet(() -> {
			HashMap<String, Action> ret = new HashMap<>();
			commands.putEdgeValue(start, end, ret);
			return ret;
		}).put(command, action);
	}

	@Override
	public MutableValueGraph<ContextState, CommandSet> listCommands(ContextState root) {
		Map<String, ContextState> states = commands
				.nodes()
				.stream()
				.filter(s -> !s.equals("root"))
				.collect(Collectors.toMap(s -> s, s -> new ContextState(name, s)));
		states.put("root", root);

		MutableValueGraph<ContextState, CommandSet> ret = ValueGraphBuilder.directed().build();
		// ret.putEdgeValue(root, node, Collections.unmodifiableSet(startCommands.keySet()));
		for (EndpointPair<String> edge : commands.edges()) {
			ret.putEdgeValue(
					states.get(edge.source()),
					states.get(edge.target()),
					new CommandSet(this, commands.edgeValue(edge.source(), edge.target()).get().keySet()));
		}
		return ret;
	}

	@Override
	public void onCommandSpoken(ContextState currentState, String command) {
		try {
			for (String nextState : commands.successors(currentState.state)) {
				Optional<Action> action = Optional.fromNullable(commands.edgeValue(currentState.state, nextState).get().get(command));
				if (action.isPresent()) {
					action.get().execute(control);
					return;
				}
			}
			log.warn("Command " + command + " is not registered for state " + currentState.state);
		} catch (IOException e) {
			log.warn("Could not execute command '" + command + "'", e);
		} catch (InterruptedException e) {
			log.warn("Could not wait for process to finish, it will continue running in background");
		}
	}
}
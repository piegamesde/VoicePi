package de.piegames.picontrol.module;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.piegames.picontrol.state.ContextState;

public class ApplicationModule extends Module {

	private Set<String> exit = new HashSet<>(), reload = new HashSet<>();
	// Other possible commands: pause/resume, mute/unmute, ...

	public ApplicationModule() throws RuntimeException, IOException {
		JsonObject config = new JsonParser().parse(Files.newBufferedReader(Paths.get("modules", "application-control", "module-config.json"))).getAsJsonObject();
		config.getAsJsonArray("exit-commands").forEach(element -> exit.add(element.getAsString()));
		config.getAsJsonArray("reload-commands").forEach(element -> reload.add(element.getAsString()));

		LOG.debug("Commands to exit the application: " + Arrays.toString(exit.toArray()));
		LOG.debug("Commands to reload the modules of the application: " + Arrays.toString(reload.toArray()));
		int commandCount = reload.size();
		reload.removeAll(exit);
		if (reload.size() < commandCount)
			LOG.warn((commandCount - reload.size()) + " reload commands are also registered als exit commands, removing them.");
	}

	@Override
	public MutableValueGraph<ContextState, Set<String>> listCommands(ContextState root) {
		MutableValueGraph<ContextState, Set<String>> ret = ValueGraphBuilder.directed().build();
		Set<String> commands = new HashSet<>();
		commands.addAll(exit);
		commands.addAll(reload);
		ContextState node = new ContextState(this, "end");
		ret.addNode(node);
		ret.putEdgeValue(root, node, commands);
		return ret;
	}

	@Override
	public void commandSpoken(ContextState currentState, String command) {
		if (exit.contains(command))
			System.exit(0);// TODO quit normally
		else
			; // TODO warning
	}
}
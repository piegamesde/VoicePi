package de.piegames.voicepi.module;

import java.util.Set;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import de.piegames.voicepi.state.CommandSet;
import de.piegames.voicepi.state.ContextState;

public abstract class SimpleModule extends Module {

	protected Set<String> commands;

	public SimpleModule(VoicePi control, String name, JsonObject config) throws RuntimeException {
		super(control, name, config);
	}

	@Override
	public MutableValueGraph<ContextState, CommandSet> listCommands(ContextState root) {
		commands = listCommands();

		MutableValueGraph<ContextState, CommandSet> ret = ValueGraphBuilder.directed().build();
		ContextState node = new ContextState(name, "end");
		ret.putEdgeValue(root, node, new CommandSet(this, commands));
		return ret;
	}

	public abstract Set<String> listCommands();
}

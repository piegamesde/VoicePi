package de.piegames.voicepi.module;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import de.piegames.voicepi.state.CommandSet;
import de.piegames.voicepi.state.ContextState;


public class DummyModule extends Module {

	public DummyModule(String name) throws RuntimeException {
		super(null, name, null);
	}

	public DummyModule(VoicePi control, String name, JsonObject config) throws RuntimeException {
		super(control, name, config);
	}

	@Override
	public MutableValueGraph<ContextState, CommandSet> listCommands(ContextState root) {
		return ValueGraphBuilder.directed().build();
	}

	@Override
	public void onCommandSpoken(ContextState currentState, String command) {
	}
}

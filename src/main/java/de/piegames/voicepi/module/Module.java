package de.piegames.voicepi.module;

import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.common.graph.MutableValueGraph;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import de.piegames.voicepi.state.CommandSet;
import de.piegames.voicepi.state.ContextState;

public abstract class Module {

	protected final Log		log	= LogFactory.getLog(getClass());
	protected final VoicePi	control;
	protected final String	name;
	protected JsonObject	config;

	public Module(VoicePi control, String name, JsonObject config) throws RuntimeException {
		this.control = control;
		this.name = Objects.requireNonNull(name);
		this.config = config;
		if (name.equals("*"))
			throw new IllegalArgumentException("Wildcard name '*' is not allowed as a module name");
	}

	public abstract MutableValueGraph<ContextState, CommandSet> listCommands(ContextState root);

	public abstract void onCommandSpoken(ContextState currentState, String command);

	public void close() {
	}
}
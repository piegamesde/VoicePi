package de.piegames.picontrol.module;

import java.util.Objects;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.common.graph.MutableValueGraph;
import com.google.gson.JsonObject;
import de.piegames.picontrol.PiControl;
import de.piegames.picontrol.state.ContextState;

public abstract class Module {

	protected final Log			log		= LogFactory.getLog(getClass());
	protected final PiControl	control;
	protected final String		name;
	protected JsonObject		config;

	public Module(PiControl control, String name, JsonObject config) throws RuntimeException {
		this.control = Objects.requireNonNull(control);
		this.name = Objects.requireNonNull(name);
		this.config = config;
	}

	public abstract MutableValueGraph<ContextState<Module>, Set<String>> listCommands(ContextState<Module> root);

	public abstract void onCommandSpoken(ContextState<Module> currentState, String command);

	public void close() {
	}
}
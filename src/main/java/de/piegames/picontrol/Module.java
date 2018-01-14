package de.piegames.picontrol;

import java.io.IOException;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.common.graph.MutableValueGraph;
import de.piegames.picontrol.state.ContextState;

public abstract class Module {

	protected final Log LOG = LogFactory.getLog(getClass());

	public Module() throws RuntimeException, IOException {
	}

	public abstract MutableValueGraph<ContextState, Set<String>> listCommands(ContextState root, ContextState end);
	
	public abstract void commandSpoken(ContextState currentState, String command);

	public void close() {
	}
}
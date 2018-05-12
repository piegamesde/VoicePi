package de.piegames.voicepi.state;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.common.base.Joiner;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class VoiceState {

	protected final Log										log		= LogFactory.getLog(getClass());
	protected final ContextState							root	= new ContextState("voicepi", "root");
	protected ContextState									start	= root;
	public final ObjectProperty<ContextState>				current	= new SimpleObjectProperty<ContextState>(root);
	protected MutableValueGraph<ContextState, CommandSet>	states	= ValueGraphBuilder.directed().build();

	public VoiceState() {
	}

	public ContextState getRoot() {
		return root;
	}

	public ContextState getStart() {
		return start;
	}

	public void setActivationCommands(Set<String> commands) {
		if (start != root)
			states.removeNode(start);
		if (commands == null || commands.isEmpty()) {
			log.debug("Disabling activation commands");
			if (current.getValue() == start)
				setState(root);
			start = root;
			return;
		}
		log.debug("Setting activation commands to " + Arrays.toString(commands.toArray()));
		start = new ContextState("voicepi", "listening");
		states.addNode(start);
		states.putEdgeValue(start, root, new CommandSet(null, commands));
		if (current.getValue() == root)
			resetState();
	}

	public void addModuleGraph(MutableValueGraph<ContextState, CommandSet> graph) {
		Set<ContextState> nodes = new HashSet<>(graph.nodes());
		if (!nodes.contains(root)) {
			log.warn("The registered commands graph contains no root. No command will ever reach it");
			log.debug("(Unless you hack the graph or inter-module communication is a feature somewhere in the far future)");
		} else {
			Set<String> rootEdges = graph.successors(root).stream().flatMap(node -> graph.edgeValue(root, node).get().commands.stream()).collect(Collectors.toSet());
			if (rootEdges.isEmpty()) {
				log.warn("The registered commands graph has no edges outgoing from the root state. No command will ever reach it");
				log.debug("(Unless you hack the graph or inter-module communication is a feature somewhere in the far future)");
			}
		}
		nodes.retainAll(states.nodes());
		nodes.remove(root);
		if (!nodes.isEmpty()) {
			log.warn("The nodes of the graph intersect with already existing nodes");
			log.debug("The following states are duplicate: " + Joiner.on(", ").join(nodes));
		}

		// TODO test for multiple commands from the same node
		graph.edges().forEach(pair -> states.putEdgeValue(
				pair.source(),
				pair.target(),
				graph.edgeValue(pair.source(), pair.target()).get()));
	}

	/**
	 * To be called after a command was spoken. It will check all registered commands for the current state against the spoken text. If a registered command is
	 * found, the machine will advance to next state where the command points to and return that state.
	 *
	 * If the new state has no outgoing edges it will set the new current state to be root. If no registered command is found, the internal state does not
	 * change and it returns null.
	 *
	 * @return the {@code CommandSet} owning the command that was spoken or {@code null} if it didn't change
	 */
	public CommandSet commandSpoken(String command) {
		for (ContextState node : states.successors(current.getValue())) {
			CommandSet edge = states.edgeValue(current.getValue(), node).get();
			if (edge.commands.contains(command)) {
				setState(node);
				if (states.outDegree(current.getValue()) == 0)
					resetState();
				return edge;
			}
		}
		return null;
	}

	public boolean isActivationNeeded() {
		return start != root && start.matches("voicepi:listening");
	}

	public boolean isWaitingForActivation() {
		return current.getValue() == start && isActivationNeeded();
	}

	public ContextState getCurrentState() {
		return current.getValue();
	}

	/** Get all commands that could be spoken in the current state */
	public Set<String> getAvailableCommands() {
		return getAvailableCommands(current.getValue());
	}

	/** Get all commands that could be spoken when in a given state */
	public Set<String> getAvailableCommands(ContextState state) {
		if (!states.nodes().contains(state))
			return Collections.emptySet();
		return states.successors(state).stream().flatMap(node -> states.edgeValue(state, node).get().commands.stream()).collect(Collectors.toSet());
	}

	/** Get all commands from all edges of the graph */
	public Set<String> getAllCommands() {
		return states.edges().stream().flatMap(edge -> states.edgeValue(edge.source(), edge.target()).get().commands.stream()).collect(Collectors.toSet());
	}

	public void resetState() {
		setState(start);
	}

	public void setState(ContextState newState) {
		current.set(newState);
	}

	public boolean isIdle() {
		return isActivationNeeded() ? current.get() == start : current.get() == root;
	}
}
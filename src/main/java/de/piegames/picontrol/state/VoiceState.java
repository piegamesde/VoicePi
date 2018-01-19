package de.piegames.picontrol.state;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import de.piegames.picontrol.module.Module;

public class VoiceState {

	protected final ContextState							root		= new ContextState("root");
	protected ContextState									current		= root;
	protected MutableValueGraph<ContextState, Set<String>>	states		= ValueGraphBuilder.directed().build();

	public VoiceState() {
	}

	public ContextState getRoot() {
		return root;
	}

	public void addModuleGraph(MutableValueGraph<ContextState, Set<String>> graph) {
		Set<ContextState> nodes = new HashSet<>(graph.nodes());
		if (!nodes.contains(root))
			;// TODO warn
		Set<String> rootEdges = graph.successors(root).stream().flatMap(node -> graph.edgeValue(root, node).get().stream()).collect(Collectors.toSet());
		if (rootEdges.isEmpty())
			;// TODO warn
		nodes.retainAll(states.nodes());
		nodes.remove(root);
		if (!nodes.isEmpty())
			;// TODO error

		// TODO test for multiple commands from the same node
		graph.edges().forEach(pair -> states.putEdgeValue(pair.source(), pair.target(), graph.edgeValue(pair.source(), pair.target()).get()));
	}

	public Module commandSpoken(String command) {
		for (ContextState node : states.successors(current))
			if (states.edgeValue(current, node).get().contains(command)) {
				current = node;
				if (states.outDegree(current) == 0)
					current = root;
				return node.owner;
			}
		return null;// TODO throw exception?
	}

	public ContextState getCurrentState() {
		return current;
	}

	public Set<String> availableCommands() {
		return availableCommands(current);
	}

	public Set<String> availableCommands(ContextState state) {
		return states.successors(state).stream().flatMap(node -> states.edgeValue(state, node).get().stream()).collect(Collectors.toSet());
	}

	public Set<String> getAllCommands() {
		return states.edges().stream().flatMap(edge -> states.edgeValue(edge.source(), edge.target()).get().stream()).collect(Collectors.toSet());
	}
}
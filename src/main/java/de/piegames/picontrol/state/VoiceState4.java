package de.piegames.picontrol.state;

import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import de.piegames.picontrol.Module;

public class VoiceState4 {

	protected final ContextState							root		= new ContextState("root"), end = new ContextState("end");
	protected ContextState									current		= root;
	protected MutableValueGraph<ContextState, Set<String>>	rootStates	= ValueGraphBuilder.directed().build();
	protected MutableValueGraph<ContextState, Set<String>>	states		= rootStates;

	public VoiceState4() {
	}

	public ContextState getRoot() {
		return root;
	}

	public ContextState getEnd() {
		return end;
	}

	public void addModuleGraph(MutableValueGraph<ContextState, Set<String>> graph) {
		Set<ContextState> nodes = graph.nodes();
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
}
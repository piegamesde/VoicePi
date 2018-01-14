package de.piegames.picontrol.state;

import java.util.Set;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;

public class VoiceState3 {

	protected final ContextState					root		= new ContextState("root"), end = new ContextState("end");
	protected ContextState							current		= root;
	protected MutableNetwork<ContextState, String>	rootStates	= NetworkBuilder.directed().allowsParallelEdges(true).build();
	protected MutableNetwork<ContextState, String>	states		= rootStates;

	public VoiceState3() {
	}

	public ContextState getRoot() {
		return root;
	}

	public ContextState getEnd() {
		return end;
	}

	public void addModuleGraph(Network<ContextState, String> graph) {
		Set<ContextState> nodes = graph.nodes();
		if (!nodes.contains(root))
			;// TODO warn
		Set<String> rootEdges = graph.outEdges(root);
		if (rootEdges.isEmpty())
			;// TODO warn
		Set<String> existingRootEdges = states.outEdges(root);

		// {// Check that the only common vertices are root and end
		// Set<ContextState> vertices = new HashSet<>(states.vertexSet());
		// vertices.retainAll(graph.vertexSet());
		// if (vertices.size() != 2 || !vertices.contains(root) || !vertices.contains(end)) {
		// // Warn, exception
		// }
		// }

		nodes.forEach(states::addNode);
		for (ContextState node : graph.nodes()) {
			for (ContextState node2 : graph.successors(node))
				graph.edgesConnecting(node, node2).forEach(edge -> states.addEdge(node, node2, edge));
		}
	}

	public void commandSpoken(String command) {
		// states.
	}

	public ContextState getCurrentState() {
		return current;
	}
}
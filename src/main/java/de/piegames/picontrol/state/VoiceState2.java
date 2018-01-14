package de.piegames.picontrol.state;

import java.util.HashSet;
import java.util.Set;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.graph.AsGraphUnion;
import org.jgrapht.graph.DirectedPseudograph;

public class VoiceState2 {

	protected final ContextState						root		= new ContextState("root"), end = new ContextState("end");
	protected ContextState								current		= root;
	protected DirectedPseudograph<ContextState, String>	rootStates	= new DirectedPseudograph<>(String.class);
	protected AbstractGraph<ContextState, String>		states		= rootStates;												// new AsGraphUnion<>(g1,
																																// g2);

	public VoiceState2() {
	}

	public ContextState getRoot() {
		return root;
	}

	public ContextState getEnd() {
		return end;
	}

	public void addModuleGraph(AbstractGraph<ContextState, String> graph) {
		{// Check that the only common vertices are root and end
			Set<ContextState> vertices = new HashSet<>(states.vertexSet());
			vertices.retainAll(graph.vertexSet());
			if (vertices.size() != 2 || !vertices.contains(root) || !vertices.contains(end)) {
				// Warn, exception
			}
		}
		states = new AsGraphUnion<>(states, graph);
	}

	public void commandSpoken(String command) {

	}

	public ContextState getCurrentState() {
		return current;
	}
}
package de.piegames.picontrol.state;

import org.apache.commons.graph.domain.basic.DirectedGraphImpl;
import org.apache.commons.graph.domain.statemachine.State;

public class VoiceState {

	// protected StateMachine state;
	protected DirectedGraphImpl	state;
	protected State				root, end;

	public VoiceState() {
		// state = new StateMachine("All states");
		state = new DirectedGraphImpl();
		// state.
		root = new State("root");
		end = new State("end");
		// state.addState(root);
		// state.setStartState(root);
	}

	public State getRootState() {
		return root;
	}

	public State getEndState() {
		return end;
	}
}
package de.piegames.picontrol.state;

import java.util.Objects;
import de.piegames.picontrol.module.Module;

public class ContextState {

	public final Module	owner;
	public final String	name;

	public ContextState(Module owner, String name) {
		this.owner = Objects.requireNonNull(owner);
		this.name = Objects.requireNonNull(name);
	}

	ContextState(String name) {
		owner = null;
		this.name = Objects.requireNonNull(name);
	}
}
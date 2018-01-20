package de.piegames.picontrol.state;

import java.util.Objects;

public class ContextState<T> {

	public final T	owner;
	public final String	name;

	public ContextState(T owner, String name) {
		this.owner = Objects.requireNonNull(owner);
		this.name = Objects.requireNonNull(name);
	}

	ContextState(String name) {
		owner = null;
		this.name = Objects.requireNonNull(name);
	}
}
package de.piegames.voicepi.state;

import java.util.Objects;

public class ContextState<T> {

	public final T		owner;
	public final String	name;

	public ContextState(T owner, String name) {
		this.owner = Objects.requireNonNull(owner);
		this.name = Objects.requireNonNull(name);
	}

	ContextState(String name) {
		owner = null;
		this.name = Objects.requireNonNull(name);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContextState<?> other = (ContextState<?>) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ContextState: " + owner + ":" + name;
	}
}
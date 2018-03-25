package de.piegames.voicepi.state;

import java.util.Objects;

public class ContextState {

	public final String	module;
	public final String	state;

	public ContextState(String name) {
		this(name.split(":")[0], name.split(":")[1]);
	}

	public ContextState(String module, String state) {
		this.module = Objects.requireNonNull(module);
		this.state = Objects.requireNonNull(state);
		if (module.contains(":") || module.contains("*"))
			throw new IllegalArgumentException("Module names cannot conatin * or :, '" + module + "' given");
		if (state.contains(":") || state.contains("*"))
			throw new IllegalArgumentException("State names cannot conatin * or :, '" + state + "' given from module " + module);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((module == null) ? 0 : module.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
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
		ContextState other = (ContextState) obj;
		if (module == null) {
			if (other.module != null)
				return false;
		} else if (!module.equals(other.module))
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return module + ":" + state;
	}

	public boolean matches(String wildcard) {
		return wildcard.replace("*:", module + ":").replace(":*", ":" + state).equals(toString());
	}
}
package de.piegames.voicepi;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import de.piegames.voicepi.action.Action;

/** This class stores all the settings relevant to the core of the application. */
public class Settings {

	protected Action		onStart				= Action.DO_NOTHING;
	protected Action		onExit				= Action.DO_NOTHING;
	protected Action		onActivation		= Action.DO_NOTHING;
	protected Action		onTimeout			= Action.DO_NOTHING;
	protected Action		onReload			= Action.DO_NOTHING;
	protected Action		onWrongCommand		= Action.DO_NOTHING;

	protected int			timeout				= 0;
	protected Set<String>	activationCommands	= new HashSet<>();

	public Settings() {
	}

	public Settings(Action onStart, Action onExit, Action onActivation, Action onTimeout, Action onReload, Action onWrongCommand, int timeout, Set<String> activationCommands) {
		setOnStart(onStart);
		setOnExit(onExit);
		setOnActivation(onActivation);
		setOnTimeout(onTimeout);
		setOnReload(onReload);
		setOnWrongCommand(onWrongCommand);
		setTimeout(timeout);
		setActivationCommands(activationCommands);
	}

	public Action getOnStart() {
		return onStart;
	}

	public void setOnStart(Action onStart) {
		this.onStart = Objects.requireNonNull(onStart, "Use Action.DO_NOTHING instead of null");
	}

	public Action getOnExit() {
		return onExit;
	}

	public void setOnExit(Action onExit) {
		this.onExit = Objects.requireNonNull(onExit, "Use Action.DO_NOTHING instead of null");
	}

	public Action getOnActivation() {
		return onActivation;
	}

	public void setOnActivation(Action onActivation) {
		this.onActivation = Objects.requireNonNull(onActivation, "Use Action.DO_NOTHING instead of null");
	}

	public Action getOnTimeout() {
		return onTimeout;
	}

	public void setOnTimeout(Action onTimeout) {
		this.onTimeout = Objects.requireNonNull(onTimeout, "Use Action.DO_NOTHING instead of null");
	}

	public Action getOnReload() {
		return onReload;
	}

	public void setOnReload(Action onReload) {
		this.onReload = Objects.requireNonNull(onReload, "Use Action.DO_NOTHING instead of null");
	}

	public Action getOnWrongCommand() {
		return onWrongCommand;
	}

	public void setOnWrongCommand(Action onWrongCommand) {
		this.onWrongCommand = Objects.requireNonNull(onWrongCommand, "Use Action.DO_NOTHING instead of null");
	}

	public int getTimeout() {
		if (timeout < 0)
			throw new IllegalArgumentException("Negative timeout values are not allowed. How do you imagine this to work?");
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public Set<String> getActivationCommands() {
		return activationCommands;
	}

	public void setActivationCommands(Set<String> activationCommands) {
		this.activationCommands = Objects.requireNonNull(activationCommands);
	}
}
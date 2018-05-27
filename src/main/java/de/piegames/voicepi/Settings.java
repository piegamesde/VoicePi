package de.piegames.voicepi;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import com.google.gson.annotations.SerializedName;
import de.piegames.voicepi.action.Action;

/** This class stores all the settings relevant to the core of the application. */
public class Settings {

	@SerializedName("on-start")
	protected Action		onStart				= Action.DO_NOTHING;
	@SerializedName("on-exit")
	protected Action		onExit				= Action.DO_NOTHING;
	@SerializedName("on-activation")
	protected Action		onActivation		= Action.DO_NOTHING;
	@SerializedName("on-timeout")
	protected Action		onTimeout			= Action.DO_NOTHING;
	@SerializedName("on-reload")
	protected Action		onReload			= Action.DO_NOTHING;
	@SerializedName("on-wrong-command")
	protected Action		onWrongCommand		= Action.DO_NOTHING;
	@SerializedName("on-command-spoken")
	protected Action		onCommandSpoken		= Action.DO_NOTHING;
	@SerializedName("language-code")
	protected String		langCode			= "en-US";

	protected int			timeout				= 0;
	@SerializedName("activation-commands")
	protected Set<String>	activationCommands	= new HashSet<>();

	public Settings() {
	}

	public Settings(Action onStart, Action onExit, Action onActivation, Action onTimeout, Action onReload, Action onWrongCommand, Action onCommandSpoken, int timeout, Set<String> activationCommands) {
		setOnStart(onStart);
		setOnExit(onExit);
		setOnActivation(onActivation);
		setOnTimeout(onTimeout);
		setOnReload(onReload);
		setOnWrongCommand(onWrongCommand);
		setTimeout(timeout);
		setActivationCommands(activationCommands);
		setOnCommandSpoken(onCommandSpoken);
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

	public Action getOnCommandSpoken() {
		return onCommandSpoken;
	}

	public void setOnCommandSpoken(Action onCommandSpoken) {
		this.onCommandSpoken = Objects.requireNonNull(onCommandSpoken, "Use Action.DO_NOTHING instead of null");
	}

	public void setLangCode(String langCode) {
		//Todo check if valid ?!
		this.langCode = langCode;
	}

	public String getLangCode() {
		return langCode;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		if (timeout < 0)
			throw new IllegalArgumentException("Negative timeout values are not allowed. How do you imagine this to work?");
		this.timeout = timeout;
	}

	public Set<String> getActivationCommands() {
		return activationCommands;
	}

	public void setActivationCommands(Set<String> activationCommands) {
		this.activationCommands = Objects.requireNonNull(activationCommands);
	}

	@Override
	public String toString() {
		return "Settings [onStart=" + onStart + ", onExit=" + onExit + ", onActivation=" + onActivation + ", onTimeout=" + onTimeout + ", onReload=" + onReload + ", onWrongCommand=" + onWrongCommand + ", timeout=" + timeout
				+ ", activationCommands=" + activationCommands + "]";
	}
}
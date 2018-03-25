package de.piegames.voicepi.state;

import java.util.Set;
import de.piegames.voicepi.module.Module;

public class CommandSet {

	public final Module	owner;
	public Set<String>	commands;

	public CommandSet(Module owner, Set<String> commands) {
		this.owner = owner;
		this.commands = commands;
	}

	public CommandSet(Module owner) {
		this(owner, null);
	}
}

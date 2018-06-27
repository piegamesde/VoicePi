package de.piegames.voicepi.state;

import java.util.Set;
import java.util.function.Predicate;

import de.piegames.voicepi.module.Module;

/**
 * Commands make up the edges of the finite state machine graph (represented by {@link VoiceState}) used to keep track of context. Most
 * commands have a module owning them which will be called if the command was spoken.
 */
public abstract class Command {

	/** The module owning this command */
	public final Module owner;

	public Command(Module owner) {
		this.owner = owner;
	}

	/**
	 * Tells if a given spoken text matches the command. If the return value is true, the command will be "executed" by advancing the state in
	 * the {@link VoiceState} and if {@link #owner} is not {@code null} {@link Module#onCommandSpoken(ContextState, String)} will be called.
	 */
	public abstract boolean matches(String spoken);

	/**
	 * A list of keywords or phrases to increase the STT's recognition rate. Each String in the returned set is a phrase of words separated by
	 * space. Note that some STT engines (namely Sphinx) require to know all words that may be spoken in advance. Some engines may be
	 * case-sensitive.
	 */
	public abstract Set<String> getKeywords();

	/**
	 * The commands are a set of commands the user could say. They are not used anywhere by the application except for communicating with the
	 * user. They are meant to be human-readable (unlike the keywords which are used by STT engines). Commands that are more open about which
	 * spoken phrases match should tell this in an intuitive fashion. It is recommended to use similar structure to conventional command line
	 * applications, like "{@code Play <SONG>}".
	 */
	public abstract Set<String> getCommands();

	/**
	 * A CommandSet is a {@code Command} implementation with a fixed set of allowed commands. A spoken phrase matches this command if it equals
	 * any of the allowed commands. The given commands are used as keywords too.
	 */
	public static class CommandSet extends Command {

		protected final Set<String> commands;

		public CommandSet(Module owner, Set<String> commands) {
			super(owner);
			this.commands = commands;
		}

		@Override
		public Set<String> getKeywords() {
			return commands;
		}

		@Override
		public Set<String> getCommands() {
			return commands;
		}

		@Override
		public boolean matches(String spoken) {
			return commands.contains(spoken);
		}
	}

	public static class CommandMatcher extends Command {
		protected final Predicate<String> matcher;
		protected final Set<String> keywords;
		protected final Set<String> commands;

		public CommandMatcher(Module owner, Set<String> commands, Set<String> keywords, Predicate<String> matcher) {
			super(owner);
			this.keywords = keywords;
			this.matcher = matcher;
			this.commands = commands;
		}

		@Override
		public boolean matches(String spoken) {
			return matcher.test(spoken);
		}

		@Override
		public Set<String> getKeywords() {
			return keywords;
		}

		@Override
		public Set<String> getCommands() {
			return commands;
		}
	}
}
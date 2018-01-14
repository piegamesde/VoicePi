package de.piegames.picontrol;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * */
public class KnowledgeBase {

	private Set<String>	commands;
	private Path		corpus, lm, dict;

	// TODO save last commands so it only downloads if something actually changed
	public KnowledgeBase(Collection<String> commands) {
		this.commands = new HashSet<>(commands);
	}

	public Path getLanguageModel() {
		return lm;
	}

	public Path getDictionary() {
		return dict;
	}
}
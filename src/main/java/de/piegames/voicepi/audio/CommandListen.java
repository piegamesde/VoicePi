package de.piegames.voicepi.audio;

import java.util.Objects;

public class CommandListen {

	protected CircularByteBuffer buffer;

	public CommandListen(CircularByteBuffer buffer) {
		this.buffer = Objects.requireNonNull(buffer);

	}
}
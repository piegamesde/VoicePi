package de.piegames.voicepi.audio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.TargetDataLine;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ToggleAudioInputStream extends AudioInputStream {

	public static enum State {
		LISTENING, DEAF, CUT_SILENCE, EOF;
	}

	public final ObjectProperty<State>	state	= new SimpleObjectProperty<ToggleAudioInputStream.State>(State.LISTENING);
	protected boolean					deaf, cutSilence;

	public ToggleAudioInputStream(TargetDataLine line) {
		super(line);
	}

	public ToggleAudioInputStream(InputStream stream, AudioFormat format, long length) {
		super(stream, format, length);
	}

	@Override
	public int read() throws IOException {
		throw new UnsupportedOperationException("Single reads are not possible");
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		switch (state.get()) {
			case LISTENING:
				return super.read(b, off, len);
			case DEAF:
				int read = (int) super.skip(len);
				return 0;
			case CUT_SILENCE:
				read = (int) super.skip(len);
				Arrays.fill(b, off, off + read, (byte) 0);
				return read;
			case EOF:
				return -1;
			default:
				throw new IllegalStateException(state.get() + " is not a valid state");
		}
	}
}

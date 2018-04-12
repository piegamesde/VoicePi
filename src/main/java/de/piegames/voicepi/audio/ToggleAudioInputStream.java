package de.piegames.voicepi.audio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.TargetDataLine;

public class ToggleAudioInputStream extends AudioInputStream {

	protected boolean deaf;

	public ToggleAudioInputStream(TargetDataLine line) {
		super(line);
	}

	public ToggleAudioInputStream(InputStream stream, AudioFormat format, long length) {
		super(stream, format, length);
	}

	@Override
	public int read() throws IOException {
		int ret = super.read();
		return deaf ? 0 : ret;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = super.read(b, off, len);
		if (read == -1)
			return read;
		if (deaf)
			Arrays.fill(b, off, off + read, (byte) 0);
		return read;
	}

	public void setDeaf(boolean deaf) {
		this.deaf = deaf;
	}
}

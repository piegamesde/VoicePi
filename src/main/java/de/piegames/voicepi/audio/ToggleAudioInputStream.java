package de.piegames.voicepi.audio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.TargetDataLine;

public class ToggleAudioInputStream extends AudioInputStream {

	protected boolean deaf, cutSilence;

	public ToggleAudioInputStream(TargetDataLine line, boolean cutSilence) {
		super(line);
		this.cutSilence = cutSilence;
	}

	public ToggleAudioInputStream(InputStream stream, AudioFormat format, long length, boolean cutSilence) {
		super(stream, format, length);
		this.cutSilence = cutSilence;
	}

	@Override
	public int read() throws IOException {
		int ret = super.read();
		return deaf ? 0 : ret;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (deaf) {
			int read = (int) super.skip(len);
			if (cutSilence)
				return 0;
			else {
				Arrays.fill(b, off, off + read, (byte) 0);
				return read;
			}
		} else
			return super.read(b, off, len);
	}

	public void setDeaf(boolean deaf) {
		this.deaf = deaf;
	}
}

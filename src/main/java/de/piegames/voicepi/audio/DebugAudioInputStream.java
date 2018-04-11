package de.piegames.voicepi.audio;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class DebugAudioInputStream extends AudioInputStream {

	protected SourceDataLine line;

	public DebugAudioInputStream(TargetDataLine target) throws LineUnavailableException {
		super(target);
		line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, target.getFormat()));
		line.open(target.getFormat());
		line.start();
	}

	public DebugAudioInputStream(InputStream stream, AudioFormat format, long length) throws LineUnavailableException {
		super(stream, format, length);
		line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
		line.open(format);
		line.start();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = super.read(b, off, len);
		if (read == -1)
			return read;
		return read;
	}

	@Override
	public void close() throws IOException {
		super.close();
		line.stop();
		line.close();
	}
}
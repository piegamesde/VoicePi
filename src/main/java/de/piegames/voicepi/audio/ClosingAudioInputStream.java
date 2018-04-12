package de.piegames.voicepi.audio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.TargetDataLine;

public class ClosingAudioInputStream extends AudioInputStream {

	protected VolumeSpeechDetector volume;
	protected boolean				waitForSpeak;

	public ClosingAudioInputStream(TargetDataLine line, VolumeSpeechDetector volume, boolean waitForSpeak) {
		super(line);
		this.volume = Objects.requireNonNull(volume);
		this.waitForSpeak = waitForSpeak;
	}

	public ClosingAudioInputStream(InputStream stream, AudioFormat format, long length, VolumeSpeechDetector volume, boolean waitForSpeak) {
		super(stream, format, length);
		this.volume = Objects.requireNonNull(volume);
		this.waitForSpeak = waitForSpeak;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = super.read(b, off, len);
		if (read == -1)
			return read;
		float rms = 0;
		for (int i = off; i < off + len;) {
			int sample = 0;
			sample |= b[i++] & 0xFF; // (reverse these two lines
			sample |= b[i++] << 8; // if the format is big endian)
			rms += (sample / 32768f) * (sample / 32768f);
		}
		rms = (float) Math.sqrt(rms / (read / 2));
		volume.onSample(rms);
		if (volume.isSpeaking() ^ !waitForSpeak) {
			close();
		}

		return read;
	}
}
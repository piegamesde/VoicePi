package de.piegames.voicepi.audio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.TargetDataLine;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class RMSAudioInputStream extends AudioInputStream {

	protected VolumeSpeechDetector									volume;
	public final ReadOnlyObjectProperty<VolumeSpeechDetector.State>	state;
	private ReadOnlyObjectWrapper<VolumeSpeechDetector.State>		writableState;

	public RMSAudioInputStream(TargetDataLine line, VolumeSpeechDetector volume) {
		super(line);
		this.volume = Objects.requireNonNull(volume);
		writableState = new ReadOnlyObjectWrapper<>(volume.getState());
		writableState.bind(volume.state);
		state = writableState.getReadOnlyProperty();
	}

	public RMSAudioInputStream(InputStream stream, AudioFormat format, long length, VolumeSpeechDetector volume) {
		super(stream, format, length);
		this.volume = Objects.requireNonNull(volume);
		writableState = new ReadOnlyObjectWrapper<>(volume.getState());
		writableState.bind(volume.state);
		state = writableState.getReadOnlyProperty();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = super.read(b, off, len);
		if (read < 1)
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
		return read;
	}
}
package de.piegames.voicepi.audio;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.sound.sampled.AudioFormat;

public class RMSInputStream extends FilterInputStream {

	protected final AudioFormat				format;
	protected final VolumeSpeechDetector	volume;

	public RMSInputStream(InputStream stream, AudioFormat format, VolumeSpeechDetector volume) {
		super(stream);
		this.format = Objects.requireNonNull(format);
		this.volume = Objects.requireNonNull(volume);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = in.read(b, off, len);
		if (read < 1)
			return read;
		float rms = 0;
		for (int i = off; i < off + len;) {
			int sample = 0;
			sample |= b[i++] & 0xFF; // (reverse these two lines
			sample |= b[i++] << 8; // if the format is big endian)
			rms += (sample / 32768f) * (sample / 32768f);
			// System.out.println(sample / 32768f);
		}
		rms = (float) Math.sqrt(rms / (read / 2));
		volume.onSample(rms);

		return read;
	}

	protected byte[] buffer = new byte[4096];

	@Override
	public long skip(long n) throws IOException {
		int skipped = 0;
		while (n > 4096) {
			skipped += read(buffer, 0, 4096);
			n -= 4096;
		}
		skipped += read(buffer, 0, (int) n);
		return skipped;
	}

	@Override
	public int read() throws IOException {
		throw new UnsupportedOperationException("You cannot read only one byte on audio data");
	}
}
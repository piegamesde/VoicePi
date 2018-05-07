package de.piegames.voicepi.audio;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javafx.beans.property.SimpleObjectProperty;

/**
 * An InputStream that when fed with audio data in the correct format (16bit unsigned PCM little endian) will calculate the root-mean-square (RMS, a measurement
 * of audio volume) of each read chunk and pass it to a given {@link VolumeSpeechDetector}.
 */
public class RMSInputStream extends FilterInputStream {

	protected final AudioFormat							format;
	public final SimpleObjectProperty<Consumer<Float>>	callback	= new SimpleObjectProperty<>();;

	public RMSInputStream(InputStream stream, AudioFormat format, Consumer<Float> callback) {
		super(stream);
		this.format = Objects.requireNonNull(format);
		this.callback.set(callback);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		Consumer<Float> callback = this.callback.get();
		if (callback == null)
			return in.read(b, off, len);

		int read = in.read(b, off, len);
		if (read < 1)
			return read;
		float rms = 0;
		// Decoding this live is way more performant than to require a fixed format and format the whole stream by the system

		Encoding encode = format.getEncoding();
		int bps = format.getSampleSizeInBits() / 8;

		if (encode == Encoding.PCM_SIGNED) {
			for (int i = off; i < off + len;) {
				int sample = 0;
				if (format.isBigEndian()) {
					sample |= b[i++] << 8; // if the format is big endian)
					sample |= b[i++] & 0xFF; // (reverse these two lines
				} else {
					sample |= b[i++] & 0xFF; // (reverse these two lines
					sample |= b[i++] << 8; // if the format is big endian)
				}
				rms += (sample / 32768f) * (sample / 32768f);
			}
		} else if (format.getEncoding() == Encoding.PCM_FLOAT) {
			for (int i = off; i < off + len;) {
				int sample = 0;
				if (format.isBigEndian()) {
					sample |= (b[i++] & 0xFF) << 24; // is big endian)
					sample |= (b[i++] & 0xFF) << 16; // if the format
					sample |= (b[i++] & 0xFF) << 8; // four lines
					sample |= b[i++] & 0xFF; // (reverse these
				} else {
					sample |= b[i++] & 0xFF; // (reverse these
					sample |= (b[i++] & 0xFF) << 8; // four lines
					sample |= (b[i++] & 0xFF) << 16; // if the format
					sample |= (b[i++] & 0xFF) << 24; // is big endian)
				}
				rms += Float.intBitsToFloat(sample) * Float.intBitsToFloat(sample);
			}
		}
		rms = (float) Math.sqrt(rms * bps / read);
		callback.accept(rms);
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
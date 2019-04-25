package de.piegames.voicepi.audio;

import java.io.IOException;
import java.util.Optional;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Implements the {@link Audio} class using Java's javax.sound package which will use whatever is available on the current system. */
public class DefaultAudio extends Audio {

	protected AudioFormat format;

	public DefaultAudio(JsonObject config) {
		super(config);
		this.format = new AudioFormat(
				Optional.ofNullable(config.getAsJsonPrimitive("sample-rate")).map(JsonPrimitive::getAsFloat).orElse(FORMAT.getSampleRate()),
				Optional.ofNullable(config.getAsJsonPrimitive("sample-size")).map(JsonPrimitive::getAsInt).orElse(FORMAT.getSampleSizeInBits()),
				Optional.ofNullable(config.getAsJsonPrimitive("channels")).map(JsonPrimitive::getAsInt).orElse(FORMAT.getChannels()),
				Optional.ofNullable(config.getAsJsonPrimitive("signed")).map(JsonPrimitive::getAsBoolean).orElse(true),
				Optional.ofNullable(config.getAsJsonPrimitive("big-endian")).map(JsonPrimitive::getAsBoolean).orElse(false));
	}

	@Override
	public AudioInputStream normalListening(AudioFormat targetEncoding) throws IOException {
		try {
			if (AudioSystem.isLineSupported(new DataLine.Info(TargetDataLine.class, targetEncoding))) {
				TargetDataLine line = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, targetEncoding));
				line.open(format);
				line.start();
				AudioInputStream stream = new AudioInputStream(line);
				return stream;
			} else {
				TargetDataLine line = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
				line.open(format);
				line.start();
				AudioInputStream stream = new AudioInputStream(line);
				stream = formatStream(stream, targetEncoding);
				return stream;
			}
		} catch (LineUnavailableException e) {
			throw new IOException(e);
		}
	}

	@Override
	public CircularBufferInputStream normalListening2() throws IOException {
		AudioInputStream ain = normalListening(format);
		CircularByteBuffer buffer = new CircularByteBuffer(getCommandBufferSize());
		CircularBufferInputStream in = new CircularBufferInputStream(buffer) {

			@Override
			public int read(byte[] src, int off, int len) throws IOException {
				if (buffer == null)
					return -1;
				int read = ain.read(src, off, len);
				if (read < 1)
					return read;
				buffer.put(src, off, read);
				buffer.skip(read);
				return read;
			}

			@Override
			public void close() throws IOException {
				ain.close();
				super.close();
			}
		};
		return in;
	}

	@Override
	public void play(AudioInputStream ais) throws IOException {
		AudioFormat audioFormat = ais.getFormat();
		try {
			SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, audioFormat));
			line.open(audioFormat);
			line.start();

			int count = 0;
			byte[] data = new byte[4096];
			while ((count = ais.read(data)) != -1 && !Thread.interrupted())
				line.write(data, 0, count);

			line.drain();
			line.close();
		} catch (LineUnavailableException e) {
			throw new IOException(e);
		}
	}

	@Override
	public AudioFormat getListeningFormat() {
		return format;
	}
}

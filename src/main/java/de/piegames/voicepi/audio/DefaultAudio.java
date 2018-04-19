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
	public AudioInputStream normalListening() throws LineUnavailableException {
		TargetDataLine line = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
		line.open(format);
		line.start();
		AudioInputStream stream = new AudioInputStream(line);
		// stream = formatStream(stream);
		return stream;
	}

	@Override
	public CircularBufferInputStream normalListening2() throws LineUnavailableException, IOException {
		TargetDataLine line = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
		line.open(format);
		line.start();
		CircularByteBuffer buffer = new CircularByteBuffer(1024 * 256);
		CircularBufferInputStream in = new CircularBufferInputStream(buffer) {

			@Override 
			public int read(byte[] src, int off, int len) {
				if (buffer == null)
					return -1;
				int read = line.read(src, off, len);
				if (read < 1) 
					return read;
				buffer.put(src, off, read);
				buffer.skip(read);
				return read;
			}

			@Override
			public void close() throws IOException {
				System.out.println("CLOSE");
				line.stop();
				line.close();
				super.close();
			}
		};
		return in;
	}

	@Override
	public void play(AudioInputStream ais) throws LineUnavailableException, IOException {
		AudioFormat audioFormat = ais.getFormat();
		SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, audioFormat));
		line.open(audioFormat);
		line.start();

		int count = 0;
		byte[] data = new byte[65532];
		while ((count = ais.read(data)) != -1)
			line.write(data, 0, count);

		line.drain();
		line.close();
	}

	@Override
	public AudioFormat getListeningFormat() {
		return format;
	}
}

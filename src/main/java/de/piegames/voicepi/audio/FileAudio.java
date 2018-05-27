package de.piegames.voicepi.audio;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.piegames.voicepi.Settings;

public class FileAudio extends Audio {

	protected File			inFile, outFile;
	protected AudioFormat	format;

	public FileAudio(JsonObject config) {
		super(config);
		inFile = new File(Optional.ofNullable(config.getAsJsonPrimitive("in-file")).map(JsonPrimitive::getAsString).orElse("in.wav"));
		outFile = Optional.ofNullable(config.getAsJsonPrimitive("out-file")).map(JsonPrimitive::getAsString).map(File::new).orElse(null);
	}

	public FileAudio(JsonObject config, File inFile, File outFile) {
		super(config);
		this.inFile = Objects.requireNonNull(inFile);
		this.outFile = outFile;
	}

	@Override
	public void init(Settings settings) throws IOException {
		try {
			format = AudioSystem.getAudioFileFormat(inFile).getFormat();
		} catch (UnsupportedAudioFileException e) {
			throw new IOException(e);
		}
	}

	@Override
	public AudioInputStream normalListening(AudioFormat targetFormat) throws IOException {
		try {
			return formatStream(AudioSystem.getAudioInputStream(inFile), targetFormat);
		} catch (UnsupportedAudioFileException e) {
			throw new IOException(e);
		}
	}

	@Override
	public CircularBufferInputStream normalListening2() throws IOException {
		AudioInputStream ain;
		try {
			ain = AudioSystem.getAudioInputStream(inFile);
		} catch (UnsupportedAudioFileException e) {
			throw new IOException(e);
		}
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
	public void play(AudioInputStream stream) throws IOException {
		if (outFile != null)
			AudioSystem.write(stream, Type.WAVE, outFile);
	}

	public void setOutFile(File file) {
		this.outFile = file;
	}

	public void setInFile(File file) throws IOException {
		this.inFile = Objects.requireNonNull(file);
		init(null);
	}

	@Override
	public AudioFormat getListeningFormat() {
		return format;
	}
}

package de.piegames.voicepi.audio;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.apache.commons.io.input.ClosedInputStream;
import com.google.gson.JsonObject;


public class NoAudio extends Audio {

	public NoAudio(JsonObject config) {
		super(config);
	}

	@Override
	public AudioInputStream normalListening(AudioFormat targetFormat) throws IOException {
		return new AudioInputStream(new ClosedInputStream(), FORMAT, AudioSystem.NOT_SPECIFIED);
	}

	@Override
	public CircularBufferInputStream normalListening2() throws IOException {
		return new CircularBufferInputStream(new CircularByteBuffer());
	}

	@Override
	public void play(AudioInputStream stream) throws IOException {
	}

	@Override
	public AudioFormat getListeningFormat() {
		return FORMAT;
	}
}

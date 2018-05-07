package de.piegames.voicepi.stt;

import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import de.piegames.voicepi.audio.Audio;
import de.piegames.voicepi.audio.ToggleAudioInputStream;
import de.piegames.voicepi.audio.ToggleAudioInputStream.State;
import edu.cmu.sphinx.api.AbstractSpeechRecognizer;
import edu.cmu.sphinx.api.Configuration;

public class SphinxSpeechRecognizer extends AbstractSpeechRecognizer {

	private Audio					in;
	private ToggleAudioInputStream	inputStream;

	public SphinxSpeechRecognizer(Configuration configuration, Audio in) throws IOException {
		super(configuration);
		this.in = in;
	}

	public void startRecognition(boolean clear) throws LineUnavailableException, IOException {
		AudioInputStream stream = in.normalListening(Audio.FORMAT);
		inputStream = new ToggleAudioInputStream(stream, stream.getFormat(), AudioSystem.NOT_SPECIFIED);
		// context.getInstance(StreamDataSource.class)
		// .setInputStream(inputStream);
		recognizer.allocate();
		context.setSpeechSource(inputStream);
	}

	public void stopRecognition1() throws IOException {
		inputStream.state.set(State.EOF);
		inputStream.close();
	}

	public void stopRecognition2() throws IOException {
		recognizer.deallocate();
	}

	public void setDeaf(boolean deaf) {
		inputStream.state.set(deaf ? State.DEAF : State.LISTENING);
	}
}

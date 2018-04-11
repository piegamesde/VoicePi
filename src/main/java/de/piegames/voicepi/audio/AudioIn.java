package de.piegames.voicepi.audio;

import java.io.IOException;
import java.util.Objects;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import org.apache.commons.io.input.CloseShieldInputStream;

public abstract class AudioIn {

	public static final AudioFormat	PCM_FORMAT	= new AudioFormat(8000, 16, 1, true, false);

	protected AudioFormat			format;
	protected VolumeSpeechDetector	volume;

	public AudioIn(AudioFormat format) {
		this.format = Objects.requireNonNull(format);
		volume = new VolumeSpeechDetector(100, 500);
	}

	/** This will start listening until the returned {@code AudioInputStream} is closed */
	public abstract AudioInputStream normalListening() throws LineUnavailableException;

	/**
	 * This will start listening until a command was spoken or {@code timeout} seconds passed
	 *
	 * @throws LineUnavailableException
	 */
	public abstract AudioInputStream activeListening(int timeout) throws LineUnavailableException;

	/**
	 * This will wait until a command gets spoken, then return and automatically stop listening once the command is over
	 *
	 * @throws LineUnavailableException
	 * @throws IOException
	 */
	public abstract AudioInputStream passiveListening() throws LineUnavailableException, IOException;

	public static class DefaultIn extends AudioIn {

		public DefaultIn(AudioFormat format) {
			super(format);
		}

		@Override
		public AudioInputStream normalListening() throws LineUnavailableException {
			TargetDataLine line = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
			line.open(format);
			line.start();
			return new AudioInputStream(line);
		}

		@Override
		public AudioInputStream activeListening(int timeout) throws LineUnavailableException {
			return new ClosingAudioInputStream(
					AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, AudioSystem.getAudioInputStream(PCM_FORMAT, normalListening())),
					AudioIn.PCM_FORMAT,
					AudioSystem.NOT_SPECIFIED,
					volume);
		}

		@Override
		public AudioInputStream passiveListening() throws LineUnavailableException, IOException {
			AudioInputStream stream = AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, AudioSystem.getAudioInputStream(PCM_FORMAT, normalListening()));
			ClosingAudioInputStream wait = new ClosingAudioInputStream(new CloseShieldInputStream(stream), PCM_FORMAT, AudioSystem.NOT_SPECIFIED, volume);
			byte[] buffer = new byte[1024];
			while (wait.read(buffer) != -1)
				;
			wait.close();// Actually not needed
			volume.startSpeaking();
			return new ClosingAudioInputStream(stream, PCM_FORMAT, AudioSystem.NOT_SPECIFIED, volume);
		}
	}

	public static class JackIn extends AudioIn {

		public JackIn(AudioFormat format) {
			super(format);
		}

		@Override
		public AudioInputStream normalListening() throws LineUnavailableException {
			return null;
		}

		@Override
		public AudioInputStream activeListening(int timeout) {
			return null;
		}

		@Override
		public AudioInputStream passiveListening() {
			return null;
		}
	}
}
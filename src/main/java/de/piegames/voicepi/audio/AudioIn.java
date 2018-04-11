package de.piegames.voicepi.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public abstract class AudioIn {

	public AudioIn() {
	}

	public abstract AudioInputStream startListening() throws LineUnavailableException;

	public abstract AudioInputStream activeListening(int timeout);

	public static class DefaultIn extends AudioIn {

		@Override
		public AudioInputStream startListening() throws LineUnavailableException {
			AudioFormat format = new AudioFormat(8000, 16, 1, true, false);
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format);
			line.start(); // start capturing
			AudioInputStream stream = new AudioInputStream(line);
			return stream;
			// TODO close it somehow
		}

		@Override
		public AudioInputStream activeListening(int timeout) {
			return null;
		}
	}

	public static class JackIn extends AudioIn {

		@Override
		public AudioInputStream startListening() throws LineUnavailableException {
			return null;
		}

		@Override
		public AudioInputStream activeListening(int timeout) {
			return null;
		}
	}
}
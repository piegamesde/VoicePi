package de.piegames.voicepi.tts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.google.gson.JsonObject;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.AudioPlayer;
import de.piegames.voicepi.VoicePi;

public class FreeSpeechEngine extends SpeechEngine {

	private Voice					voice;
	private InputStreamAudioPlayer	player;

	public FreeSpeechEngine(VoicePi control, JsonObject config) throws IOException, UnsupportedAudioFileException {
		super(control, config);
		VoiceManager voiceManager = VoiceManager.getInstance();
		voice = voiceManager.getVoice("kevin16");
		player = new InputStreamAudioPlayer();
		voice.setAudioPlayer(player);
		voice.allocate();
	}

	@Override
	public AudioInputStream generateAudio(String text) {
		voice.speak(text);
		return player.getStream();
	}

	public class InputStreamAudioPlayer implements AudioPlayer {

		private AudioFormat			currentFormat;
		private byte[]				outputData;
		private int					curIndex;
		private int					totBytes;
		private Vector<InputStream>	outputList;

		public InputStreamAudioPlayer() {
			this.currentFormat = null;
			this.curIndex = 0;
			this.totBytes = 0;
			this.outputList = new Vector<>();
		}

		@Override
		public synchronized void setAudioFormat(AudioFormat format) {
			this.currentFormat = format;
		}

		@Override
		public AudioFormat getAudioFormat() {
			return this.currentFormat;
		}

		@Override
		public void pause() {
		}

		@Override
		public synchronized void resume() {
		}

		@Override
		public synchronized void cancel() {
		}

		@Override
		public synchronized void reset() {
		}

		@Override
		public void startFirstSampleTimer() {
		}

		@Override
		public synchronized void close() {
		}

		public synchronized AudioInputStream getStream() {
			try {
				SequenceInputStream is = new SequenceInputStream(this.outputList.elements());
				AudioInputStream ais = new AudioInputStream(is, this.currentFormat,
						this.totBytes / this.currentFormat.getFrameSize());
				// System.out.println("Wrote synthesized speech to " + this.baseName);
				// AudioSystem.write(ais, this.outputType, iae);
				return ais;
			} catch (IllegalArgumentException arg4) {
				log.warn("Cannot create audio input stream", arg4);
				// System.err.println("Can\'t write audio type " + this.outputType);
				return null;
			}
		}

		@Override
		public float getVolume() {
			return 1.0F;
		}

		@Override
		public void setVolume(float volume) {
		}

		@Override
		public void begin(int size) {
			this.outputData = new byte[size];
			this.curIndex = 0;
		}

		@Override
		public boolean end() {
			this.outputList.add(new ByteArrayInputStream(this.outputData));
			this.totBytes += this.outputData.length;
			return true;
		}

		@Override
		public boolean drain() {
			return true;
		}

		@Override
		public synchronized long getTime() {
			return -1L;
		}

		@Override
		public synchronized void resetTime() {
		}

		@Override
		public boolean write(byte[] audioData) {
			return this.write(audioData, 0, audioData.length);
		}

		@Override
		public boolean write(byte[] bytes, int offset, int size) {
			System.arraycopy(bytes, offset, this.outputData, this.curIndex, size);
			this.curIndex += size;
			return true;
		}

		@Override
		public String toString() {
			return "FileAudioPlayer";
		}

		@Override
		public void showMetrics() {
		}
	}
}

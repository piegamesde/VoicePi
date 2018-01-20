package de.piegames.picontrol.tts;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Vector;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.AudioPlayer;
import com.sun.speech.freetts.util.Utilities;
import de.piegames.picontrol.PiControl;

public class FreeSpeechEngine extends SpeechEngine {

	private Voice					voice;
	private InputStreamAudioPlayer	player;

	public FreeSpeechEngine(PiControl control) throws IOException, UnsupportedAudioFileException {
		super(control);
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
		private String				baseName;
		private byte[]				outputData;
		private int					curIndex;
		private int					totBytes;
		private Type				outputType;
		private Vector<InputStream>	outputList;

		public InputStreamAudioPlayer(String baseName, Type type) {
			this.currentFormat = null;
			this.curIndex = 0;
			this.totBytes = 0;
			this.baseName = baseName + "." + type.getExtension();
			this.outputType = type;
			this.outputList = new Vector<>();
		}

		public InputStreamAudioPlayer() {
			this(Utilities.getProperty("com.sun.speech.freetts.AudioPlayer.baseName", "freetts"), Type.WAVE);
		}

		public synchronized void setAudioFormat(AudioFormat format) {
			this.currentFormat = format;
		}

		public AudioFormat getAudioFormat() {
			return this.currentFormat;
		}

		public void pause() {
		}

		public synchronized void resume() {
		}

		public synchronized void cancel() {
		}

		public synchronized void reset() {
		}

		public void startFirstSampleTimer() {
		}

		public synchronized void close() {
		}

		public synchronized AudioInputStream getStream() {
			try {
				SequenceInputStream is = new SequenceInputStream(this.outputList.elements());
				AudioInputStream ais = new AudioInputStream(is, this.currentFormat,
						(long) (this.totBytes / this.currentFormat.getFrameSize()));
				System.out.println("Wrote synthesized speech to " + this.baseName);
				// AudioSystem.write(ais, this.outputType, iae);
				return ais;
			} catch (IllegalArgumentException arg4) {
				System.err.println("Can\'t write audio type " + this.outputType);
				return null;
			}
		}

		public float getVolume() {
			return 1.0F;
		}

		public void setVolume(float volume) {
		}

		public void begin(int size) {
			this.outputData = new byte[size];
			this.curIndex = 0;
		}

		public boolean end() {
			this.outputList.add(new ByteArrayInputStream(this.outputData));
			this.totBytes += this.outputData.length;
			return true;
		}

		public boolean drain() {
			return true;
		}

		public synchronized long getTime() {
			return -1L;
		}

		public synchronized void resetTime() {
		}

		public boolean write(byte[] audioData) {
			return this.write(audioData, 0, audioData.length);
		}

		public boolean write(byte[] bytes, int offset, int size) {
			System.arraycopy(bytes, offset, this.outputData, this.curIndex, size);
			this.curIndex += size;
			return true;
		}

		public String toString() {
			return "FileAudioPlayer";
		}

		public void showMetrics() {
		}
	}
}

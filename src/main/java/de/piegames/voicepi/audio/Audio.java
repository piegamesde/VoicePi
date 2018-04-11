package de.piegames.voicepi.audio;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.FloatBuffer;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackOptions;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;
import org.jaudiolibs.jnajack.JackProcessCallback;
import org.jaudiolibs.jnajack.JackSampleRateCallback;
import org.jaudiolibs.jnajack.JackStatus;

public abstract class Audio {

	/** The default format. It will be used for all audio processing and if possible for audio recording. */
	public static final AudioFormat	FORMAT	= new AudioFormat(16000, 16, 1, true, false);

	protected AudioFormat			format;
	protected VolumeSpeechDetector	volume;

	public Audio() {
		this(FORMAT);
	}

	public Audio(AudioFormat format) {
		this.format = Objects.requireNonNull(format);
		volume = new VolumeSpeechDetector(100, 500);
	}

	/**
	 * This will start listening until the returned {@code AudioInputStream} is closed
	 *
	 * @throws IOException
	 */
	public abstract AudioInputStream normalListening() throws LineUnavailableException, IOException;

	/**
	 * This will start listening until a command was spoken or {@code timeout} seconds passed
	 *
	 * @throws LineUnavailableException
	 * @throws IOException
	 */
	public abstract AudioInputStream activeListening(int timeout) throws LineUnavailableException, IOException;

	/**
	 * This will start listening until a command was spoken or {@code timeout} seconds passed and return the recorded data.
	 *
	 * @throws LineUnavailableException
	 * @throws IOException
	 */
	// public abstract byte[] activeListeningRaw(int timeout) throws LineUnavailableException, IOException;

	/**
	 * This will wait until a command gets spoken, then return and automatically stop listening once the command is over
	 *
	 * @throws LineUnavailableException
	 * @throws IOException
	 */
	public abstract AudioInputStream passiveListening() throws LineUnavailableException, IOException;

	public abstract void play(AudioInputStream stream) throws LineUnavailableException, IOException, InterruptedException;

	public void init() throws JackException {
	}

	public void close() throws JackException, IOException {
	}

	public static class DefaultAudio extends Audio {

		public DefaultAudio(AudioFormat format) {
			super(format);
		}

		@Override
		public AudioInputStream normalListening() throws LineUnavailableException {
			TargetDataLine line = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
			line.open(format);
			line.start(); // start capturing
			AudioInputStream stream = new AudioInputStream(line);
			stream = formatStream(stream);
			return stream;
		}

		@Override
		public AudioInputStream activeListening(int timeout) throws LineUnavailableException {
			return new ClosingAudioInputStream(
					AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, AudioSystem.getAudioInputStream(FORMAT, normalListening())),
					Audio.FORMAT,
					AudioSystem.NOT_SPECIFIED,
					volume);
		}

		@Override
		public AudioInputStream passiveListening() throws LineUnavailableException, IOException {
			AudioInputStream stream = formatStream(normalListening());
			ClosingAudioInputStream wait = new ClosingAudioInputStream(new CloseShieldInputStream(stream), FORMAT, AudioSystem.NOT_SPECIFIED, volume);
			byte[] buffer = new byte[1024];
			while (wait.read(buffer) != -1)
				;
			wait.close();// Actually not needed
			volume.startSpeaking();
			return new ClosingAudioInputStream(stream, FORMAT, AudioSystem.NOT_SPECIFIED, volume);
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
	}

	public static class JackAudio extends Audio implements JackProcessCallback, JackSampleRateCallback {

		protected JackClient				client;
		protected JackPort					out, in;
		protected int						sampleRate;
		protected Queue<AudioInputStream>	outQueue	= new LinkedList<>();
		protected Queue<OutputStream>		inQueue		= new LinkedList<>();

		public JackAudio(AudioFormat format) {
			super(format);
		}

		@Override
		public void init() throws JackException {
			Jack jack = Jack.getInstance();
			client = jack.openClient("VoicePi", EnumSet.noneOf(JackOptions.class), EnumSet.noneOf(JackStatus.class));
			sampleRate = client.getSampleRate();
			in = client.registerPort("in", JackPortType.AUDIO, EnumSet.of(JackPortFlags.JackPortIsInput));
			out = client.registerPort("out", JackPortType.AUDIO, EnumSet.of(JackPortFlags.JackPortIsOutput));
			client.setSampleRateCallback(this);
			client.setProcessCallback(this);
			client.activate();
			client.transportStart();
		}

		@Override
		public void close() throws JackException, IOException {
			client.transportStop();
			client.deactivate();
			synchronized (outQueue) {
				for (AudioInputStream in : outQueue)
					in.close();
				outQueue.clear();
			}
		}

		@Override
		public AudioInputStream normalListening() throws LineUnavailableException, IOException {
			PipedOutputStream pout = new PipedOutputStream();
			PipedInputStream pin = new PipedInputStream(pout, 1024 * 16);
			inQueue.add(pout);
			AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
			AudioInputStream audio = new AudioInputStream(pin, format, AudioSystem.NOT_SPECIFIED);
			audio = formatStream(audio);
			return audio;
		}

		@Override
		public AudioInputStream activeListening(int timeout) throws LineUnavailableException, IOException {
			return new ClosingAudioInputStream(normalListening(),
					Audio.FORMAT,
					AudioSystem.NOT_SPECIFIED,
					volume);
		}

		@Override
		public AudioInputStream passiveListening() throws LineUnavailableException, IOException {
			AudioInputStream stream = formatStream(normalListening());
			ClosingAudioInputStream wait = new ClosingAudioInputStream(new CloseShieldInputStream(stream), stream.getFormat(), AudioSystem.NOT_SPECIFIED, volume);
			byte[] buffer = new byte[1024];
			while (wait.read(buffer) != -1)
				;
			wait.close();// Actually not needed
			volume.startSpeaking();
			return new ClosingAudioInputStream(stream, FORMAT, AudioSystem.NOT_SPECIFIED, volume);
		}

		@Override
		public void play(AudioInputStream stream) throws InterruptedException {
			// stream = formatStream(stream);
			synchronized (outQueue) {
				outQueue.add(stream);
			}
			// TODO this won't wait but it should
		}

		@Override
		public boolean process(JackClient client, int samples) {
			// Process out
			synchronized (outQueue) {
				if (!outQueue.isEmpty()) {
					FloatBuffer outData = out.getFloatBuffer();
					try {
						AudioInputStream stream = outQueue.peek();
						byte[] buffer = new byte[samples * 2];
						int position = 0;
						while (position < samples * 2) {
							int read = stream.read(buffer, position, samples * 2 - position);
							for (int i = position; i < position + read;) {
								int sample = 0;
								sample |= buffer[i++] & 0xFF; // (reverse these two lines
								sample |= buffer[i++] << 8; // if the format is big endian)
								outData.put(sample / 32768f);
							}
							position += read;
							if (read < samples * 2) {
								stream.close();
								outQueue.poll();
								if (outQueue.isEmpty())
									break;
								stream = outQueue.peek();
							}
						}
					} catch (IOException e) {
						// TODO exception handling
						e.printStackTrace();
					}
				}
			}
			{// Process in
				FloatBuffer inData = in.getFloatBuffer();
				byte[] buffer = new byte[inData.capacity() * 2];
				for (int i = 0; i < inData.capacity(); i++) {
					int sample = Math.round(inData.get(i) * 32767);
					buffer[i * 2] = (byte) sample;
					buffer[i * 2 + 1] = (byte) (sample >> 8);
				}

				inQueue.removeIf(out -> {
					try {
						out.write(buffer, 0, buffer.length);
					} catch (IOException e) {
						e.printStackTrace();
						return true;
					}
					return false;
				});
			}
			return true;
		}

		@Override
		public void sampleRateChanged(JackClient client, int sampleRate) {
			if (client == JackAudio.this.client)
				this.sampleRate = sampleRate;
		}
	}

	public static AudioInputStream formatStream(AudioInputStream in) {
		if (!in.getFormat().equals(FORMAT))
			in = AudioSystem.getAudioInputStream(FORMAT, in);
		return in;
	}
}
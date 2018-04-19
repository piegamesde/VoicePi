package de.piegames.voicepi.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import org.greenrobot.essentials.io.CircularByteBuffer;
import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackBufferSizeCallback;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackOptions;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;
import org.jaudiolibs.jnajack.JackProcessCallback;
import org.jaudiolibs.jnajack.JackSampleRateCallback;
import org.jaudiolibs.jnajack.JackStatus;
import com.google.api.client.util.IOUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.piegames.voicepi.audio.VolumeSpeechDetector.State;
import javafx.util.Pair;

public abstract class Audio {

	/** The default format. It will be used for all audio processing and if possible for audio recording. */
	public static final AudioFormat	FORMAT	= new AudioFormat(16000, 16, 1, true, false);
	// public static final AudioFormat FORMAT = new AudioFormat(48000, 16, 1, true, false);

	protected AudioFormat			format;

	public Audio(JsonObject config) {
		this.format = new AudioFormat(
				Optional.ofNullable(config.getAsJsonPrimitive("sample-rate")).map(JsonPrimitive::getAsFloat).orElse(FORMAT.getSampleRate()),
				Optional.ofNullable(config.getAsJsonPrimitive("sample-size")).map(JsonPrimitive::getAsInt).orElse(FORMAT.getSampleSizeInBits()),
				Optional.ofNullable(config.getAsJsonPrimitive("channels")).map(JsonPrimitive::getAsInt).orElse(FORMAT.getChannels()),
				Optional.ofNullable(config.getAsJsonPrimitive("signed")).map(JsonPrimitive::getAsBoolean).orElse(true),
				Optional.ofNullable(config.getAsJsonPrimitive("big-endian")).map(JsonPrimitive::getAsBoolean).orElse(false));
		// TODO configure
	}

	/**
	 * This will start listening until the returned {@code AudioInputStream} is closed
	 *
	 * @throws IOException
	 */
	public abstract AudioInputStream normalListening() throws LineUnavailableException, IOException;

	/**
	 * This will wait until a command gets spoken, then return and automatically stop listening once the command is over
	 *
	 * @throws LineUnavailableException
	 * @throws IOException
	 */
	public Pair<AudioInputStream, VolumeSpeechDetector> listenCommand() throws LineUnavailableException, IOException {
		// TODO optimize this all, cache buffers, make constants settings
		AudioInputStream stream = normalListening();
		VolumeSpeechDetector volume = new VolumeSpeechDetector(250, 10000, 800);
		RMSAudioInputStream wait = new RMSAudioInputStream(stream, stream.getFormat(), AudioSystem.NOT_SPECIFIED, volume);
		long startTime = System.currentTimeMillis();
		System.out.println("Calibrating");
		while (System.currentTimeMillis() - startTime < 1000)
			wait.read(new byte[1024]);
		System.out.println("Calibrated " + volume.average);
		volume.stopCalibrating();
		ToggleAudioInputStream toggle = new ToggleAudioInputStream(wait, stream.getFormat(), AudioSystem.NOT_SPECIFIED, true);
		toggle.setDeaf(true);
		volume.state.addListener((observable, oldVal, newVal) -> {
			System.out.println(newVal + " (" + oldVal + ")");
			if (newVal != State.QUIET && oldVal == State.QUIET)
				toggle.setDeaf(false);
			if (newVal == State.TIMEOUT || newVal == State.TOO_SHORT || (newVal == State.QUIET && oldVal != State.QUIET))
				try {
					System.out.println("close");
					toggle.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		});
		return new Pair<AudioInputStream, VolumeSpeechDetector>(toggle, volume);
	}

	public abstract void play(AudioInputStream stream) throws LineUnavailableException, IOException, InterruptedException;

	public void init() throws JackException {
	}

	public void close() throws JackException, IOException {
	}

	public AudioFormat getFormat() {
		return format;
	}

	public static class DefaultAudio extends Audio {

		public DefaultAudio(JsonObject config) {
			super(config);
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

	public static class JackAudio extends Audio implements JackProcessCallback, JackSampleRateCallback, JackBufferSizeCallback {

		protected JackClient						client;
		protected JackPort							out, in;
		protected int								sampleRate, bufferSize;
		protected LinkedList<AudioInputStream>		outQueue	= new LinkedList<>();
		protected Queue<CircularBufferInputStream>	inQueue		= new LinkedList<>();

		public JackAudio(JsonObject config) {
			super(config);
		}

		@Override
		public void init() throws JackException {
			Jack jack = Jack.getInstance();
			client = jack.openClient("VoicePi", EnumSet.noneOf(JackOptions.class), EnumSet.noneOf(JackStatus.class));
			in = client.registerPort("in", JackPortType.AUDIO, EnumSet.of(JackPortFlags.JackPortIsInput));
			out = client.registerPort("out", JackPortType.AUDIO, EnumSet.of(JackPortFlags.JackPortIsOutput));
			client.setSampleRateCallback(this);
			client.setProcessCallback(this);
			client.setBuffersizeCallback(this);
			client.activate();
			sampleRateChanged(client, client.getSampleRate());
			buffersizeChanged(client, client.getBufferSize());
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
			synchronized (inQueue) {
				for (CircularBufferInputStream in : inQueue) {
					in.close();
					in.notifyAll();
				}
				inQueue.clear();
			}
		}

		@Override
		public AudioInputStream normalListening() throws LineUnavailableException, IOException {
			// TODO reduce buffer size
			CircularBufferInputStream in = new CircularBufferInputStream(new CircularByteBuffer(bufferSize * 8));
			synchronized (inQueue) {
				inQueue.add(in);
			}
			AudioInputStream audio = new AudioInputStream(in, format, AudioSystem.NOT_SPECIFIED);
			audio = formatStream(audio);
			return audio;
		}

		@Override
		public void play(AudioInputStream stream) {
			stream = formatStream(stream, format);
			synchronized (outQueue) {
				outQueue.add(stream);
			}
			synchronized (stream) {
				try {
					stream.wait();
				} catch (InterruptedException e) {

				}
				System.out.println("Finished waiting " + stream);
			}
			synchronized (outQueue) {
				outQueue.remove(stream);
			}
		}

		private byte[]		buffer;
		private ByteBuffer	byteBuffer;
		private FloatBuffer	floatBuffer;

		@Override
		public boolean process(JackClient client, int samples) {
			// Process in
			synchronized (inQueue) {
				ByteBuffer bb = in.getBuffer();
				bb.get(buffer);
				inQueue.removeIf(out -> {
					CircularByteBuffer b = out.getBuffer();
					if (b == null)
						return true;
					b.put(buffer);
					return false;
				});
			}
			// Process out
			synchronized (outQueue) {
				if (!outQueue.isEmpty()) {
					FloatBuffer outData = out.getFloatBuffer();
					// TODO cache all buffers
					for (int j = 0; j < outQueue.size(); j++) {
						boolean first = j == 0;
						AudioInputStream stream = outQueue.get(j);
						try {
							int read = stream.read(buffer, 0, samples * 4);
							for (int i = 0; i < samples; i++) {
								boolean done = i >= read / 4;
								outData.put(i, (first ? 0 : outData.get(i)) + (done ? 0 : floatBuffer.get(i)));
							}
							if (read < samples * 4) {
								stream.close();
								synchronized (stream) {
									System.out.println("Notify " + stream);
									stream.notifyAll();
								}
							}
						} catch (IOException e) {
							// TODO exception handling
							e.printStackTrace();
						}
					}
				} else {
					// TODO only write this one time
					out.getFloatBuffer().put(new float[samples]);
				}
			}
			return true;
		}

		@Override
		public void sampleRateChanged(JackClient client, int sampleRate) {
			if (client == JackAudio.this.client) {
				format = new AudioFormat(Encoding.PCM_FLOAT, sampleRate, 32, 1, 4, sampleRate, false);
				this.sampleRate = sampleRate;
			}
		}

		@Override
		public void buffersizeChanged(JackClient client, int bufferSize) {
			if (client == JackAudio.this.client) {
				bufferSize *= 4;
				this.bufferSize = bufferSize;
				buffer = new byte[bufferSize];
				byteBuffer = ByteBuffer.wrap(buffer);
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
				floatBuffer = byteBuffer.asFloatBuffer();
			}
		}

	}

	public static AudioInputStream formatStream(AudioInputStream in) {
		return formatStream(in, FORMAT);
	}

	public static AudioInputStream formatStream(AudioInputStream in, AudioFormat target) {
		if (!in.getFormat().equals(target))
			in = AudioSystem.getAudioInputStream(target, in);
		return in;
	}

	public static byte[] readAllBytes(AudioInputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOUtils.copy(in, out);
		return out.toByteArray();
	}
}
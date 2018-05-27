package de.piegames.voicepi.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Queue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
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
import com.google.gson.JsonObject;
import de.piegames.voicepi.Settings;

/**
 * Implements the {@link Audio} class using the system's JackAudio interface (through JNAJack bindings). This will require Jack to be installed and a Jack
 * server to be running to work properly. When running, it will create a new Jack client with one input and one output. Use this if you want to hook VoicePi to
 * something else than just mic+speakers, like other applications.
 */
public class JackAudio extends Audio implements JackProcessCallback, JackSampleRateCallback, JackBufferSizeCallback {

	protected JackClient						client;
	protected AudioFormat						format;
	protected JackPort							out, in;
	protected int								sampleRate, bufferSize;
	protected LinkedList<AudioInputStream>		outQueue	= new LinkedList<>();
	protected Queue<CircularBufferInputStream>	inQueue		= new LinkedList<>();

	public JackAudio(JsonObject config) {
		super(config);
	}

	@Override
	public void init(Settings settings) throws IOException {
		try {
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
		} catch (JackException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void close() throws IOException {
		try {
			client.transportStop();
		} catch (JackException e) {
			throw new IOException(e);
		}
		client.deactivate();
		synchronized (outQueue) {
			for (AudioInputStream in : outQueue)
				in.close();
			outQueue.clear();
		}
		synchronized (inQueue) {
			for (CircularBufferInputStream in : inQueue) {
				in.close();
				synchronized (in) {
					in.notifyAll();
				}
			}
			inQueue.clear();
		}
	}

	@Override
	public AudioInputStream normalListening(AudioFormat targetEncoding) throws IOException {
		CircularBufferInputStream in = new CircularBufferInputStream(new CircularByteBuffer(bufferSize * 128));
		synchronized (inQueue) {
			inQueue.add(in);
		}
		AudioInputStream audio = new AudioInputStream(in, format, AudioSystem.NOT_SPECIFIED);
		audio = formatStream(audio, targetEncoding);
		return audio;
	}

	@Override
	public CircularBufferInputStream normalListening2() throws IOException {
		CircularBufferInputStream in = new CircularBufferInputStream(new CircularByteBuffer(getCommandBufferSize()));
		synchronized (inQueue) {
			inQueue.add(in);
		}
		return in;
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
						log.warn("Could not play audio", e);
						try {
							stream.close();
						} catch (IOException e1) {
							log.warn("Could not even close the stream that could not play audio", e);
						}
						synchronized (stream) {
							System.out.println("Notify " + stream);
							stream.notifyAll();
						}
					}
				}
			} else {
				// TODO only write this one time
				// TODO don't create a float[]
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
			log.debug("Setting new audio format: " + format + " Buffer size: " + getCommandBufferSize());
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

	@Override
	public AudioFormat getListeningFormat() {
		return format;
	}
}

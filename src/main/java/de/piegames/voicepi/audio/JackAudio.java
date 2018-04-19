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
import javax.sound.sampled.LineUnavailableException;
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
		CircularBufferInputStream in = new CircularBufferInputStream(new CircularByteBuffer(bufferSize * 256));
		synchronized (inQueue) {
			inQueue.add(in);
		}
		AudioInputStream audio = new AudioInputStream(in, format, AudioSystem.NOT_SPECIFIED);
		// audio = formatStream(audio);
		return audio;
	}

	@Override
	public CircularBufferInputStream normalListening2() throws LineUnavailableException, IOException {
		// TODO reduce buffer size
		CircularBufferInputStream in = new CircularBufferInputStream(new CircularByteBuffer(bufferSize * 256));
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
						// TODO exception handling
						e.printStackTrace();
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

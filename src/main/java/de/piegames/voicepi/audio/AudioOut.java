package de.piegames.voicepi.audio;

import java.io.IOException;
import java.util.EnumSet;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackOptions;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;
import org.jaudiolibs.jnajack.JackStatus;

public abstract class AudioOut {

	public AudioOut() {
	}

	public void init() throws JackException {
	}

	public void close() throws JackException {
	}

	public abstract void play(AudioInputStream stream) throws LineUnavailableException, IOException;

	public static class DefaultOut extends AudioOut {

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

	public static class JackOut extends AudioOut {

		protected JackClient	client;
		protected JackPort		out;

		public JackOut() {
		}

		@Override
		public void init() throws JackException {
			Jack jack = Jack.getInstance();
			client = jack.openClient("Test", EnumSet.noneOf(JackOptions.class), EnumSet.noneOf(JackStatus.class));
			int sampleRate = client.getSampleRate();
			out = client.registerPort("out", JackPortType.AUDIO, EnumSet.of(JackPortFlags.JackPortIsOutput));
			client.activate();
			client.transportStart();
		}

		@Override
		public void close() throws JackException {
			client.transportStop();
			client.deactivate();
		}

		@Override
		public void play(AudioInputStream stream) {
		}
	}
}

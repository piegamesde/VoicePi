package de.piegames.voicepi.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import org.jaudiolibs.jnajack.JackException;
import com.google.api.client.util.IOUtils;
import com.google.gson.JsonObject;
import de.piegames.voicepi.audio.VolumeSpeechDetector.State;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public abstract class Audio {

	/** The default format. It will be used for all audio processing and if possible for audio recording. */
	public static final AudioFormat FORMAT = new AudioFormat(16000, 16, 1, true, false);

	public Audio(JsonObject config) {// TODO configure
	}

	/**
	 * This will start listening until the returned {@code AudioInputStream} is closed
	 *
	 * @throws IOException
	 */
	public abstract AudioInputStream normalListening() throws LineUnavailableException, IOException;

	public abstract CircularBufferInputStream normalListening2() throws LineUnavailableException, IOException;

	/**
	 * This will wait until a command gets spoken, then return and automatically stop listening once the command is over
	 *
	 * @throws LineUnavailableException
	 * @throws IOException
	 */
	public byte[] listenCommand() throws IOException, LineUnavailableException {
		IntegerProperty startIndex = new SimpleIntegerProperty();
		IntegerProperty endIndex = new SimpleIntegerProperty();
		CircularBufferInputStream stream = normalListening2();
		CircularByteBuffer buffer = stream.getBuffer();
		AudioInputStream stream2 = formatStream(new AudioInputStream(stream, getListeningFormat(), AudioSystem.NOT_SPECIFIED));
		VolumeSpeechDetector volume = new VolumeSpeechDetector(250, 10000, 800);
		RMSInputStream wait = new RMSInputStream(stream2, getListeningFormat(), volume);

		{ // Calibrating. This will listen for n milliseconds and calculate the average volume from it. This will be used as threshold later on
			long startTime = System.currentTimeMillis();
			System.out.println("Calibrating");
			while (System.currentTimeMillis() - startTime < 1000)
				wait.read(new byte[1024]); // TODO replace this by read
			System.out.println("Calibrated " + volume.average);
			volume.stopCalibrating();
		}
		volume.state.addListener((observable, oldVal, newVal) -> {
			if (oldVal == State.QUIET && (newVal == State.SPEAKING)) {
				startIndex.set(buffer.getIndex());
			}
			if (newVal == State.TIMEOUT || newVal == State.TOO_SHORT || (newVal == State.QUIET && oldVal != State.QUIET)) {
				try {
					endIndex.set(buffer.getIndex());
					wait.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		while (wait.read(new byte[1024]) != -1)
			;
		if (volume.aborted())
			return null;
		int start = Math.floorMod(startIndex.get() - 10240, buffer.capacity());
		int len = Math.floorMod(endIndex.get() - start, buffer.capacity());
		byte[] ret = new byte[len];
		buffer.getRaw(start, ret, 0, len);
		return ret;
	}

	public abstract void play(AudioInputStream stream) throws LineUnavailableException, IOException, InterruptedException;

	public void init() throws JackException {
	}

	public void close() throws JackException, IOException {
	}

	public abstract AudioFormat getListeningFormat();

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
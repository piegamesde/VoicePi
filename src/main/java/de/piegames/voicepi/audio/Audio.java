package de.piegames.voicepi.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.api.client.util.IOUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.piegames.voicepi.audio.VolumeSpeechDetector.State;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public abstract class Audio {

	/** The default format. It will be used for all audio processing and if possible for audio recording. */
	public static final AudioFormat	FORMAT				= new AudioFormat(16000, 16, 1, true, false);

	protected final Log				log					= LogFactory.getLog(getClass());
	protected float					commandBufferSize, minCommandLength, maxCommandPauseTime, timeoutTime, calibrationTime;
	protected long					lastCalibrationTime	= 0;
	protected float					calibratedAverage	= 1;

	public Audio(JsonObject config) {
		commandBufferSize = Optional.ofNullable(config.getAsJsonPrimitive("command-buffer-size")).map(JsonPrimitive::getAsFloat).orElse(10f);
		minCommandLength = Optional.ofNullable(config.getAsJsonPrimitive("min-command-length")).map(JsonPrimitive::getAsFloat).orElse(0.25f);
		maxCommandPauseTime = Optional.ofNullable(config.getAsJsonPrimitive("max-command-pause-time")).map(JsonPrimitive::getAsFloat).orElse(0.8f);
		timeoutTime = Optional.ofNullable(config.getAsJsonPrimitive("command-timeout-time")).map(JsonPrimitive::getAsFloat).orElse(10f);
		calibrationTime = Optional.ofNullable(config.getAsJsonPrimitive("calibration-time")).map(JsonPrimitive::getAsFloat).orElse(1f);
		if (commandBufferSize < 1) {
			log.warn("Minimum command buffer size is 1s");
			commandBufferSize = 1;
		}
		// TODO don't calibrate before each command
	}

	/**
	 * Start listening until the returned {@code AudioInputStream} is closed. The resulting AudioInputStream will have the format specified by
	 * {@code targetFormat}
	 *
	 * @param targetFormat the target audio format for the resulting AudioInputStream.
	 * @throws IOException if something goes wrong
	 */
	public abstract AudioInputStream normalListening(AudioFormat targetFormat) throws IOException;

	/**
	 * Start listening until the returned {@link AudioInputStream} is closed. The resulting AudioInputStream will have the format specified by
	 * {@code #getListeningFormat()}. The returned stream will be backed by a {@link CircularByteBuffer}. If the buffer is full, it will overwrite the least
	 * recent audio data without blocking. If the buffer is empty, it will block and wait until some audio data is available. The buffer will be large enough to
	 * fit {@link #commandBufferSize} seconds of audio data.
	 *
	 * @throws IOException if something goes wrong
	 */
	public abstract CircularBufferInputStream normalListening2() throws IOException;

	/**
	 * This will wait until a command gets spoken, record all audio until the command is over, convert it to the target format and return.
	 *
	 * @param targetFormat the target audio format for the resulting AudioInputStream.
	 * @return An AudioInputStream backed by a byte buffer containing the audio data of the spoken command in the specified audio format or {@code null} if no
	 *         command was spoken.
	 * @throws IOException if something goes wrong
	 */
	@SuppressWarnings("resource")
	public AudioInputStream listenCommand(AudioFormat targetFormat) throws IOException {
		IntegerProperty startIndex = new SimpleIntegerProperty();
		IntegerProperty endIndex = new SimpleIntegerProperty();

		CircularBufferInputStream stream = normalListening2();
		CircularByteBuffer buffer = stream.getBuffer();
		RMSInputStream wait = new RMSInputStream(stream, getListeningFormat(), null);
		calibrate(wait);
		VolumeSpeechDetector volume = new VolumeSpeechDetector(calibratedAverage, (int) (minCommandLength * 1000f), (int) (timeoutTime * 1000f), (int) (maxCommandPauseTime * 1000f), (int) ((commandBufferSize - 0.5f) * 1000f));
		wait.callback.set(volume);

		volume.state.addListener((observable, oldVal, newVal) -> {
			if (oldVal == State.QUIET && (newVal == State.SPEAKING || newVal == State.STARTED_SPEAKING)) {
				startIndex.set(buffer.getIndex());
			}
			if (newVal == State.TIMEOUT || newVal == State.TOO_SHORT || (newVal == State.QUIET && oldVal != State.QUIET)) {
				try {
					System.out.println("Closing stream because of " + newVal);
					endIndex.set(buffer.getIndex());
					wait.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		while (wait.read(new byte[1024]) != -1)
			;
		System.out.println(volume.getState() + " " + volume.aborted());
		if (volume.aborted())
			return null;
		int start = Math.floorMod(startIndex.get() - secondsToBytes(getListeningFormat(), 0.5f), buffer.capacity());
		int len = Math.floorMod(endIndex.get() - start, buffer.capacity());
		byte[] data = new byte[len];
		buffer.getRaw(start,
				data, 0, len);
		AudioInputStream ret = new AudioInputStream(new ByteArrayInputStream(data), getListeningFormat(), AudioSystem.NOT_SPECIFIED);
		if (targetFormat != null)
			ret = formatStream(ret, targetFormat);
		return ret;
	}

	/** This will listen for n milliseconds and calculate the average volume from it. This will be used as threshold later on */
	protected void calibrate(RMSInputStream rmsIn) throws IOException {
		if (System.currentTimeMillis() - lastCalibrationTime < (commandBufferSize * 1000f))
			return;
		class AverageConsumer implements Consumer<Float> {

			float	sum		= 0f;
			int		count	= 0;

			@Override
			public void accept(Float t) {
				sum += t;
				count++;
			}
		}
		AverageConsumer c = new AverageConsumer();
		rmsIn.callback.set(c);
		long startTime = System.currentTimeMillis();
		System.out.println("Calibrating");
		while (System.currentTimeMillis() - startTime < calibrationTime * 1000)
			rmsIn.read(new byte[1024]);
		calibratedAverage = c.sum / c.count;
		System.out.println("Calibrated " + calibratedAverage);
		lastCalibrationTime = (long) (startTime + calibrationTime * 1000);
	}

	/**
	 * Play the audio data from {@code stream} until it does not contain data anymore. This might be because it returns EOS, throws an Exception while reading
	 * or reading a chunk does not return the chunk's size. If the current thread is interrupted, it will stop playing and return as soon as possible.
	 *
	 * @throws IOException if an error occurred while reading or playing the stream
	 */
	public abstract void play(AudioInputStream stream) throws IOException;

	/** Called to initialize all audio stuff required to operate */
	public void init() throws IOException {
	}

	/** Called to free all resources claimed by {@link #init()} */
	public void close() throws IOException {
	}

	/**
	 * This is the implementation's audio format used for recording any audio data. If requesting audio in a different format, it will probably be converted
	 * before returning. If you do request another audio format (say, with a different encoding), try at least to keep this one's sample rate to avoid
	 * resampling.
	 */
	public abstract AudioFormat getListeningFormat();

	protected int getCommandBufferSize() {
		return secondsToBytes(getListeningFormat(), commandBufferSize);
	}

	/** Calculates the amount of bytes needed to store n seconds of audio data in the given audio format */
	public static int secondsToBytes(AudioFormat format, float seconds) {
		return (int) (seconds * format.getFrameRate() * format.getFrameSize());
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
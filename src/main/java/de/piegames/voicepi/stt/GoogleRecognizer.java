package de.piegames.voicepi.stt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1p1beta1.RecognizeResponse;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import de.piegames.voicepi.VoicePi;
import edu.cmu.sphinx.api.SpeechResult;

public class GoogleRecognizer extends SphinxBaseRecognizer {

	public GoogleRecognizer(VoicePi control, JsonObject config) {
		super(control, config);
	}

	@Override
	public void load(BlockingQueue<Collection<String>> commandsSpoken, Set<String> commands) throws IOException {
		super.load(commandsSpoken, commands);
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			log.debug("Listening");
			try {
				AudioFormat format = new AudioFormat(8000, 16, 1, true, false);
				DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
				TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
				line.open(format);
				line.start(); // start capturing
				AudioInputStream stream = new AudioInputStream(line);
				new Thread(() -> {
					try {
						Thread.sleep(5000);
						stream.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}).start();
				
				// ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				AudioSystem.write(stream, AudioFileFormat.Type.WAVE, new File("test2.wav"));
				line.stop();
				line.close();
				// AudioSystem.write(stream, AudioFileFormat.Type.WAVE, outputStream);
				// byte[] fileData = outputStream.toByteArray();
				// Files.write(Paths.get("test.wav"), fileData);
				// syncRecognizeFile(fileData);
				syncRecognizeFile(Files.readAllBytes(Paths.get("test2.wav")));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		log.debug("Not listening anymore");
		
	}

	@Override
	public void startRecognition() {
		log.debug("Starting PocketSphinxRecognizer");
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void stopRecognition() {
		log.debug("Stopping PocketSphinxRecognizer");
		thread.interrupt();
		thread = null;
	}

	@Override
	public void deafenRecognition(boolean deaf) {
		super.deafenRecognition(deaf);
		if (!isRunning())
			return;
	}

	@Override
	public void unload() {

	}

	public void syncRecognizeFile(byte[] data) throws Exception, IOException {
		SpeechClient speech = SpeechClient.create();
		
		ByteString audioBytes = ByteString.copyFrom(data);

		// Configure request with local raw PCM audio
		RecognitionConfig config = RecognitionConfig.newBuilder()
				.setEncoding(AudioEncoding.LINEAR16)
				.setLanguageCode("de-DE")
				.setSampleRateHertz(8000)
				.build();
		RecognitionAudio audio = RecognitionAudio.newBuilder()
				.setContent(audioBytes)
				.build();

		// Use blocking call to get audio transcript
		RecognizeResponse response = speech.recognize(config, audio);
		List<SpeechRecognitionResult> results = response.getResultsList();
		ArrayList<String> strres = new ArrayList<String>();
		for (SpeechRecognitionResult result : results) {
			// There can be several alternative transcripts for a given chunk of speech. Just use the
			// first (most likely) one here.
			SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
			strres.add(alternative.getTranscript().toUpperCase());
		}
		this.commandsSpoken.offer(strres);
		speech.close();
	}
}
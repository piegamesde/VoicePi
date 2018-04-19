package de.piegames.voicepi.stt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1p1beta1.RecognizeResponse;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechContext;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import de.piegames.voicepi.VoicePi;

public class GoogleRecognizer extends SpeechRecognizer {

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
				System.out.println("Start-------------------------------------------");
				// Pair<AudioInputStream, VolumeSpeechDetector> result = control.getAudio().listenCommand();
				// AudioInputStream stream = result.getKey();
				// byte[] fileData = Audio.readAllBytes(stream);
				// // System.out.println("Done");
				// if (result.getValue().aborted()) {
				// System.out.println("NOPE!");
				// continue;
				// }
				byte[] fileData = control.getAudio().listenCommand();
				// System.out.println("Done");
				if (fileData == null) {
					System.out.println("NOPE!");
					continue;
				}
				control.getAudio().play(
						new AudioInputStream(new ByteArrayInputStream(fileData), control.getAudio().getListeningFormat(), AudioSystem.NOT_SPECIFIED));
				// System.out.println(AudioSystem.write(
				// new AudioInputStream(new ByteArrayInputStream(fileData), stream.getFormat(), AudioSystem.NOT_SPECIFIED), Type.WAVE, new File("test.wav")));
				// System.out.println(AudioSystem.write(control.getAudio().listenCommand(), Type.WAVE, new File("test.wav")));
				// Thread.sleep(1000);
				// System.out.println("Blubba");

				// List<String> strres = syncRecognizeData(fileData);
				// this.commandsSpoken.offer(strres);
			} catch (Exception e) {
				log.error("Could not analyze audio: ", e);
				// e.printStackTrace();
			}
		}
		log.debug("Not listening anymore");
	}

	@Override
	public void deafenRecognition(boolean deaf) {
		super.deafenRecognition(deaf);
		if (!isRunning())
			return;
	}

	@Override
	public boolean transcriptionSupported() {
		return true;
	}

	@Override
	public List<String> transcribe() {
		AudioInputStream ai = null;
		try {
			// TODO may be null
			byte[] b = control.getAudio().listenCommand();
			return syncRecognizeData(b);
			// TODO multi-catch?
		} catch (LineUnavailableException e) {
			log.error("Could not read from microphone input", e);
			// e.printStackTrace();
		} catch (IOException e) {
			log.error("Could not read from microphone input", e);
			// e.printStackTrace();
		} catch (Exception e) {
			log.error("Could not analyze microphone input", e);
		}
		return Collections.emptyList();
	}

	@Override
	public void unload() {
	}

	public List<String> syncRecognizeData(byte[] data) throws Exception, IOException {
		if (data == null)
			return Collections.emptyList();
		log.info("Processing audio data...");
		SpeechClient speech = SpeechClient.create(); // TODO reuse variable
		ByteString audioBytes = ByteString.copyFrom(data);

		// Configure request with local raw PCM audio
		RecognitionConfig config = RecognitionConfig.newBuilder()
				.setEncoding(AudioEncoding.LINEAR16)
				.setLanguageCode(control.getSettings().getLangCode())
				.setSampleRateHertz(16000)
				// TODO don't use this when transcribing
				.addSpeechContexts(SpeechContext.newBuilder().addAllPhrases(control.getStateMachine().getAvailableCommands()).build())
				.build();
		RecognitionAudio audio = RecognitionAudio.newBuilder()
				.setContent(audioBytes)
				.build();

		// Use blocking call to get audio transcript
		RecognizeResponse response = speech.recognize(config, audio);
		List<SpeechRecognitionResult> results = response.getResultsList();
		ArrayList<String> strres = new ArrayList<String>();
		// TODO this is wrong (but will work fine on short audio data)
		for (SpeechRecognitionResult result : results) {
			// There can be several alternative transcripts for a given chunk of speech. Just use the
			// first (most likely) one here.
			SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
			strres.add(alternative.getTranscript().toUpperCase());
		}
		speech.close();
		return strres;
	}
}
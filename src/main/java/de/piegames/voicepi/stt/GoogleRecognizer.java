package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig.Builder;
import com.google.cloud.speech.v1p1beta1.RecognizeResponse;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechContext;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import de.piegames.voicepi.audio.Audio;

public class GoogleRecognizer extends SpeechRecognizer {

	public GoogleRecognizer(JsonObject config) {
		super(config);
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			log.debug("Listening");
			try {
				System.out.println("Start-------------------------------------------");
				AudioInputStream in = audio.listenCommand(Audio.FORMAT);
				if (in == null) {
					System.out.println("NOPE!");
					continue;
				}
				byte[] fileData = Audio.readAllBytes(in);
				List<String> strres = syncRecognizeData(fileData, false);
				this.commandsSpoken.offer(strres);
			} catch (Exception e) {
				log.error("Could not analyze audio: ", e);
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
		try {
			// TODO may be null
			AudioInputStream in = audio.listenCommand(Audio.FORMAT);
			// System.out.println("Done");
			if (in == null) {
				System.out.println("NOPE!");
				return Collections.emptyList();
			}
			byte[] b = Audio.readAllBytes(in);
			return syncRecognizeData(b, true);
			// TODO multi-catch?
		} catch (IOException e) {
			log.error("Could not read from microphone input", e);
		} catch (Exception e) {
			log.error("Could not analyze microphone input", e);
		}
		return Collections.emptyList();
	}

	@Override
	public void unload() {
	}

	public List<String> syncRecognizeData(byte[] data, boolean transcribe) throws Exception, IOException {
		if (data == null)
			return Collections.emptyList();
		log.info("Processing audio data...");
		SpeechClient speech = SpeechClient.create(); // TODO reuse variable
		ByteString audioBytes = ByteString.copyFrom(data);

		// Configure request with local raw PCM audio
		Builder builder = RecognitionConfig.newBuilder()
				.setEncoding(AudioEncoding.LINEAR16)
				.setLanguageCode(settings.getLangCode())
				.setSampleRateHertz(16000);
		if (!transcribe)
			builder = builder
					.addSpeechContexts(SpeechContext.newBuilder().addAllPhrases(stateMachine.getAvailableCommands()).build());
		RecognitionConfig config = builder.build();
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
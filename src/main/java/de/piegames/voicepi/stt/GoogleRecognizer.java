package de.piegames.voicepi.stt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
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
import de.piegames.voicepi.audio.Audio;

public class GoogleRecognizer extends SpeechRecognizer {

	public GoogleRecognizer(VoicePi control, JsonObject config) {
		super(control, config);
	}

	@Override
	public void load(BlockingQueue<Collection<String>> commandsSpoken, Set<String> commands) throws IOException {
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			log.debug("Listening");
			try {
				byte[] fileData = Audio.readAllBytes(control.getAudio().listenCommand());
				Files.write(Paths.get("test.wav"), fileData);
				syncRecognizeFile(fileData);
			} catch (Exception e) {
				e.printStackTrace();
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
	public void unload() {

	}

	public void syncRecognizeFile(byte[] data) throws Exception, IOException {
		SpeechClient speech = SpeechClient.create(); // TODO reuse variable

		ByteString audioBytes = ByteString.copyFrom(data);

		// Configure request with local raw PCM audio
		RecognitionConfig config = RecognitionConfig.newBuilder()
				.setEncoding(AudioEncoding.LINEAR16)
				.setLanguageCode(control.getSettings().getLangCode())
				.setSampleRateHertz(16000)
				.build();
		RecognitionAudio audio = RecognitionAudio.newBuilder()
				.setContent(audioBytes)
				.build();
		// TODO add available commands as "context" so that Google will prefer them

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
		this.commandsSpoken.offer(strres);
		speech.close();
	}
}
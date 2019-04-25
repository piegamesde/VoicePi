package de.piegames.voicepi.stt;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Message;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Structure;
import com.google.gson.JsonObject;
import de.piegames.voicepi.Settings;
import de.piegames.voicepi.audio.Audio;
import de.piegames.voicepi.state.VoiceState;
import edu.cmu.sphinx.api.Configuration;

public class PocketSphinxRecognizer extends SphinxBaseRecognizer {

	protected Pipeline pipeline;

	public PocketSphinxRecognizer(JsonObject config) {
		super(config);
	}

	@Override
	public void load(Audio audio, VoiceState stateMachine, Settings settings, BlockingQueue<Collection<String>> commandsSpoken, Set<String> commands) throws IOException {
		super.load(audio, stateMachine, settings, commandsSpoken, commands);
		// Configure stt
		Configuration sphinxConfig = new Configuration();
		sphinxConfig.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		sphinxConfig.setDictionaryPath(dicPath.toAbsolutePath().toUri().toURL().toString());
		sphinxConfig.setLanguageModelPath(lmPath.toAbsolutePath().toUri().toURL().toString());

		log.debug("Dictionary path: " + dicPath.toAbsolutePath());
		log.debug("Language model path: " + lmPath.toAbsolutePath());

		Gst.init();
		// FIXME: this will fail after reloading for some reason
		pipeline = Pipeline.launch("autoaudiosrc ! audioconvert !  audioresample ! pocketsphinx name=asr ! fakesink");
		Element asr = pipeline.getElementByName("asr");
		System.out.println(Arrays.toString(asr.listPropertyNames().toArray()));
		asr.set("lm", lmPath.toAbsolutePath());
		asr.set("dict", dicPath.toAbsolutePath());
		Bus bus = pipeline.getBus();
		bus.connect(new Bus.MESSAGE() {

			@Override
			public void busMessage(Bus bus, Message message) {
				try {
					Structure s = message.getStructure();
					if (isRunning() && !deaf && s != null && "pocketsphinx".equals(s.getName()) && s.getValue("final").equals(true)) {
						String said = s.getValue("hypothesis").toString();
						if (!said.isEmpty()) {
							LinkedList<String> words = new LinkedList<>(Arrays.asList(said.split(" ")));
							LinkedList<String> spoken = new LinkedList<>();
							while (!words.isEmpty()) {
								spoken.add(String.join(" ", words));
								words.removeFirst();
							}
							commandSpoken(spoken);
						}
					}
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void run() {
	}

	@Override
	public void startRecognition() {
		super.startRecognition();
		pipeline.play();
	}

	@Override
	public void stopRecognition() {
		pipeline.pause();
		super.stopRecognition();
	}

	@Override
	public void deafenRecognition(boolean deaf) {
		super.deafenRecognition(deaf);
		if (!isRunning())
			return;
		if (deaf)
			pipeline.pause();
		else
			pipeline.play();
	}

	@Override
	public void unload() {
		super.unload();
		pipeline.stop();
		Gst.deinit();
	}

	@Override
	public boolean transcriptionSupported() {
		return false;
	}
}
package de.piegames.voicepi.stt;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import de.piegames.voicepi.state.ContextState;

public class MultiRecognizer extends SpeechRecognizer {

	protected List<SpeechRecognizer>	recognizers;
	protected boolean					onlyOne;

	public MultiRecognizer(VoicePi control, JsonObject config) {
		super(control, config);
		recognizers = new ArrayList<>();
		onlyOne = config.getAsJsonPrimitive("only-one-active").getAsBoolean();
		for (JsonElement e : config.getAsJsonArray("engines")) {
			JsonObject stt = e.getAsJsonObject();
			try {
				recognizers.add((SpeechRecognizer) Class.forName(stt.getAsJsonPrimitive("class-name").getAsString())
						.getConstructor(JsonObject.class)
						.newInstance(stt));
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e1) {
				log.warn("Could not instantiate speech recognizer " + stt.getAsJsonPrimitive("class-name").getAsString(), e1);
			}
		}
	}

	@Override
	public void load(BlockingQueue<Collection<String>> commandsSpoken, Set<String> commands) throws IOException {
		this.commandsSpoken = commandsSpoken;
		for (SpeechRecognizer r : recognizers)
			r.load(commandsSpoken, commands);
	}

	@Override
	public void run() {
	}

	@Override
	public void onStateChanged(ContextState current) {
		for (SpeechRecognizer r : recognizers) {
			r.onStateChanged(current);
			if (r.isStateEnabled() && !r.isRunning())
				r.startRecognition();
			if (!r.isStateEnabled() && r.isRunning())
				r.stopRecognition();
		}
	}

	@Override
	public void startRecognition() {
		for (SpeechRecognizer r : recognizers)
			if (r.isStateEnabled())
				r.startRecognition();
	}

	@Override
	public void stopRecognition() {
		for (SpeechRecognizer r : recognizers)
			r.stopRecognition();
	}

	@Override
	public void deafenRecognition(boolean deaf) {
		for (SpeechRecognizer r : recognizers)
			r.deafenRecognition(deaf);
	}

	@Override
	public void unload() {
		for (SpeechRecognizer r : recognizers)
			r.unload();
	}
}

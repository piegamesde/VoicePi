package de.piegames.voicepi.stt;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.piegames.voicepi.VoicePi;
import de.piegames.voicepi.state.ContextState;

public class MultiRecognizer extends SpeechRecognizer {

	protected List<SpeechRecognizer>	            recognizers;
	protected boolean					            onlyOne;
    protected Map<SpeechRecognizer, List<String>>   activate;

	public MultiRecognizer(VoicePi control, JsonObject config) {
		super(control, config);
		recognizers = new ArrayList<>();
        activate	= new HashMap<>();

		onlyOne = config.getAsJsonPrimitive("only-one-active").getAsBoolean();
		for (JsonElement e : config.getAsJsonArray("engines")) {
			JsonObject stt = e.getAsJsonObject();
			try {
			    SpeechRecognizer sr = (SpeechRecognizer) Class.forName(stt.getAsJsonPrimitive("class-name").getAsString())
                        .getConstructor(VoicePi.class, JsonObject.class)
                        .newInstance(control, stt);
				recognizers.add(sr);

                if ( stt.isJsonNull() || !stt.has("active-on")) {

                } else if (stt.get("active-on").isJsonPrimitive()) {
                    activate.put(sr, Collections.singletonList(stt.getAsJsonPrimitive("active-on").getAsString()));
                } else {
                    List<String> ls = new ArrayList<>();
                    for (JsonElement f : stt.getAsJsonArray("active-on"))
                        ls.add(f.getAsString());
                    activate.put(sr, ls);
                }
                if (activate.isEmpty())
                    activate.put(sr, Collections.singletonList("*:*"));
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
	public void startRecognition() {
		for (SpeechRecognizer r : recognizers)
            r.startRecognition(); // TODO implement only one active
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
    public boolean transcriptionSupported() {
        return false;
    }

    @Override
	public void unload() {
		for (SpeechRecognizer r : recognizers)
			r.unload();
	}
}

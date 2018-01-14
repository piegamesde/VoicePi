package de.piegames.picontrol;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.piegames.picontrol.state.VoiceState4;
import de.piegames.picontrol.tts.SpeechEngine;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;

public class PiControl {

	protected VoiceState4			stateMachine;
	protected SpeechEngine			tts;
	protected LiveSpeechRecognizer	stt;
	protected Map<String, Module> modules = new HashMap<>();
	
	public PiControl() {
		reload();
	}

	public void reload() {
		modules.values().forEach(Module::close);
		modules.clear();
		
		JsonObject config = new JsonParser().parse(Files.newBufferedReader(Paths.get("config.json"))).getAsJsonObject();
		tts = (SpeechEngine) Class.forName(config.getAsJsonPrimitive("speech-synth").getAsString()).newInstance();

		//config.getAsJsonArray("activation-commands")
		stateMachine = new VoiceState4();
		
		JsonObject modules = config.getAsJsonObject("modules");
		for (JsonElement element : config.getAsJsonArray("active-modules")) {
			Module module = (Module) Class.forName(
					modules.getAsJsonObject(element.getAsString()).getAsJsonPrimitive("class-name").getAsString()).newInstance();
			stateMachine.addModuleGraph(module.listCommands(stateMachine.getRoot(), stateMachine.getEnd()));
			this.modules.put(element.getAsString(), module);
		}
	}
	
	public void commandSpoken(String command) {
		
	}
	
	public void startRecognizer() {
		stt.startRecognition(true);
	}
	
	public void stopRecognizer() {
		stt.stopRecognition();
	}
	
	public void exitApplication() {
		stt.stopRecognition();
		System.exit(0);
	}

	public static void main(String... args) {
		new PiControl();
	}
}
package de.piegames.picontrol;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.piegames.picontrol.module.Module;
import de.piegames.picontrol.stt.SpeechRecognizer;
import de.piegames.picontrol.stt.SphinxRecognizer;
import de.piegames.picontrol.tts.SpeechEngine;

public class Configuration {

	protected final Log				log	= LogFactory.getLog(getClass());

	protected final Path			path;
	protected JsonObject			config, modulesConfig, sttConfig, ttsConfig;
	protected SpeechEngine			customTTS;
	protected SpeechRecognizer		customSTT;
	protected Map<String, Module>	customModules;

	public Configuration() {
		this(null);
	}

	public Configuration(Path path) {
		if (path == null)
			path = getDefaultPath();
		this.path = path;
	}

	public Path getDefaultPath() {
		return Paths.get("config.json");
	}

	public void loadDefaultConfig() {
		log.info("Loading default configuration");
		config = new JsonParser().parse(new InputStreamReader(getClass().getResourceAsStream("config.json"))).getAsJsonObject();
		modulesConfig = config.getAsJsonObject("modules");
		sttConfig = config.getAsJsonObject("stt");
		ttsConfig = config.getAsJsonObject("stt");
	}

	public void loadConfig() throws IOException {
		// Load config
		log.info("Loading configuration from file " + path.toAbsolutePath());
		config = new JsonParser().parse(Files.newBufferedReader(Paths.get("config.json").toAbsolutePath())).getAsJsonObject();
		modulesConfig = config.getAsJsonObject("modules");
		sttConfig = config.getAsJsonObject("stt");
		ttsConfig = config.getAsJsonObject("stt");
	}

	public JsonObject getModuleConfig(String moduleName) {
		return modulesConfig.get(moduleName).getAsJsonObject();
	}

	public JsonObject getSTTConfig(String sttName) {
		return sttConfig.getAsJsonObject(sttName).getAsJsonObject();
	}

	public JsonObject getTTSConfig(String ttsName) {
		return ttsConfig.getAsJsonObject(ttsName).getAsJsonObject();
	}

	public JsonObject getConfig() {
		return config;
	}

	public void saveConfig() throws IOException {
		log.info("Saving configuration to " + path.toAbsolutePath());
		Files.write(path, new GsonBuilder().setPrettyPrinting().create().toJson(config).getBytes());
	}

	public Map<String, Module> loadModulesFromConfig(PiControl control) {
		// Load module
		Map<String, Module> ret = new HashMap<>();
		for (JsonElement element : config.getAsJsonArray("active-modules")) {
			try {
				String moduleName = element.getAsString();
				JsonObject moduleConfig = getModuleConfig(moduleName);
				Module module = (Module) Class.forName(moduleConfig
						.getAsJsonPrimitive("class-name").getAsString())
						.getConstructor(PiControl.class, String.class, JsonObject.class)
						.newInstance(control, moduleName, moduleConfig);
				ret.put(moduleName, module);
			} catch (Throwable e) {
				log.warn("Could not instantiate module " + element.getAsString(), e);
			}
		}
		return ret;
	}

	public SpeechRecognizer loadSTTFromConfig() {
		return new SphinxRecognizer(config);// TODO generalize
	}

	public SpeechEngine loadTTSFromConfig(PiControl control) {
		try {
			return (SpeechEngine) Class.forName(config.getAsJsonPrimitive("speech-synth").getAsString())
					.getConstructor(PiControl.class)
					.newInstance(control);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			log.warn("Could not instantiate speech synthesizer as specified in the config file", e);
			return null;
		}
	}

	public void setTTS(SpeechEngine tts) {
		this.customTTS = tts;
	}

	public void setSTT(SpeechRecognizer stt) {
		this.customSTT = stt;
	}

	public void setModules(Map<String, Module> modules) {
		this.customModules = modules;
	}

	public SpeechEngine getTTS() {
		return customTTS;
	}

	public SpeechRecognizer getSTT() {
		return customSTT;
	}

	public Map<String, Module> getModules() {
		return customModules;
	}
}
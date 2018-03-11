package de.piegames.voicepi;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import de.piegames.voicepi.action.Action;
import de.piegames.voicepi.action.Action.ActionType;
import de.piegames.voicepi.action.PlaySoundAction;
import de.piegames.voicepi.action.RunCommandAction;
import de.piegames.voicepi.action.SayTextAction;
import de.piegames.voicepi.module.Module;
import de.piegames.voicepi.state.ContextState;
import de.piegames.voicepi.state.VoiceState;
import de.piegames.voicepi.stt.DeafRecognizer;
import de.piegames.voicepi.stt.SpeechRecognizer;
import de.piegames.voicepi.tts.MutedSpeechEngine;
import de.piegames.voicepi.tts.SpeechEngine;
import io.gsonfire.GsonFireBuilder;
import io.gsonfire.TypeSelector;

public class VoicePi {

	protected static final Log		log			= LogFactory.getLog(VoicePi.class);

	protected boolean				listening	= false;
	protected boolean				exit;
	protected VoiceState<Module>	stateMachine;
	protected SpeechEngine			tts;
	protected SpeechRecognizer		stt;
	protected Map<String, Module>	modules		= new HashMap<>();
	protected Settings				settings	= new Settings();

	protected Configuration			config;

	public VoicePi(Configuration config) {
		this.config = Objects.requireNonNull(config);
		reload();
	}

	public void run() {
		settings.onStart.execute(this, log, "onStart");
		while (!exit) {
			try {
				Collection<String> spoken = stt.commandsSpoken.poll((settings.timeout > 0) ? settings.timeout : Integer.MAX_VALUE, TimeUnit.SECONDS);
				if (spoken != null)
					onCommandSpoken(spoken);
				else {
					log.info("Timed out");
					settings.onTimeout.execute(this, log, "onTimeout");
					stateMachine.resetState();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.fatal("Exception while listening to speech input", e);
				break;
			}
			if (Thread.interrupted()) {
				log.info("An interrupt message was received, stopping the application");
				exitApplication();
				break;
			}
		}
		settings.onExit.execute(this, log, "onExit");
	}

	public void onCommandSpoken(String command) {
		onCommandSpoken(Arrays.asList(command));
	}

	public void onCommandSpoken(Collection<String> possibleCommand) {
		possibleCommand.stream().forEach(System.out::println);
		Module responsible = null;
		ContextState<Module> state = null;
		String command = null;
		for (String s : possibleCommand) {
			if (s.startsWith("<s>"))
				s = s.substring(3);
			if (s.endsWith("</s>"))
				s = s.substring(0, s.length() - 4);
			s = s.trim();

			if (stateMachine.commandSpoken(s) != null) {
				state = stateMachine.getCurrentState();
				responsible = state.owner;
				command = s;
				break;
			}
		}
		if (state == stateMachine.getRoot()) {
			log.info("Activated.");
			settings.onActivation.execute(this, log, "onActivation");
			return;
		}
		if (responsible != null) {
			stt.pauseRecognition();
			responsible.onCommandSpoken(stateMachine.getCurrentState(), command);
			stt.resumeRecognition();
		} else {
			log.info("What you just said makes no sense, sorry");
			log.debug("Current state: " + stateMachine.getCurrentState());
			log.debug("Available commands: " + stateMachine.getAvailableCommands());
			settings.onWrongCommand.execute(this, log, "onWrongCommand");
		}
	}

	public void reload() {
		settings.onReload.execute(this, log, "onReload");

		// Close modules
		if (stt != null) {
			stt.stopRecognition();
			stt.unload();
		}
		log.info("Reloading all modules");
		modules.values().forEach(Module::close);
		modules.clear();

		{ // Load Settings
			// TODO use Optional
			settings = config.getSettings();
			if (settings == null)
				settings = config.loadSettingsFromConfig();
			if (settings == null)
				settings = new Settings();
		}

		// Initialize state machine
		stateMachine = new VoiceState<>();
		stateMachine.setActivationCommands(settings.activationCommands);
		// stateMachine.setActivationCommands(
		// StreamSupport.stream(config.getConfig().getAsJsonArray("activation-commands").spliterator(), false)
		// .map(m -> m.getAsString())
		// .collect(Collectors.toSet()));

		modules.putAll(Optional.ofNullable(config.getModules()).orElse(config.loadModulesFromConfig(this)));
		modules.forEach((name, module) -> {
			stateMachine.addModuleGraph(module.listCommands(stateMachine.getRoot()));
			this.modules.put(name, module);
		});

		// Get all commands
		Set<String> commands = stateMachine.getAllCommands();
		if (commands.isEmpty())
			log.error("No commands registered. This application won't work properly without commands");
		log.debug("All registered commands:\n" + String.join(System.getProperty("line.separator"), commands));
		commands.remove(null);
		commands.remove("");

		{ // Load STT
			// TODO use Optional
			stt = config.getSTT();
			if (stt == null)
				stt = config.loadSTTFromConfig();

			try {
				if (stt != null)
					stt.load(commands);
			} catch (IOException e) {
				log.error("Could not load the speech recognition module; switching to DeafRecognizer", e);
				stt = null;
			}

			if (stt == null)
				stt = new DeafRecognizer(null);
			stt.startRecognition();
		}
		{ // Load TTS
			// TODO use Optional
			tts = config.getTTS();
			if (tts == null)
				tts = config.loadTTSFromConfig(this);
			if (tts == null)
				tts = new MutedSpeechEngine(this, null);
		}
	}

	public void exitApplication() {
		if (exit) // Application already stopped
			return;
		exit = true;
		log.info("Stopping speech recognition");
		stt.stopRecognition();
		stt.unload();
		modules.values().forEach(Module::close);
		log.info("Quitting application");
		System.exit(0);
	}

	public SpeechEngine getTTS() {
		return tts;
	}

	public SpeechRecognizer getSTT() {
		return stt;
	}

	public Settings getSettings() {
		return settings;
	}

	public static void main(String... args) {
		// TODO add CLI
		Configuration config = new Configuration();
		try {
			config.loadConfig();
		} catch (NoSuchFileException e) {
			log.error("Could not find config file at default path; loading default");
			config.loadDefaultConfig();
			try {
				config.saveConfig();
			} catch (IOException e1) {
			}
		} catch (JsonParseException e) {
			log.error("Your config file is corrupt, please fix it or delete it", e);
			return;
		} catch (IOException e) {
			log.error("Could not load config file; loading default", e);
			config.loadDefaultConfig();
		}
		VoicePi control = new VoicePi(config);
		control.run();
	}

	public static final Gson GSON = new GsonFireBuilder()
			.registerTypeSelector(Action.class, new TypeSelector<Action>() {

				@Override
				public Class<? extends Action> getClassForElement(JsonElement readElement) {
					switch (ActionType.forName(readElement.getAsJsonObject().getAsJsonPrimitive("action").getAsString())) {
						case RUN_COMMAND:
							return RunCommandAction.class;
						case SAY_TEXT:
							return SayTextAction.class;
						case PLAY_SOUND:
							return PlaySoundAction.class;
						case NONE:
						default:
							return Action.DoNothingAction.class;
					}
				}
			})
			.createGsonBuilder()
			.setPrettyPrinting()
			.setLenient()
			.create();
}
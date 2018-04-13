package de.piegames.voicepi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaudiolibs.jnajack.JackException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.piegames.voicepi.action.Action;
import de.piegames.voicepi.action.Action.ActionType;
import de.piegames.voicepi.audio.Audio;
import de.piegames.voicepi.module.Module;
import de.piegames.voicepi.state.CommandSet;
import de.piegames.voicepi.state.ContextState;
import de.piegames.voicepi.state.VoiceState;
import de.piegames.voicepi.stt.DeafRecognizer;
import de.piegames.voicepi.stt.SpeechRecognizer;
import de.piegames.voicepi.tts.MutedSpeechEngine;
import de.piegames.voicepi.tts.SpeechEngine;
import io.gsonfire.GsonFireBuilder;
import io.gsonfire.TypeSelector;

public class VoicePi implements Runnable {

	protected static final Log					log				= LogFactory.getLog(VoicePi.class);

	protected boolean							listening		= false;
	protected boolean							exit;
	protected VoiceState						stateMachine;
	protected SpeechEngine						tts;
	protected BlockingQueue<Collection<String>>	commandsSpoken;
	protected SpeechRecognizer					stt;
	protected Audio								audio;
	protected Map<String, Module>				modules			= new HashMap<>();
	protected Settings							settings		= new Settings();
	protected final Queue<ContextState>			notifications	= new SynchronousQueue<>();

	protected Configuration						config;

	public VoicePi(Configuration config) {
		this.config = Objects.requireNonNull(config);
	}

	@Override
	public void run() {
		// TODO add "listening-state" of the whole application so that the tests can appropriately wait for actions to finish
		// TODO make only run once
		settings.onStart.execute(this, log, "onStart");
		log.debug("Current state: " + stateMachine.getCurrentState());
		log.debug("Available commands: " + stateMachine.getAvailableCommands());
		while (!exit) {
			try {
				if (!notifications.isEmpty() && stateMachine.isIdle()) {
					Thread.sleep(1000);
					ContextState state = notifications.remove();
					log.info("Push notification incoming: " + state);
					stateMachine.current.set(state);
				}
				Collection<String> spoken = commandsSpoken.poll((settings.timeout > 0) ? settings.timeout : Integer.MAX_VALUE, TimeUnit.SECONDS);
				if (spoken != null) {
					onCommandSpoken(spoken);
					log.debug("Current state: " + stateMachine.getCurrentState());
					log.debug("Available commands: " + stateMachine.getAvailableCommands());
				} else if (!stateMachine.isWaitingForActivation() && stateMachine.isActivationNeeded()) {
					log.info("Timed out");
					stt.deafenRecognition(true);
					settings.onTimeout.execute(this, log, "onTimeout");
					stateMachine.resetState();
					stt.deafenRecognition(false);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.fatal("Exception while listening to speech input", e);
				break;
			}
			if (Thread.interrupted()) {
				log.info("An interrupt message was received, stopping the application");
				break;
			}
		}
		log.debug("Quit main loop");
		exitApplication();
	}

	public void onCommandSpoken(String command) {
		onCommandSpoken(Arrays.asList(command));
	}

	public void onCommandSpoken(Collection<String> possibleCommand) {
		log.debug("You might have said: " + Arrays.toString(possibleCommand.toArray()));
		Module responsible = null;
		boolean initialActivation = stateMachine.isWaitingForActivation();
		ContextState initialState = stateMachine.getCurrentState();
		String command = null;
		for (String s : possibleCommand) {
			if (s.startsWith("<s>"))
				s = s.substring(3);
			if (s.endsWith("</s>"))
				s = s.substring(0, s.length() - 4);
			s = s.trim();

			CommandSet edge = stateMachine.commandSpoken(s);
			if (edge != null) {
				responsible = edge.owner;
				command = s;
				break;
			}
		}
		ContextState state = stateMachine.getCurrentState();
		if (initialActivation && state == stateMachine.getRoot()) {
			log.info("Activated.");
			stt.deafenRecognition(true);
			settings.onActivation.execute(this, log, "onActivation");
			stt.deafenRecognition(false);
			return;
		}
		if (responsible != null) {
			stt.deafenRecognition(true);
			settings.onCommandSpoken.execute(this, log, "onCommandSpoken");
			// initialState: The state before this command was spoken and thus the state this command belongs to
			responsible.onCommandSpoken(initialState, command);
			stt.deafenRecognition(false);
		} else if (stateMachine.isActivationNeeded() && initialState == stateMachine.getStart()) {
			log.info("You need to activate first");
		} else {
			log.info("What you just said makes no sense, sorry");
			stt.deafenRecognition(true);
			settings.onWrongCommand.execute(this, log, "onWrongCommand");
			stt.deafenRecognition(false);
		}
	}

	public void reload() {
		log.info("Reloading the configuration");
		settings.onReload.execute(this, log, "onReload");

		notifications.clear();
		commandsSpoken = new LinkedBlockingQueue<>();
		// Close modules
		if (stt != null) {
			stt.stopRecognition();
			stt.unload();
			stt = null;
		}
		tts = null;
		if (audio != null)
			try {
				audio.close();
			} catch (JackException | IOException e2) {
				log.warn("Could not close audio", e2);
			}
		modules.values().forEach(Module::close);
		modules.clear();

		// Load config
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

		{ // Load Settings
			// TODO use Optional
			settings = config.getSettings();
			if (settings == null)
				settings = config.loadSettingsFromConfig();
			if (settings == null)
				settings = new Settings();
			log.debug("Loaded settings: " + settings);
		}

		// Initialize state machine
		stateMachine = new VoiceState();
		stateMachine.setActivationCommands(settings.activationCommands);

		modules.putAll(Optional.ofNullable(config.getModules()).orElse(config.loadModulesFromConfig(this)));
		modules.forEach((name, module) -> {
			stateMachine.addModuleGraph(module.listCommands(stateMachine.getRoot()));
			this.modules.put(name, module);
		});

		{// Load audio
			audio = config.getAudio();
			if (audio == null)
				audio = config.loadAudioFromConfig();
			if (audio == null) {
				log.fatal("Cannot load audio from configuration. This is required to run VoicePi!");
				exitApplication();
				throw new InternalError("Could not load audio");
			}
		}

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
				stt = config.loadSTTFromConfig(this);

			try {
				if (stt != null)
					stt.load(commandsSpoken, commands);
			} catch (IOException e) {
				log.error("Could not load the speech recognition module; switching to DeafRecognizer", e);
				stt = null;
			}

			if (stt == null)
				stt = new DeafRecognizer(this);
			stt.startRecognition();
		}
		{ // Load TTS
			// TODO use Optional
			tts = config.getTTS();
			if (tts == null)
				tts = config.loadTTSFromConfig(this);
			if (tts == null) {
				tts = new MutedSpeechEngine(this, null);
				log.error("Could not load the speech synthesis module; switching to MutedRecognizer");
			}
		}
	}

	public void exitApplication() {
		if (exit) // Application already stopped
			return;
		exit = true;

		log.info("Stopping speech recognition");
		if (stt != null) {
			stt.stopRecognition();
			stt.unload();
		}
		modules.values().forEach(Module::close);
		settings.onExit.execute(this, log, "onExit");
		log.info("Quitting application");
	}

	public SpeechEngine getTTS() {
		return tts;
	}

	public SpeechRecognizer getSTT() {
		return stt;
	}

	public Audio getAudio() {
		return audio;
	}

	public ContextState getCurrentState() {
		return stateMachine.getCurrentState();
	}

	public VoiceState getStateMachine() {
		return stateMachine;
	}

	public Settings getSettings() {
		return settings;
	}

	public void pushNotification(ContextState state) {
		notifications.add(state);
	}

	public static void main(String... args) {
		Configuration config = new Configuration();
		VoicePi control = new VoicePi(config);
		control.reload();
		control.run();
	}

	public static final Gson GSON = new GsonFireBuilder()
			.registerTypeSelector(Action.class, new TypeSelector<Action>() {

				@Override
				public Class<? extends Action> getClassForElement(JsonElement readElement) {
					return ActionType.forName(readElement.getAsJsonObject().getAsJsonPrimitive("action").getAsString()).getActionClass();
				}
			})
			.createGsonBuilder()
			.registerTypeHierarchyAdapter(Action.class, new TypeAdapter<Action>() {

				@Override
				public void write(JsonWriter out, Action value) throws IOException {
					throw new IOException("Not implemented, not needed");
				}

				@Override
				public Action read(JsonReader in) throws IOException {
					try {
						return Action.fromJson(com.google.gson.internal.Streams.parse(in).getAsJsonObject());
					} catch (NullPointerException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | JsonParseException e) {
						throw new JsonParseException("Could not instantiate Action", e);
					}
				}
			})
			.setPrettyPrinting()
			.setLenient()
			.create();
}
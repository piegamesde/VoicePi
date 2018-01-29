package de.piegames.picontrol;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.piegames.picontrol.module.Module;
import de.piegames.picontrol.state.VoiceState;
import de.piegames.picontrol.stt.DeafRecognizer;
import de.piegames.picontrol.stt.SpeechRecognizer;
import de.piegames.picontrol.tts.MutedSpeechEngine;
import de.piegames.picontrol.tts.SpeechEngine;

public class PiControl {

	protected final Log				log			= LogFactory.getLog(getClass());

	protected boolean				listening	= false;
	protected boolean				exit;
	protected VoiceState<Module>	stateMachine;
	protected SpeechEngine			tts;
	protected SpeechRecognizer		stt;
	protected Map<String, Module>	modules		= new HashMap<>();

	protected Configuration			config;

	public PiControl(Configuration config) {
		this.config = Objects.requireNonNull(config);
		reload();
	}

	public void run() {
		resume();
		while (!exit) {
			try {
				Collection<String> spoken = stt.nextCommand();
				if (spoken != null)
					onCommandSpoken(spoken);
				else {
					log.info("Nothing spoken. Did you disable your microphone? Waiting for 5s");
					Thread.sleep(5000);// TODO sleep for less long, but only log the first time to not spam the console.
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
	}

	public void onCommandSpoken(String command) {
		onCommandSpoken(Arrays.asList(command));
	}

	public void onCommandSpoken(Collection<String> possibleCommand) {
		possibleCommand.stream().forEach(System.out::println);
		Module responsible = null;
		String command = null;
		for (String s : possibleCommand) {
			if (s.startsWith("<s>"))
				s = s.substring(3);
			if (s.endsWith("</s>"))
				s = s.substring(0, s.length() - 4);
			s = s.trim();
			if ((responsible = stateMachine.commandSpoken(s)) != null) {
				command = s;
				break;
			}
		}
		if (responsible != null)
			responsible.onCommandSpoken(stateMachine.getCurrentState(), command);
		else
			log.info("What you just said makes no sense, sorry");
	}

	public void reload() {
		// Close modules
		if (stt != null)
			stt.stopRecognition();
		log.info("Reloading all modules");
		modules.values().forEach(Module::close);
		modules.clear();

		// Initialize state machine
		// config.getAsJsonArray("activation-commands") // TODO activation commands
		stateMachine = new VoiceState<>();

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
				stt = null; // TODO
			}

			if (stt == null)
				stt = new DeafRecognizer(null);
		}
		{ // Load TTS
			// TODO use Optional
			tts = config.getTTS();
			if (tts == null)
				tts = config.loadTTSFromConfig(this);
			if (tts == null)
				tts = new MutedSpeechEngine(this);
		}
	}

	public void pause() {
		stt.pauseRecognition();
	}

	public void resume() {
		stt.resumeRecognition();
	}

	public void exitApplication() {
		if (exit) // Application already stopped
			return;
		exit = true;
		log.info("Stopping speech recognition");
		stt.stopRecognition();
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

	public static void main(String... args) {
		Configuration config = new Configuration();
		try {
			config.loadConfig(Paths.get("config.json"));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		// config.setSTT(new DeafRecognizer(null));
		PiControl control = new PiControl(config);
		control.run();
		// control.resume();
		//
		// control.onCommandSpoken("TEST");
		// control.onCommandSpoken("WHERE ARE YOU");
		// control.onCommandSpoken("YOU IP");
	}
}
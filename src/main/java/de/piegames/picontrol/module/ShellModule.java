package de.piegames.picontrol.module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import de.piegames.picontrol.PiControl;
import de.piegames.picontrol.state.ContextState;

public class ShellModule extends Module {

	public static enum Mode {
		NORMAL, BACKGROUND, READ_ALOUD;
	}

	protected Map<String, String>	commands	= new HashMap<>();
	protected Mode					mode;

	public ShellModule(PiControl control, String name, Path base) throws RuntimeException {
		super(control, name, base);
		config.getAsJsonObject("commands").entrySet().stream()
				.forEach(e -> commands.put(e.getKey(), e.getValue().getAsString()));
		String mode = config.getAsJsonPrimitive("mode").getAsString();
		switch (mode) {
			case "read-aloud":
				this.mode = Mode.READ_ALOUD;
				break;
			case "normal":
			default:
				this.mode = Mode.NORMAL;
		}
	}

	@Override
	public MutableValueGraph<ContextState<Module>, Set<String>> listCommands(ContextState<Module> root) {
		MutableValueGraph<ContextState<Module>, Set<String>> ret = ValueGraphBuilder.directed().build();
		ContextState<Module> node = new ContextState<>(this, "end");
		ret.putEdgeValue(root, node, Collections.unmodifiableSet(commands.keySet()));
		return ret;
	}

	@Override
	public void commandSpoken(ContextState<Module> currentState, String command) {
		try {
			Process process = Runtime.getRuntime().exec(commands.get(command));
			if (mode == Mode.NORMAL)
				process.waitFor();
			if (mode == Mode.READ_ALOUD) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						control.getTTS().speakAndWait(line);
					}
				}
			}
		} catch (IOException e) {
			log.warn("Could not execute command '" + command + "'", e);
		} catch (NullPointerException e) {
			log.warn("Command " + command + " is not registered");
		} catch (InterruptedException e) {
			log.warn("Could not wait for process to finish, it will continue running in background");
		}
	}
}
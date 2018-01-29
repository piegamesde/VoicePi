package de.piegames.picontrol.module;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.common.graph.MutableValueGraph;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import de.piegames.picontrol.PiControl;
import de.piegames.picontrol.state.ContextState;

public abstract class Module {

	protected final Log			log		= LogFactory.getLog(getClass());
	protected final PiControl	control;
	protected final String		name;
	protected Path				basePath;
	protected JsonObject		config	= new JsonObject();

	public Module(PiControl control, String name, Path base) throws RuntimeException {
		this.control = Objects.requireNonNull(control);
		this.name = Objects.requireNonNull(name);
		this.basePath = base.resolve(name);
		try {
			config = new JsonParser().parse(Files.newBufferedReader(basePath.resolve("module-config.json"))).getAsJsonObject();
		} catch (NoSuchFileException e) {
			log.warn("File " + basePath.resolve("module-config.json").toAbsolutePath() + " not found, please create it");
			throw new ExceptionInInitializerError(e);
		} catch (JsonParseException e) {
			log.warn("module-config.json is ill-formed");
			throw new ExceptionInInitializerError(e);
		} catch (IOException e) {
			log.warn("Could not read module-config.json");
			throw new ExceptionInInitializerError(e);
		}
	}

	public abstract MutableValueGraph<ContextState<Module>, Set<String>> listCommands(ContextState<Module> root);

	public abstract void onCommandSpoken(ContextState<Module> currentState, String command);

	public void close() {
	}
}
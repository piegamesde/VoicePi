package de.piegames.picontrol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class CommandsCache {

	protected final Log				log		= LogFactory.getLog(getClass());

	protected Path					path;
	protected List<CacheElement>	cache	= new LinkedList<>();

	public CommandsCache(Path path) {
		log.debug("New cache file " + path.toAbsolutePath());
		this.path = Objects.requireNonNull(path);
	}

	public void loadFromCache() throws IOException {
		log.debug("Loading cache from file");
		cache.clear();
		if (!Files.exists(path)) {
			log.info("No cache file found at given path");
			return;
		}
		for (JsonElement e : new JsonParser().parse(Files.newBufferedReader(path)).getAsJsonArray()) {
			JsonObject o = e.getAsJsonObject();
			Set<String> commands = new HashSet<>();
			for (JsonElement command : o.getAsJsonArray("commands"))
				commands.add(command.getAsString());
			cache.add(new CacheElement(
					commands,
					o.getAsJsonPrimitive("last-modified").getAsLong(),
					o.getAsJsonPrimitive("lm").getAsString(),
					o.getAsJsonPrimitive("dic").getAsString()));
		}
	}

	public Optional<CacheElement> check(Set<String> commands) {
		return cache.stream().filter(elem -> elem.commands.equals(commands)).findFirst();
	}

	public void addToCache(Set<String> commands, String lm, String dic) {
		cache.removeIf(elem -> elem.commands.equals(commands));
		cache.add(new CacheElement(commands, System.currentTimeMillis(), lm, dic));
	}

	public void saveToFile(int maxElements) throws IOException {
		log.debug("Saving maximally of " + maxElements + " to the cache file.");
		JsonArray array = new JsonArray(maxElements);
		cache.stream().sorted(Comparator.<CacheElement> comparingLong(e -> e.lastModified).reversed()).limit(maxElements).forEach(e -> array.add(e.toJson()));
		Files.write(path, new GsonBuilder().setPrettyPrinting().create().toJson(array).getBytes());
	}

	public static class CacheElement {

		public final Set<String>	commands;
		public final long			lastModified;
		public final String			lm, dic;

		public CacheElement(Set<String> commands, long lastModified, String lm, String dic) {
			this.commands = commands;
			this.lastModified = lastModified;
			this.lm = lm;
			this.dic = dic;
		}

		public JsonElement toJson() {
			JsonObject ret = new JsonObject();
			ret.add("last-modified", new JsonPrimitive(lastModified));
			ret.add("lm", new JsonPrimitive(lm));
			ret.add("dic", new JsonPrimitive(dic));
			JsonArray commands = new JsonArray();
			this.commands.forEach(command -> commands.add(new JsonPrimitive(command)));
			ret.add("commands", commands);
			return ret;
		}
	}
}

package de.piegames.picontrol;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import de.piegames.picontrol.module.Module;
import de.piegames.picontrol.state.VoiceState;
import de.piegames.picontrol.tts.MutedSpeechEngine;
import de.piegames.picontrol.tts.SpeechEngine;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;

public class PiControl {

	protected final Log				log		= LogFactory.getLog(getClass());

	protected boolean				listening;
	protected VoiceState<Module>	stateMachine;
	protected SpeechEngine			tts;
	protected LiveSpeechRecognizer	stt;
	protected Map<String, Module>	modules	= new HashMap<>();

	public PiControl() {
		reload();
		SpeechResult result;
		while (listening) {
			if ((result = stt.getResult()) != null) {
				log.info("You said: " + result.getHypothesis());
				Collection<String> best = result.getNbest(Integer.MAX_VALUE);
				// TODO actually sort them by quality
				best.stream().forEach(System.out::println);
				Module responsible = null;
				String command = null;
				for (String s : best) {
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
					responsible.commandSpoken(stateMachine.getCurrentState(), command);
				else
					log.info("What you just said makes no sense, sorry");
			} else
				Thread.yield();
		}
		exitApplication();
	}

	public void reload() {
		pauseRecognizer();
		// TODO clean up
		log.info("Reloading all modules");
		modules.values().forEach(Module::close);
		modules.clear();

		JsonObject config;
		try {
			config = new JsonParser().parse(Files.newBufferedReader(Paths.get("config.json").toAbsolutePath())).getAsJsonObject();
		} catch (JsonIOException | JsonSyntaxException | IOException e) {
			log.error("Could not read config.json", e);
			exitApplication();
			return;
		}
		try {
			tts = (SpeechEngine) Class.forName(config.getAsJsonPrimitive("speech-synth").getAsString())
					.getConstructor(PiControl.class)
					.newInstance(this);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			log.fatal("Could not instantiate speech synthesizer; switching to MutedSpeechEngine", e);
			tts = new MutedSpeechEngine(this);
		}

		// config.getAsJsonArray("activation-commands")
		stateMachine = new VoiceState<>();

		JsonObject modules = config.getAsJsonObject("modules");
		for (JsonElement element : config.getAsJsonArray("active-modules")) {
			try {
				Module module = (Module) Class.forName(
						modules.getAsJsonObject(element.getAsString()).getAsJsonPrimitive("class-name").getAsString())
						.getConstructor(PiControl.class, String.class, Path.class)
						.newInstance(this, element.getAsString(), Paths.get("modules"));

				stateMachine.addModuleGraph(module.listCommands(stateMachine.getRoot()));
				this.modules.put(element.getAsString(), module);
			} catch (Throwable e) {
				log.warn("Could not instantiate module " + element.getAsString(), e);
			}
		}

		Set<String> commands = stateMachine.getAllCommands();
		if (commands.isEmpty()) {
			log.fatal("No commands registered. This application cannot work without commands");
			exitApplication();
		}
		log.debug("All registered commands:\n" + String.join(System.getProperty("line.separator"), commands));
		commands.remove(null);
		commands.remove("");
		Path lmPath = null;
		Path dicPath = null;
		// Check history for already compiled language models
		int cacheSize = config.getAsJsonPrimitive("corpus-history-size").getAsInt();
		if (cacheSize == 0)
			log.debug("Caching disabled");
		for (int i = 0; i < cacheSize; i++) {
			try {
				Set<String> corpus = new HashSet<>(Files.readAllLines(Paths.get("cached-" + i + ".corpus")));
				corpus.remove(null);
				corpus.remove("");
				if (commands.equals(corpus)) {
					log.debug("Cache hit");
					lmPath = Paths.get("cached-" + i + ".lm");
					dicPath = Paths.get("cached-" + i + ".dic");
				}
			} catch (NoSuchFileException e) {
			} catch (IOException e) {
				log.info("Could not read cache file " + i, e);
			}
		}
		try {
			// TODO de-uglify this mess once it works
			// Do this by putting the cache into one .json file to avoid messing with the file system too much
			if (lmPath == null || dicPath == null) {
				Path corpus = Paths.get("cached-0.corpus");
				long lastModifiedTime = 0;
				int index = 0;
				// Remove least used corpus from history
				// TODO use config instead of hardcoded 10
				for (int i = 0; i < cacheSize; i++) {
					Path p = Paths.get("cached-" + i + ".corpus");
					try {
						if (Files.getLastModifiedTime(p).toMillis() > lastModifiedTime) {
							lastModifiedTime = Files.getLastModifiedTime(p).toMillis();
							corpus = p;
							index = i;
						} else {
							corpus = p;
							index = i;
							break;
						}
					} catch (NoSuchFileException e) {
					} catch (IOException e) {
						log.info("Could not read cache file " + i, e);
					}
				}
				lmPath = Paths.get("cached-" + index + ".lm");
				dicPath = Paths.get("cached-" + index + ".dic");
				// Write them to corpus file
				Files.write(corpus, commands);

				HttpClient client = new DefaultHttpClient();
				String downloadURL;
				{
					MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
					entity.addPart("formtype", new StringBody("simple"));
					entity.addPart("corpus", new FileBody(corpus.toFile()));

					log.debug("Uploading corpus file to \"http://www.speech.cs.cmu.edu/cgi-bin/tools/lmtool/run\". Visit \"http://www.speech.cs.cmu.edu/tools/lmtool.html\" for more information.");
					HttpPost post = new HttpPost("http://www.speech.cs.cmu.edu/cgi-bin/tools/lmtool/run");
					post.setEntity(entity);

					HttpResponse response = client.execute(post);
					log.debug("Response from the server: " + response.getStatusLine() + ", " + Arrays.toString(response.getAllHeaders()));
					downloadURL = response.getFirstHeader("Location").getValue();
					EntityUtils.consume(response.getEntity());
				}
				// Get actual download urls
				String baseName;
				{
					log.debug("The compiled models can be found at and will be downloaded from \"" + downloadURL + "\"");
					// TODO make this work with Apache
					// HttpGet get = new HttpGet(downloadURL);
					// HttpResponse response = client.execute(get);
					// log.debug("Response from the server: " + response.getStatusLine() + ", " + Arrays.toString(response.getAllHeaders()));
					String text = IOUtils.toString(new URL(downloadURL), (Charset) null);
					log.debug("The response from the server: " + text);
					Pattern pattern = Pattern.compile("(<b>)(\\d*?)(</b>)");
					Matcher m = pattern.matcher(text);
					m.find();
					baseName = m.group(2);
					log.debug("The base name is " + baseName);
				}
				// Download new language model
				{
					log.debug("Downloading the language model file from " + downloadURL + "/" + baseName + ".lm");
					IOUtils.copy(new URL(downloadURL + "/" + baseName + ".lm").openStream(), Files.newOutputStream(lmPath));
					log.debug("Downloading the dictionary file from " + downloadURL + "/" + baseName + ".dic");
					IOUtils.copy(new URL(downloadURL + "/" + baseName + ".dic").openStream(), Files.newOutputStream(dicPath));
				}
			}
			// Configure stt
			Configuration sphinxConfig = new Configuration();
			sphinxConfig.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
			sphinxConfig.setDictionaryPath(dicPath.toAbsolutePath().toString());
			sphinxConfig.setLanguageModelPath(lmPath.toAbsolutePath().toString());

			stt = new LiveSpeechRecognizer(sphinxConfig);
		} catch (IOException e) {
			log.fatal("Something went wrong when trying to create the speech recogniser", e);
			exitApplication();
		}

		startRecognizer();
	}

	public void startRecognizer() {
		if (listening)
			log.debug("Resuming speech recognition");
		else
			log.info("Starting speech recognition");
		listening = true;
		stt.startRecognition(true);
	}

	public void pauseRecognizer() {
		if (stt == null)
			return;
		log.debug("Pausing speech recognition");
		stt.stopRecognition();
	}

	public void exitApplication() {
		log.info("Stopping speech recognition");
		listening = false;
		stt.stopRecognition();
		modules.values().forEach(Module::close);
		log.info("Quitting application");
		System.exit(0);
	}

	public SpeechEngine getTTS() {
		return tts;
	}

	public static void main(String... args) {
		new PiControl();
	}
}
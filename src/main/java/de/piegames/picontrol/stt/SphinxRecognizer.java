package de.piegames.picontrol.stt;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonObject;
import de.piegames.picontrol.CommandsCache;
import de.piegames.picontrol.CommandsCache.CacheElement;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;

public class SphinxRecognizer extends SpeechRecognizer {

	protected LiveSpeechRecognizer stt;

	public SphinxRecognizer(JsonObject config) {
		super(config);
	}

	@Override
	public void load(Set<String> commands) throws IOException {
		// Check cache
		CommandsCache cache = new CommandsCache(Paths.get("cache.json"));
		int cacheSize = config.getAsJsonPrimitive("corpus-history-size").getAsInt();
		Path lmPath;
		Path dicPath;
		Path corpusPath;
		lmPath = Files.createTempFile("cached", ".lm");
		dicPath = Files.createTempFile("cached", ".dic");
		corpusPath = Files.createTempFile("cached", ".corpus");
		/* The reason that the temp files are required is that Sphinx only takes URLs as resources in its configuration. If there is a way around saving the
		 * data to a file just to reload it, please add it. (The only currently know option is to register an own URL scheme which is not worth the trouble.) */

		Optional<CacheElement> hit = cache.check(commands);
		if (hit.isPresent()) {
			Files.write(dicPath, hit.get().dic.getBytes());
			Files.write(lmPath, hit.get().lm.getBytes());
		} else {
			Files.write(corpusPath, commands);

			HttpClient client = new DefaultHttpClient();
			String downloadURL;
			{
				MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
				entity.addPart("formtype", new StringBody("simple"));
				entity.addPart("corpus", new FileBody(corpusPath.toFile()));

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

			// But that thing back to cache and write it
			cache.addToCache(commands, new String(Files.readAllBytes(lmPath)), new String(Files.readAllBytes(dicPath)));
			cache.saveToFile(cacheSize);
		}
		// Configure stt
		Configuration sphinxConfig = new Configuration();
		sphinxConfig.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		sphinxConfig.setDictionaryPath(dicPath.toAbsolutePath().toString());
		sphinxConfig.setLanguageModelPath(lmPath.toAbsolutePath().toString());

		stt = new LiveSpeechRecognizer(sphinxConfig);
	}

	@Override
	public Collection<String> nextCommand() throws Exception {
		SpeechResult result;
		if ((result = stt.getResult()) != null) {
			log.info("You said: " + result.getHypothesis());
			Collection<String> best = result.getNbest(Integer.MAX_VALUE);
			// TODO actually sort them by quality
			return best;
		} else
			return null;
	}

	@Override
	public void pauseRecognition() {
		// Don't listen for anything said
		stt.stopRecognition();
		// TODO check if getResult() throws an Exception when run concurrently
	}

	@Override
	public void resumeRecognition() {
		// Continue listening for anything said
		stt.startRecognition(true);
	}

	@Override
	public void stopRecognition() {
		// Stop everything and deallocate
		stt.stopRecognition();
		// Deallocate
	}
}
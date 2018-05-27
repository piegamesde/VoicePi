package de.piegames.voicepi;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Sets;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import de.piegames.voicepi.module.DummyModule;
import de.piegames.voicepi.module.Module;
import de.piegames.voicepi.state.CommandSet;
import de.piegames.voicepi.state.ContextState;
import de.piegames.voicepi.state.VoiceState;
import de.piegames.voicepi.stt.MultiRecognizer;
import de.piegames.voicepi.stt.QueueRecognizer;
import javafx.util.Pair;

public class MultiRecognizerTest {

	public static final int						TIMEOUT			= 2;

	protected MultiRecognizer					stt;
	protected QueueRecognizer					a, b, c, d, e;
	protected BlockingQueue<Collection<String>>	commandsSpoken	= new LinkedBlockingQueue<>();
	protected VoiceState						stateMachine;

	/**
	 * Creates a new state machine with two modules with three states each. Each state is directly reachable through a command and goes on to an end state with
	 * the command end
	 */
	@Before
	public void setup() throws URISyntaxException, IOException {
		stateMachine = new VoiceState();
		ContextState root = stateMachine.getRoot();
		{// ModuleA
			Module module = new DummyModule("moduleA");
			ContextState stateA = new ContextState("moduleA", "stateA");
			ContextState stateB = new ContextState("moduleA", "stateB");
			ContextState stateC = new ContextState("moduleA", "stateC");
			ContextState stateE = new ContextState("moduleA", "end");
			MutableValueGraph<ContextState, CommandSet> graph = ValueGraphBuilder.directed().build();
			graph.putEdgeValue(root, stateA, new CommandSet(module, Sets.newHashSet("AA")));
			graph.putEdgeValue(stateA, stateE, new CommandSet(module, Sets.newHashSet("end")));
			graph.putEdgeValue(root, stateB, new CommandSet(module, Sets.newHashSet("AB")));
			graph.putEdgeValue(stateB, stateE, new CommandSet(module, Sets.newHashSet("end")));
			graph.putEdgeValue(root, stateC, new CommandSet(module, Sets.newHashSet("AC")));
			graph.putEdgeValue(stateC, stateE, new CommandSet(module, Sets.newHashSet("end")));
			stateMachine.addModuleGraph(graph);
		}
		{// ModuleB
			Module module = new DummyModule("moduleB");
			ContextState stateA = new ContextState("moduleB", "stateA");
			ContextState stateB = new ContextState("moduleB", "stateB");
			ContextState stateC = new ContextState("moduleB", "stateC");
			ContextState stateE = new ContextState("moduleB", "end");
			MutableValueGraph<ContextState, CommandSet> graph = ValueGraphBuilder.directed().build();
			graph.putEdgeValue(root, stateA, new CommandSet(module, Sets.newHashSet("BA")));
			graph.putEdgeValue(stateA, stateE, new CommandSet(module, Sets.newHashSet("end")));
			graph.putEdgeValue(root, stateB, new CommandSet(module, Sets.newHashSet("BB")));
			graph.putEdgeValue(stateB, stateE, new CommandSet(module, Sets.newHashSet("end")));
			graph.putEdgeValue(root, stateC, new CommandSet(module, Sets.newHashSet("BC")));
			graph.putEdgeValue(stateC, stateE, new CommandSet(module, Sets.newHashSet("end")));
			stateMachine.addModuleGraph(graph);
		}
	}

	/** Tests the MultiRecognizer in its "only one active at the same time" mode */
	@Test
	public void testOnlyOneActive() throws IOException, InterruptedException {
		a = new QueueRecognizer();
		b = new QueueRecognizer();
		c = new QueueRecognizer();
		d = new QueueRecognizer();
		e = new QueueRecognizer();
		stt = new MultiRecognizer(
				Arrays.asList(
						new Pair<>(a, Arrays.asList("moduleA:*")),
						new Pair<>(b, Arrays.asList("*:stateB")),
						new Pair<>(c, Arrays.asList("moduleB:stateC")),
						new Pair<>(d, Arrays.asList("*:*")),
						new Pair<>(e, Arrays.asList())),
				true, 0);
		stt.load(null, stateMachine, null, commandsSpoken, null);
		stt.startRecognition();

		commandSpoken();
		assertThat(commandsSpoken.poll(TIMEOUT, TimeUnit.SECONDS), is(Arrays.asList("d")));

		testSpoken("AA", "a");
		testSpoken("AB", "a");
		testSpoken("AC", "a");
		testSpoken("BA", "d");
		testSpoken("BB", "b");
		testSpoken("BC", "c");

		stt.stopRecognition();
		stt.unload();
	}

	@Test
	public void testMultipleActive() throws InterruptedException, IOException {
		a = new QueueRecognizer();
		b = new QueueRecognizer();
		c = new QueueRecognizer();
		d = new QueueRecognizer();
		e = new QueueRecognizer();
		stt = new MultiRecognizer(
				Arrays.asList(
						new Pair<>(a, Arrays.asList("moduleA:*")),
						new Pair<>(b, Arrays.asList("*:stateB")),
						new Pair<>(c, Arrays.asList("moduleB:stateC")),
						new Pair<>(d, Arrays.asList("*:*")),
						new Pair<>(e, Arrays.asList())),
				false, 0);
		stt.load(null, stateMachine, null, commandsSpoken, null);
		stt.startRecognition();

		commandSpoken();
		assertThat(commandsSpoken.poll(TIMEOUT, TimeUnit.SECONDS), is(Arrays.asList("d")));

		testSpoken("AA", "a", "d");
		testSpoken("AB", "a", "b", "d");
		testSpoken("AB", "d", "a", "b");
		testSpoken("AC", "a", "d");
		testSpoken("BA", "d");
		testSpoken("BB", "b", "d");
		testSpoken("BC", "c", "d");

		stt.stopRecognition();
		stt.unload();
	}

	@Test
	public void testStartStop() throws InterruptedException, IOException {
		a = new QueueRecognizer();
		b = new QueueRecognizer();
		c = new QueueRecognizer();
		d = new QueueRecognizer();
		e = new QueueRecognizer();
		stt = new MultiRecognizer(
				Arrays.asList(
						new Pair<>(a, Arrays.asList("moduleA:*")),
						new Pair<>(b, Arrays.asList("*:stateB")),
						new Pair<>(c, Arrays.asList("moduleB:stateC")),
						new Pair<>(d, Arrays.asList("*:*")),
						new Pair<>(e, Arrays.asList())),
				false, 0);
		stt.load(null, stateMachine, null, commandsSpoken, null);
		stt.startRecognition();

		commandSpoken();
		assertThat(commandsSpoken.poll(TIMEOUT, TimeUnit.SECONDS), is(Arrays.asList("d")));

		testSpoken("AA", "a", "d");
		testSpoken("AB", "a", "b", "d");
		stt.stopRecognition();
		stt.startRecognition();
		testSpoken("AB", "d", "a", "b");
		stt.stopRecognition();
		stateMachine.commandSpoken("AC");
		stt.startRecognition();
		testSpoken(null, "a", "d");
		testSpoken("BA", "d");
		testSpoken("BB", "b", "d");
		testSpoken("BC", "c", "d");

		stt.stopRecognition();
		stt.unload();
	}

	@Test
	public void testTranscription() {
		// TODO
	}

	private void testSpoken(String state, String... spoken) throws InterruptedException {
		if (state != null)
			stateMachine.commandSpoken(state);
		commandSpoken();
		Set<String> actuallySpoken = new HashSet<>();
		for (int i = 0; i < spoken.length; i++)
			actuallySpoken.addAll(commandsSpoken.poll(TIMEOUT, TimeUnit.SECONDS));
		assertThat(actuallySpoken, is(Sets.newHashSet(spoken)));
		assertTrue(commandsSpoken.isEmpty());
		stateMachine.commandSpoken("end");
	}

	private void commandSpoken() {
		a.commandSpoken("a");
		b.commandSpoken("b");
		c.commandSpoken("c");
		d.commandSpoken("d");
		e.commandSpoken("e");
	}
}
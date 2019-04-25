package de.piegames.voicepi;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import de.piegames.voicepi.state.VoiceStateTest;

@RunWith(Suite.class)
@SuiteClasses({ VoicePiTest.class, VoiceStateTest.class, MultiRecognizerTest.class })
public class AllTests {

}

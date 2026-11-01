


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#isVerified()}.
 */
@ExtendWith(MockitoExtension.class)
class Message_isVerified_Test {

    /**
     * The Message instance used in the tests.
     */
    private Message message;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test to avoid state leakage.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up after each test.
        message = null;
    }

    /**
     * Helper that sets the private {@code tsigState} field via reflection.
     *
     * @param state the value to assign to {@code tsigState}
     */
    private void setTsigState(int state) {
        try {
            Field f = Message.class.getDeclaredField("tsigState");
            f.setAccessible(true);
            f.setInt(message, state);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to set tsigState reflectively", e);
        }
    }

    /**
     * Test data for the true branch – only TSIG_VERIFIED (1) should return true.
     */
    private static Stream<Arguments> trueBranchProvider() {
        return Stream.of(
                Arguments.of(1, true,
                        "When the internal tsigState is set to TSIG_VERIFIED (1), isVerified() should return true.")
        );
    }

    /**
     * Test data for the false branch – all non‑verified or out‑of‑range values.
     */
    private static Stream<Arguments> falseBranchProvider() {
        return Stream.of(
                Arguments.of(0, false, "TSIG_UNSIGNED (0) – not verified."),
                Arguments.of(2, false, "TSIG_INTERMEDIATE (2) – not verified."),
                Arguments.of(3, false, "TSIG_SIGNED (3) – not verified."),
                Arguments.of(4, false, "TSIG_FAILED (4) – not verified."),
                Arguments.of(-1, false, "Negative value – not verified."),
                Arguments.of(5, false, "Value beyond defined constants – not verified.")
        );
    }

    /**
     * By default a newly created Message has {@code tsigState == TSIG_UNSIGNED},
     * therefore {@code isVerified()} must return {@code false}.
     */
    @Test
    void isVerified_defaultMessage_returnsFalse() {
        assertFalse(message.isVerified(),
                "A freshly constructed Message should not be verified");
    }

    /**
     * When a TSIG is attached with a non‑zero error code the internal state is
     * set to {@code TSIG_FAILED}. In that situation {@code isVerified()} must
     * still return {@code false}.
     */
    @Test
    void isVerified_tsigSignedWithError_returnsFalse(@Mock TSIG mockTsig) {
        // Rcode.FORMERR (1) is a typical error code indicating a bad TSIG.
        message.setTSIG(mockTsig, Rcode.FORMERR, null);
        assertFalse(message.isVerified(),
                "Message with a TSIG error should not be verified");
    }

    /**
     * When a TSIG is attached with {@code Rcode.NOERROR} the Message marks the
     * TSIG as verified. Consequently {@code isVerified()} must return {@code true}.
     */
    @Test
    void isVerified_tsigVerified_returnsTrue(@Mock TSIG mockTsig) {
        // Rcode.NOERROR (0) signals a successful verification.
        message.setTSIG(mockTsig, Rcode.NOERROR, null);
        assertTrue(message.isVerified(),
                "Message with a successfully verified TSIG should be verified");
    }

    /**
     * Verifies that {@code isVerified()} returns {@code true} only when
     * {@code tsigState == TSIG_VERIFIED}.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("trueBranchProvider")
    void isVerified_trueBranch(int tsigState, boolean expected, String description) {
        setTsigState(tsigState);
        assertTrue(message.isVerified(),
                "When tsigState == TSIG_VERIFIED, isVerified() should be true");
    }

    /**
     * Verifies that {@code isVerified()} returns {@code false} for all
     * non‑verified or out‑of‑range {@code tsigState} values.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("falseBranchProvider")
    void isVerified_falseBranch(int tsigState, boolean expected, String description) {
        setTsigState(tsigState);
        assertEquals(expected, message.isVerified(),
                "Unexpected result for tsigState = " + tsigState);
    }

    @Test
    void isVerified_idempotent_verifiedState() {
        // TSIG_VERIFIED
        setTsigState(1);
        assertTrue(message.isVerified());
        // second call must still be true
        assertTrue(message.isVerified());
    }

    @Test
    void isVerified_idempotent_unverifiedState() {
        // TSIG_UNSIGNED
        setTsigState(0);
        assertFalse(message.isVerified());
        // second call must still be false
        assertFalse(message.isVerified());
    }

    /**
     * Verifies that {@code isVerified()} returns {@code false} for a range of
     * non‑verified or out‑of‑range {@code tsigState} values.
     */
    @Test
    void isVerified_falseBranches() {
        List<Integer> nonVerifiedStates = Arrays.asList(
                // TSIG_UNSIGNED
                0,
                // TSIG_INTERMEDIATE
                2,
                // TSIG_SIGNED
                3,
                // TSIG_FAILED
                4,
                -1,
                5);
        for (int state : nonVerifiedStates) {
            setTsigState(state);
            assertFalse(message.isVerified(),
                    "When tsigState == " + state + ", isVerified() should be false");
        }
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
org.junit.platform.commons.JUnitException: TestEngine with ID 'junit-jupiter' failed to discover tests
	at org.junit.platform.launcher.core.EngineDiscoveryOrchestrator.discoverEngineRoot(EngineDiscoveryOrchestrator.java:160)
	at org.junit.platform.launcher.core.EngineDiscoveryOrchestrator.discoverSafely(EngineDiscoveryOrchestrator.java:134)
	at org.junit.platform.launcher.core.EngineDiscoveryOrchestrator.discover(EngineDiscoveryOrchestrator.java:108)
	at org.junit.platform.launcher.core.EngineDiscoveryOrchestrator.discover(EngineDiscoveryOrchestrator.java:80)
	at org.junit.platform.launcher.core.DefaultLauncher.discover(DefaultLauncher.java:110)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:86)
	at org.junit.platform.launcher.core.DefaultLauncherSession$DelegatingLauncher.execute(DefaultLauncherSession.java:86)
	at org.junit.platform.launcher.core.SessionPerRequestLauncher.execute(SessionPerRequestLauncher.java:53)
	at org.junit.platform.console.tasks.ConsoleTestExecutor.executeTests(ConsoleTestExecutor.java:66)
	at org.junit.platform.console.tasks.ConsoleTestExecutor.lambda$execute$0(ConsoleTestExecutor.java:58)
	at org.junit.platform.console.tasks.CustomContextClassLoaderExecutor.invoke(CustomContextClassLoaderExecutor.java:33)
	at org.junit.platform.console.tasks.ConsoleTestExecutor.execute(ConsoleTestExecutor.java:58)
	at org.junit.platform.console.ConsoleLauncher.executeTests(ConsoleLauncher.java:120)
	at org.junit.platform.console.ConsoleLauncher.execute(ConsoleLauncher.java:82)
	at org.junit.platform.console.ConsoleLauncher.execute(ConsoleLauncher.java:55)
	at org.junit.platform.console.ConsoleLauncher.execute(ConsoleLauncher.java:48)
	at org.junit.platform.console.ConsoleLauncher.main(ConsoleLauncher.java:42)
Caused by: org.junit.platform.commons.JUnitException: OutputDirectoryCreator not available; probably due to unaligned versions of the junit-platform-engine and junit-platform-launcher jars on the classpath/module path.
	at org.junit.platform.engine.EngineDiscoveryRequest.getOutputDirectoryCreator(EngineDiscoveryRequest.java:109)
	at org.junit.jupiter.engine.JupiterTestEngine.discover(JupiterTestEngine.java:68)
	at org.junit.platform.launcher.core.EngineDiscoveryOrchestrator.discoverEngineRoot(EngineDiscoveryOrchestrator.java:152)
	... 16 more


Usage: ConsoleLauncher [OPTIONS]
Launches the JUnit Platform for test discovery and execution.
      [@<filename>...]       One or more argument files containing options.

COMMANDS

  -h, --help                 Display help information.
      --list-engines         List all observable test engines.

SELECTORS

      --scan-classpath, --scan-class-path[=PATH]
                             Scan all directories on the classpath or explicit classpath
                               roots. Without arguments, only directories on the system
                               classpath as well as additional classpath entries supplied via
                               -cp (directories and JAR files) are scanned. Explicit classpath
                               roots that are not on the classpath will be silently ignored.
                               This option can be repeated.
      --scan-modules         EXPERIMENTAL: Scan all resolved modules for test discovery.
  -u, --select-uri=URI       Select a URI for test discovery. This option can be repeated.
  -f, --select-file=FILE     Select a file for test discovery. This option can be repeated.
  -d, --select-directory=DIR Select a directory for test discovery. This option can be
                               repeated.
  -o, --select-module=NAME   EXPERIMENTAL: Select single module for test discovery. This
                               option can be repeated.
  -p, --select-package=PKG   Select a package for test discovery. This option can be repeated.
  -c, --select-class=CLASS   Select a class for test discovery. This option can be repeated.
  -m, --select-method=NAME   Select a method for test discovery. This option can be repeated.
  -r, --select-resource=RESOURCE
                             Select a classpath resource for test discovery. This option can
                               be repeated.
  -i, --select-iteration=TYPE:VALUE[INDEX(..INDEX)?(,INDEX(..INDEX)?)*]
                             Select iterations for test discovery (e.g. method:com.acme.Foo#m()
                               [1..2]). This option can be repeated.

FILTERS

  -n, --include-classname=PATTERN
                             Provide a regular expression to include only classes whose fully
                               qualified names match. To avoid loading classes unnecessarily,
                               the default pattern only includes class names that begin with
                               "Test" or end with "Test" or "Tests". When this option is
                               repeated, all patterns will be combined using OR semantics.
                               Default: ^(Test.*|.+[.$]Test.*|.*Tests?)$
  -N, --exclude-classname=PATTERN
                             Provide a regular expression to exclude those classes whose fully
                               qualified names match. When this option is repeated, all
                               patterns will be combined using OR semantics.
      --include-package=PKG  Provide a package to be included in the test run. This option can
                               be repeated.
      --exclude-package=PKG  Provide a package to be excluded from the test run. This option
                               can be repeated.
  -t, --include-tag=TAG      Provide a tag or tag expression to include only tests whose tags
                               match. When this option is repeated, all patterns will be
                               combined using OR semantics.
  -T, --exclude-tag=TAG      Provide a tag or tag expression to exclude those tests whose tags
                               match. When this option is repeated, all patterns will be
                               combined using OR semantics.
  -e, --include-engine=ID    Provide the ID of an engine to be included in the test run. This
                               option can be repeated.
  -E, --exclude-engine=ID    Provide the ID of an engine to be excluded from the test run.
                               This option can be repeated.

RUNTIME CONFIGURATION

      -cp, --classpath, --class-path=PATH
                             Provide additional classpath entries -- for example, for adding
                               engines and their dependencies. This option can be repeated.
      --config=KEY=VALUE     Set a configuration parameter for test discovery and execution.
                               This option can be repeated.

REPORTING

      --fail-if-no-tests     Fail and return exit status code 2 if no tests are found.
      --reports-dir=DIR      Enable report output into a specified local directory (will be
                               created if it does not exist).

CONSOLE OUTPUT

      --disable-ansi-colors  Disable ANSI colors in output (not supported by all terminals).
      --color-palette=FILE   Specify a path to a properties file to customize ANSI style of
                               output (not supported by all terminals).
      --single-color         Style test output using only text attributes, no color (not
                               supported by all terminals).
      --disable-banner       Disable print out of the welcome message.
      --details=MODE         Select an output details mode for when tests are executed. Use
                               one of: none, summary, flat, tree, verbose. If 'none' is
                               selected, then only the summary and test failures are shown.
                               Default: tree.
      --details-theme=THEME  Select an output details tree theme for when tests are executed.
                               Use one of: ascii, unicode. Default is detected based on
                               default character encoding.

For more information, please refer to the JUnit User Guide at
https://junit.org/junit5/docs/current/user-guide/

```


And you can use the following related information to assist you in your analysis and correction:

api document of class org.xbill.DNS.Message: 

 * A DNS Message. A message is the basic unit of communication between the client and server of a
 * DNS operation. A message consists of a Header and 4 message sections.
 *
 * @see Resolver
 * @see Header
 * @see Section
 * @author Brian Wellington
 

api document of method isVerified(): 

   * If this message was signed by a TSIG, was the TSIG verified?
   *
   * @see TSIG
   

return type: 
boolean

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		int org.xbill.DNS.Message.tsigState;
		int org.xbill.DNS.Message.TSIG_VERIFIED;
	related functions:
		method `public boolean isVerified()` returns `boolean`, related with `org.xbill.DNS.Message.isVerified()`, api document: 
   * If this message was signed by a TSIG, was the TSIG verified?
   *
   * @see TSIG
   ;
		method `public boolean isSigned()` returns `boolean`, related with `org.xbill.DNS.Message.isVerified()`, api document: 
   * Was this message signed by a TSIG?
   *
   * @see TSIG
   ;
class `org.xbill.DNS.TSIG`:
	related functions:
		method `public void apply(Message, int, TSIGRecord, boolean)` returns `void`, related with `org.xbill.DNS.Message.isVerified()`, api document: 
   * Generates a TSIG record with a specific error for a message and adds it to the message.
   *
   * @param m The message
   * @param error The error
   * @param old If this message is a response, the TSIG from the request
   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and
   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for
   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).
   * @since 3.2
   ;
		method `private int verify(Message, byte[], TSIGRecord, boolean, Mac)` returns `int`, related with `org.xbill.DNS.Message.isVerified()`, api document: 
   * Verifies a TSIG record on an incoming message. Since this is only called in the context where a
   * TSIG is expected to be present, it is an error if one is not present. After calling this
   * routine, Message.isVerified() may be called on this message.
   *
   * @param m The message to verify
   * @param messageBytes An array containing the message in unparsed form. This is necessary since
   *     TSIG signs the message in wire format, and we can't recreate the exact wire format (with
   *     the same name compression).
   * @param requestTSIG If this message is a response, the TSIG from the request
   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and
   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for
   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).
   * @return The result of the verification (as an Rcode)
   * @see Rcode
   ;
class `org.xbill.DNS.TSIG.StreamGenerator`:
	related functions:
		method ` void generate(Message, boolean)` returns `void`, related with `org.xbill.DNS.Message.isVerified()`;
class `org.xbill.DNS.ExtendedFlags`:
	related functions:
		method `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.isVerified()`, api document:  Converts a numeric extended flag into a String ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 

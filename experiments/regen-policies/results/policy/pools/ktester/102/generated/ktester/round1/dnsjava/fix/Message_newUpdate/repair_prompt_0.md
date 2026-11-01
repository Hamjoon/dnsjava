


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class Message_newUpdate_Test {

    @Test
    void newUpdate_createsUpdateMessageWithCorrectZone() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message msg = Message.newUpdate(zone);
        // then
        assertNotNull(msg, "Message returned by newUpdate should not be null");
        assertTrue(msg instanceof Update, "Message returned by newUpdate should be an instance of Update");
        // The opcode of an Update message must be UPDATE
        assertEquals(Opcode.UPDATE, msg.getHeader().getOpcode(), "Header opcode should be UPDATE");
        // The question section should contain the zone name
        assertNotNull(msg.getQuestion(), "Question record should be present");
        assertEquals(zone, msg.getQuestion().getName(), "Question name should match the zone supplied to newUpdate");
        // All sections other than QUESTION must be empty initially
        assertTrue(msg.getSection(Section.ANSWER).isEmpty(), "ANSWER section must be empty");
        assertTrue(msg.getSection(Section.AUTHORITY).isEmpty(), "AUTHORITY section must be empty");
        assertTrue(msg.getSection(Section.ADDITIONAL).isEmpty(), "ADDITIONAL section must be empty");
    }

    @Test
    void newUpdate_nullName_throwsNullPointerException() {
        // given
        Name nullZone = null;
        // when / then
        assertThrows(NullPointerException.class, () -> Message.newUpdate(nullZone), "Calling newUpdate with a null Name should throw NPE");
    }

    @Test
    void newUpdate_isDistinctFromNewQuery() {
        // given
        Name name = Name.fromConstantString("example.com.");
        Message updateMsg = Message.newUpdate(name);
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message queryMsg = Message.newQuery(question);
        // when / then
        assertNotEquals(updateMsg.getHeader().getOpcode(), queryMsg.getHeader().getOpcode(), "Update and Query messages must have different opcodes");
        assertEquals(Opcode.UPDATE, updateMsg.getHeader().getOpcode(), "Update message opcode must be UPDATE");
        assertEquals(Opcode.QUERY, queryMsg.getHeader().getOpcode(), "Query message opcode must be QUERY");
    }

    @ParameterizedTest(name = "[{index}] zone = {0}")
    @CsvSource({ "'example.com.', true", "'sub.domain.example.org.', true", // root zone
    "'., true'" })
    void newUpdate_validNames(String zoneString, boolean isRoot) {
        // given
        Name zone = Name.fromConstantString(zoneString);
        // when
        Message msg = Message.newUpdate(zone);
        // then
        assertNotNull(msg, "Message should not be null");
        assertTrue(msg instanceof Update, "Message should be an Update instance");
        assertEquals(Opcode.UPDATE, msg.getHeader().getOpcode(), "Header opcode must be UPDATE");
        assertNotNull(msg.getQuestion(), "Question record must be present");
        if (isRoot) {
            assertEquals(Name.root, msg.getQuestion().getName(), "Root zone should result in Name.root as the question name");
        } else {
            assertEquals(zone, msg.getQuestion().getName(), "Question name must match the supplied zone");
        }
    }

    @Test
    void newUpdate_initialState_sectionsAreEmpty() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        // then
        assertTrue(update.getSection(Section.ANSWER).isEmpty(), "ANSWER section must be empty initially");
        assertTrue(update.getSection(Section.AUTHORITY).isEmpty(), "AUTHORITY section must be empty initially");
        assertTrue(update.getSection(Section.ADDITIONAL).isEmpty(), "ADDITIONAL section must be empty initially");
    }

    @Test
    void newUpdate_initialState_notSignedAndNoTSIG() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        // then
        assertFalse(update.isSigned(), "A freshly created Update must not be signed");
        assertNull(update.getTSIG(), "A freshly created Update must have no TSIG record attached");
    }

    @Test
    void newUpdate_headerFlags_QR_AA_cleared() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        Header header = update.getHeader();
        // then
        assertFalse(header.getFlag(Flags.QR), "QR flag (response) must be cleared for a new Update");
        assertFalse(header.getFlag(Flags.AA), "AA flag (authoritative answer) must be cleared for a new Update");
    }

    @Test
    void newUpdate_headerFlags_rcodeIsNoError() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        Header header = update.getHeader();
        // then
        assertEquals(Rcode.NOERROR, header.getRcode(), "RCODE of a new Update must be NOERROR (0)");
    }

    @Test
    void newUpdate_invalidName_nonAbsolute_throwsIllegalArgumentException() {
        // The relative name is created without a trailing dot.
        // Name.fromString will throw IllegalArgumentException for a non‑absolute name,
        // which satisfies the contract of Message.newUpdate.
        assertThrows(IllegalArgumentException.class, () -> {
            // relative (no trailing dot)
            Name relative = Name.fromString("example.com");
            Message.newUpdate(relative);
        }, "Providing a non‑absolute Name should result in IllegalArgumentException");
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
 

api document of method newUpdate(Name): 

   * Creates a new Message to contain a dynamic update. A random Message ID and the zone are filled
   * in.
   *
   * @param zone The zone to be updated
   

return type: 
org.xbill.DNS.Message

dependent classes: 
class `org.xbill.DNS.Name`:
	
class `org.xbill.DNS.Message`:
	related functions:
		method `public static Message newUpdate(Name)` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.newUpdate(Name)`, api document: 
   * Creates a new Message to contain a dynamic update. A random Message ID and the zone are filled
   * in.
   *
   * @param zone The zone to be updated
   ;
class `org.xbill.DNS.ExtendedFlags`:
	related functions:
		method `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.newUpdate(Name)`, api document:  Converts a numeric extended flag into a String ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 

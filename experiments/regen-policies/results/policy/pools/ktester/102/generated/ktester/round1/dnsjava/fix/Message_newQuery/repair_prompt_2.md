


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;

class Message_newQuery_Test {

    /**
     * Supplies a stream of arguments for {@link #newQuery_variousRecordTypes_createsCorrectMessage}.
     *
     * @return a stream of {@link Arguments} each containing a type description and a {@link Record}
     */
    static Stream<Arguments> provideRecordTypes() {
        Name name = Name.fromConstantString("example.com.");
        return Stream.of(
                Arguments.of("A", Record.newRecord(name, Type.A, DClass.IN)),
                Arguments.of("AAAA", Record.newRecord(name, Type.AAAA, DClass.IN)),
                Arguments.of("MX", Record.newRecord(name, Type.MX, DClass.IN))
        );
    }

    /**
     * Helper that performs the common assertions for a given record type.
     */
    private void assertStandardQuery(Message query, Record record, String typeName) {
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(),
                "Header opcode should be QUERY for " + typeName + " record");
        assertTrue(query.getHeader().getFlag(Flags.RD),
                "RD flag should be set for " + typeName + " record");
        assertEquals(record, query.getQuestion(),
                "Question record should match the supplied " + typeName + " record");
        assertEquals(record, query.getSection(Section.QUESTION).get(0),
                "QUESTION section should contain the supplied " + typeName + " record");
        assertEquals(1, query.getHeader().getCount(Section.QUESTION),
                "Header count for QUESTION should be 1 for " + typeName + " record");
        for (int sec = Section.ANSWER; sec <= Section.ADDITIONAL; sec++) {
            assertTrue(query.getSection(sec).isEmpty(),
                    "Section " + sec + " should be empty for " + typeName + " record");
            assertEquals(0, query.getHeader().getCount(sec),
                    "Header count for section " + sec + " should be zero for " + typeName + " record");
        }
    }

    /**
     * Verifies that {@code Message.newQuery(record)} creates a message with the correct header
     * fields and places the record in the QUESTION section.
     */
    @Test
    void newQuery_createsMessageWithCorrectHeaderAndQuestion() {
        // Arrange: a simple A‑record question
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        // Act: create the query message
        Message query = Message.newQuery(question);
        // Assert: header opcode must be QUERY
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(),
                "Header opcode should be QUERY");
        // Assert: Recursion Desired flag must be set
        assertTrue(query.getHeader().getFlag(Flags.RD),
                "Header should have the RD (recursion desired) flag set");
        // Assert: the question record is stored correctly
        assertEquals(question, query.getQuestion(),
                "The question record returned by getQuestion() must match the supplied record");
        assertEquals(question, query.getSection(Section.QUESTION).get(0),
                "The QUESTION section should contain the supplied record as its first entry");
        // Assert: header count for QUESTION section is 1
        assertEquals(1, query.getHeader().getCount(Section.QUESTION),
                "Header count for QUESTION section should be 1");
        // Assert: all other sections are empty and counts are zero
        for (int sec = Section.ANSWER; sec <= Section.ADDITIONAL; sec++) {
            assertTrue(query.getSection(sec).isEmpty(),
                    "Section " + sec + " should be empty");
            assertEquals(0, query.getHeader().getCount(sec),
                    "Header count for section " + sec + " should be zero");
        }
    }

    /**
     * Verifies that passing {@code null} as the record argument results in a {@link NullPointerException}.
     */
    @Test
    void newQuery_nullRecord_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> Message.newQuery(null),
                "Message.newQuery(null) should throw NullPointerException");
    }

    /**
     * Ensures that the created message is independent of later modifications to the original
     * {@link Record} (the record is immutable in the DNS library, but this test guards against
     * accidental coupling).
     */
    @Test
    void newQuery_messageIsIndependentFromOriginalRecord() {
        Name qname = Name.fromConstantString("host.example.");
        Record original = Record.newRecord(qname, Type.AAAA, DClass.IN);
        Message query = Message.newQuery(original);
        // The library's Record objects are immutable, but we can still verify that the
        // message holds a reference that equals the original.
        assertEquals(original, query.getQuestion(),
                "Message should contain a question equal to the original record");
    }

    /**
     * Parameterized test that creates queries for several different record types and verifies
     * that the resulting message always has the correct header fields, question placement and
     * empty non‑question sections.
     *
     * @param typeName a human‑readable name for the record type (used only for test reporting)
     * @param record   the Record instance to be used as the query question
     */
    @ParameterizedTest(name = "newQuery with {0} record")
    @MethodSource("provideRecordTypes")
    void newQuery_variousRecordTypes_createsCorrectMessage(String typeName, Record record) {
        // Act: create a query for the supplied record
        Message query = Message.newQuery(record);
        // Header must be a standard query with recursion desired
        assertEquals(Opcode.QUERY, query.getHeader().getOpcode(),
                "Header opcode should be QUERY for " + typeName + " record");
        assertTrue(query.getHeader().getFlag(Flags.RD),
                "RD flag should be set for " + typeName + " record");
        // The question must be exactly the supplied record
        assertEquals(record, query.getQuestion(),
                "Question record should match the supplied " + typeName + " record");
        assertEquals(record, query.getSection(Section.QUESTION).get(0),
                "QUESTION section should contain the supplied " + typeName + " record");
        // Header count for QUESTION must be 1
        assertEquals(1, query.getHeader().getCount(Section.QUESTION),
                "Header count for QUESTION should be 1 for " + typeName + " record");
        // All other sections must be empty and have a count of zero
        for (int sec = Section.ANSWER; sec <= Section.ADDITIONAL; sec++) {
            assertTrue(query.getSection(sec).isEmpty(),
                    "Section " + sec + " should be empty for " + typeName + " record");
            assertEquals(0, query.getHeader().getCount(sec),
                    "Header count for section " + sec + " should be zero for " + typeName + " record");
        }
    }

    /**
     * A‑record query
     */
    @Test
    void newQuery_ARecord_createsCorrectMessage() {
        Name name = Name.fromConstantString("example.com.");
        Record a = Record.newRecord(name, Type.A, DClass.IN);
        Message query = Message.newQuery(a);
        assertStandardQuery(query, a, "A");
    }

    /**
     * AAAA‑record query
     */
    @Test
    void newQuery_AAAARecord_createsCorrectMessage() {
        Name name = Name.fromConstantString("example.com.");
        Record aaaa = Record.newRecord(name, Type.AAAA, DClass.IN);
        Message query = Message.newQuery(aaaa);
        assertStandardQuery(query, aaaa, "AAAA");
    }

    /**
     * MX‑record query
     */
    @Test
    void newQuery_MXRecord_createsCorrectMessage() {
        Name name = Name.fromConstantString("example.com.");
        Record mx = Record.newRecord(name, Type.MX, DClass.IN);
        Message query = Message.newQuery(mx);
        assertStandardQuery(query, mx, "MX");
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
 

api document of method newQuery(Record): 

   * Creates a new Message with a random Message ID suitable for sending as a query.
   *
   * @param r A record containing the question
   

return type: 
org.xbill.DNS.Message

dependent classes: 
class `org.xbill.DNS.Record`:
	
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.QUESTION;
	
class `org.xbill.DNS.Message`:
	dependent fields:
		org.xbill.DNS.Header org.xbill.DNS.Message.header;
	
class `org.xbill.DNS.Opcode`:
	dependent fields:
		int org.xbill.DNS.Opcode.QUERY;
	
class `org.xbill.DNS.Flags`:
	dependent fields:
		byte org.xbill.DNS.Flags.RD;
	
class `org.xbill.DNS.tools.update`:
	related functions:
		method `public Message newMessage()` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.newQuery(Record)`;
class `org.xbill.DNS.tools.jnamed`:
	related functions:
		method ` byte[] buildErrorMessage(Header, int, Record)` returns `byte[]`, related with `org.xbill.DNS.Message.newQuery(Record)`;
class `org.xbill.DNS.ZoneTransferIn`:
	related functions:
		method `private void sendQuery()` returns `void`, related with `org.xbill.DNS.Message.newQuery(Record)`;
class `org.xbill.DNS.dnssec.SMessage`:
	related functions:
		method `public Message getMessage()` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.newQuery(Record)`, api document: 
   * Gets this message as a standard DNSJAVA message.
   *
   * @return This message as a standard DNSJAVA message.
   ;
class `org.xbill.DNS.Update`:
	related functions:
		method `private void newPrereq(Record)` returns `void`, related with `org.xbill.DNS.Message.newQuery(Record)`;
class `org.xbill.DNS.SimpleResolver`:
	related functions:
		method `private void applyEDNS(Message)` returns `void`, related with `org.xbill.DNS.Message.newQuery(Record)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 

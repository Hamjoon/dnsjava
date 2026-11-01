


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class Message_removeAllRecords_Test {

    private Message message;

    private static final Name TEST_NAME = Name.fromConstantString("example.com.");

    /**
     * Populate a fresh {@link Message} with one record in each of the four sections.
     */
    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        // uses a random ID, fine for our purposes
        message = new Message();
        // QUESTION section
        Record q = Record.newRecord(TEST_NAME, Type.A, DClass.IN);
        message.addRecord(q, Section.QUESTION);
        // ANSWER section
        Record a = new ARecord(TEST_NAME, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        message.addRecord(a, Section.ANSWER);
        // AUTHORITY section
        Record ns = Record.newRecord(TEST_NAME, Type.NS, DClass.IN);
        message.addRecord(ns, Section.AUTHORITY);
        // ADDITIONAL section (use a simple TXT record)
        Record txt = new TXTRecord(TEST_NAME, DClass.IN, 0, "test");
        message.addRecord(txt, Section.ADDITIONAL);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up.
        message = null;
    }

    /* -------------------------------------------------------------
     * Helper to inspect the private {@code sections} array via reflection.
     * ------------------------------------------------------------- */
    private List<Record> getInternalSectionArray(int section) throws ReflectiveOperationException {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] arr = (List<Record>[]) f.get(message);
        return arr[section];
    }

    /**
     * Data provider for valid sections – both non‑empty and already empty.
     */
    private static Stream<Arguments> provideValidSectionScenarios() {
        return Stream.of(// Non‑empty sections (populated by @BeforeEach)
        Arguments.of(Section.ANSWER, false, "Remove records from a section that initially contains a non‑empty list of records."), // Already empty sections – we will clear them first in the test body
        Arguments.of(Section.ADDITIONAL, true, "Remove records from a section that is already empty (null) and has a zero count."));
    }

    /**
     * Data provider for invalid negative section index.
     */
    private static Stream<Arguments> provideNegativeInvalidSections() {
        return Stream.of(Arguments.of(-1, "Negative index should be rejected."), Arguments.of(-42, "Another negative index."));
    }

    /**
     * Data provider for out‑of‑range section indices (>= Section.count).
     */
    private static Stream<Arguments> provideOutOfRangeInvalidSections() {
        // Section constants are 0‑3; 4 is the first invalid value.
        return Stream.of(Arguments.of(4, "Index equal to number of sections should be invalid."), Arguments.of(5, "Index greater than number of sections should be invalid."));
    }

    @Test
    void removeAllRecords_clearsSpecifiedSection_andLeavesOthersIntact()throws Exception {
        // Verify pre‑condition: each section contains exactly one record
        assertEquals(1, message.getSection(Section.QUESTION).size());
        assertEquals(1, message.getSection(Section.ANSWER).size());
        assertEquals(1, message.getSection(Section.AUTHORITY).size());
        assertEquals(1, message.getSection(Section.ADDITIONAL).size());
        // Remove ANSWER section
        message.removeAllRecords(Section.ANSWER);
        // ANSWER section must be empty and header count zero
        List<Record> answer = message.getSection(Section.ANSWER);
        assertTrue(answer.isEmpty(), "ANSWER section should be empty after removal");
        assertEquals(0, message.getHeader().getCount(Section.ANSWER));
        // internal array entry must be null
        assertNull(getInternalSectionArray(Section.ANSWER), "Internal sections[ANSWER] should be null after removal");
        // Other sections must stay unchanged
        assertEquals(1, message.getSection(Section.QUESTION).size());
        assertEquals(1, message.getSection(Section.AUTHORITY).size());
        assertEquals(1, message.getSection(Section.ADDITIONAL).size());
    }

    @Test
    void removeAllRecords_isIdempotent()throws Exception {
        // First removal
        message.removeAllRecords(Section.AUTHORITY);
        assertTrue(message.getSection(Section.AUTHORITY).isEmpty());
        assertEquals(0, message.getHeader().getCount(Section.AUTHORITY));
        assertNull(getInternalSectionArray(Section.AUTHORITY));
        // Second removal – should not throw and state stays the same
        message.removeAllRecords(Section.AUTHORITY);
        assertTrue(message.getSection(Section.AUTHORITY).isEmpty(), "Second call should keep AUTHORITY section empty");
        assertEquals(0, message.getHeader().getCount(Section.AUTHORITY));
        assertNull(getInternalSectionArray(Section.AUTHORITY));
    }

    @Test
    void removeAllRecords_invalidSection_throwsException() {
        // Section indices are 0‑3; 4 is invalid and should cause IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> message.removeAllRecords(4));
        // Negative index also invalid
        assertThrows(IllegalArgumentException.class, () -> message.removeAllRecords(-1));
    }

    @Test
    void removeAllRecords_allSectionsSequentially()throws Exception {
        // Remove each section one after another and verify the cumulative effect
        for (int sec = 0; sec < 4; sec++) {
            message.removeAllRecords(sec);
            // The just‑removed section must be empty
            assertTrue(message.getSection(sec).isEmpty(), "Section " + sec + " should be empty after removal");
            assertEquals(0, message.getHeader().getCount(sec), "Header count for section " + sec + " should be zero");
            assertNull(getInternalSectionArray(sec));
        }
        // After all removals, the message should contain no records at all
        for (int sec = 0; sec < 4; sec++) {
            assertTrue(message.getSection(sec).isEmpty(), "All sections should be empty after removing all");
        }
    }

    @ParameterizedTest(name = "[{index}] removeAllRecords on section {0} (alreadyEmpty={1})")
    @MethodSource("provideValidSectionScenarios")
    void removeAllRecords_validSections(int section, boolean alreadyEmpty, String description) throws Exception {
        // If the scenario requires an already‑empty section, clear it first.
        if (alreadyEmpty) {
            // Ensure the internal array entry is null and header count is zero.
            message.removeAllRecords(section);
            assertTrue(message.getSection(section).isEmpty(), "Pre‑condition: section should be empty before the actual test.");
            assertEquals(0, message.getHeader().getCount(section));
            assertNull(getInternalSectionArray(section));
        } else {
            // Verify pre‑condition: the section contains exactly one record.
            assertEquals(1, message.getSection(section).size(), "Pre‑condition: section should contain one record.");
            assertNotEquals(0, message.getHeader().getCount(section));
        }
        // Act
        message.removeAllRecords(section);
        // Assert: internal array entry must be null and header count zero.
        assertNull(getInternalSectionArray(section), "Internal sections[" + section + "] must be null after removal.");
        assertEquals(0, message.getHeader().getCount(section), "Header count for section " + section + " must be zero after removal.");
        assertTrue(message.getSection(section).isEmpty(), "Public API getSection must return an empty list after removal.");
    }

    @ParameterizedTest(name = "[{index}] removeAllRecords with invalid negative section {0}")
    @MethodSource("provideNegativeInvalidSections")
    void removeAllRecords_invalidSection_negative(int invalidSection, String description) {
        assertThrows(IllegalArgumentException.class, () -> message.removeAllRecords(invalidSection), "Expected IllegalArgumentException for section " + invalidSection);
    }

    @ParameterizedTest(name = "[{index}] removeAllRecords with out‑of‑range section {0}")
    @MethodSource("provideOutOfRangeInvalidSections")
    void removeAllRecords_invalidSection_outOfRange(int invalidSection, String description) {
        assertThrows(IllegalArgumentException.class, () -> message.removeAllRecords(invalidSection), "Expected IllegalArgumentException for out‑of‑range section " + invalidSection);
    }

    /**
     * Idempotent test for multiple calls on the same section (ADDITIONAL).
     */
    @Test
    void removeAllRecords_idempotent_multipleCalls() throws Exception {
        int sec = Section.ADDITIONAL;
        // First call – should clear the section.
        message.removeAllRecords(sec);
        assertNull(getInternalSectionArray(sec));
        assertEquals(0, message.getHeader().getCount(sec));
        assertTrue(message.getSection(sec).isEmpty());
        // Second call – state must stay the same and no exception should be thrown.
        message.removeAllRecords(sec);
        assertNull(getInternalSectionArray(sec));
        assertEquals(0, message.getHeader().getCount(sec));
        assertTrue(message.getSection(sec).isEmpty());
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
 

api document of method removeAllRecords(int): 

   * Removes all records from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
		org.xbill.DNS.Header org.xbill.DNS.Message.header;
	related functions:
		method `public List<Record> getSection(int)` returns `java.util.List<org.xbill.DNS.Record>`, related with `org.xbill.DNS.Message.removeAllRecords(int)`, api document: 
   * Returns all records in the given section, or an empty list if the section is empty.
   *
   * @see Record
   * @see Section
   ;
		method `public List<RRset> getSectionRRsets(int)` returns `java.util.List<org.xbill.DNS.RRset>`, related with `org.xbill.DNS.Message.removeAllRecords(int)`, api document: 
   * Returns an array containing all records in the given section grouped into RRsets.
   *
   * @see RRset
   * @see Section
   ;
		method ` void toWire(DNSOutput)` returns `void`, related with `org.xbill.DNS.Message.removeAllRecords(int)`;
		method `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.removeAllRecords(int)`, api document: 
   * Determines if an RRset with the given name and type is already present in the given section.
   *
   * @see RRset
   * @see Section
   ;
		method `public boolean removeRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.removeAllRecords(int)`, api document: 
   * Removes a record from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.removeAllRecords(int)`, api document: 
   * Adds a record to a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public void setHeader(Header)` returns `void`, related with `org.xbill.DNS.Message.removeAllRecords(int)`, api document: 
   * Replaces the Header with a new one.
   *
   * @see Header
   ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 

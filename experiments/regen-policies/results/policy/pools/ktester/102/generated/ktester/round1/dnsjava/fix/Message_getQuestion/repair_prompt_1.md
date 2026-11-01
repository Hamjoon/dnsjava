


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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.*;

@ExtendWith(MockitoExtension.class)
class Message_getQuestion_Test {

    /**
     * Message instance used by the tests.
     */
    private Message message;

    /**
     * A sample question record used by several tests.
     */
    private Record questionRecord;

    /**
     * A second question record to test ordering.
     */
    private Record secondQuestionRecord;

    @BeforeEach
    void setupBeforeEach() throws UnknownHostException {
        // Create a fresh Message for each test.
        message = new Message();
        // Build a simple A‑record question: www.example.com. IN A
        Name qname = Name.fromConstantString("www.example.com.");
        questionRecord = Record.newRecord(qname, Type.A, DClass.IN);
        // A second question record (different name) to verify ordering.
        Name qname2 = Name.fromConstantString("mail.example.com.");
        secondQuestionRecord = Record.newRecord(qname2, Type.A, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern symmetrical.
        message = null;
        questionRecord = null;
        secondQuestionRecord = null;
    }

    @Test
    void getQuestion_whenQuestionSectionIsEmpty_returnsNull() {
        // By default a newly constructed Message has an empty QUESTION section.
        assertNull(message.getQuestion(), "Expected null when QUESTION section is empty");
    }

    @Test
    void getQuestion_whenQuestionSectionIsNull_returnsNull() {
        // Simulate a null QUESTION section by directly removing all records.
        // The public API does not expose a way to set the internal array to null,
        // but removing all records leaves the list empty, which is equivalent for the
        // contract of getQuestion().
        message.removeAllRecords(Section.QUESTION);
        assertNull(message.getQuestion(), "Expected null after removing all QUESTION records");
    }

    @Test
    void getQuestion_withSingleRecord_returnsThatRecord() {
        // Add a single question record.
        message.addRecord(questionRecord, Section.QUESTION);
        Record result = message.getQuestion();
        assertNotNull(result, "Result should not be null when a question is present");
        assertEquals(questionRecord, result, "Returned record should be the one that was added");
        assertEquals(questionRecord.getName(), result.getName(), "Returned record should have the same name as the added question");
    }

    @Test
    void getQuestion_withMultipleRecords_returnsFirstRecord() {
        // Add two question records; the first added should be returned.
        message.addRecord(questionRecord, Section.QUESTION);
        message.addRecord(secondQuestionRecord, Section.QUESTION);
        Record result = message.getQuestion();
        assertNotNull(result, "Result should not be null when multiple questions are present");
        assertEquals(questionRecord, result, "The first record added to the QUESTION section must be returned");
    }

    @Test
    void getQuestion_doesNotReturnRecordsFromOtherSections() throws IOException {
        // Add a question record to the QUESTION section.
        message.addRecord(questionRecord, Section.QUESTION);
        // Add a record to the ANSWER section – it must not affect getQuestion().
        Name answerName = Name.fromConstantString("www.example.com.");
        Record answerRecord = new ARecord(answerName, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
        message.addRecord(answerRecord, Section.ANSWER);
        Record result = message.getQuestion();
        assertEquals(questionRecord, result, "Only records from the QUESTION section should be considered");
    }

    @Test
    void getQuestion_afterCloningPreservesQuestion() throws CloneNotSupportedException {
        // Build a query message with a question, then clone it.
        message.addRecord(questionRecord, Section.QUESTION);
        Message clone = message.clone();
        assertNotNull(clone.getQuestion(), "Cloned message should retain the question");
        assertEquals(message.getQuestion(), clone.getQuestion(), "Cloned message's question must be equal to the original's");
    }

    @Test
    void getQuestion_parsesFromWireAndReturnsCorrectRecord() throws IOException {
        // Construct a message, serialize it, then deserialize to ensure the wire format
        // correctly restores the question.
        message.addRecord(questionRecord, Section.QUESTION);
        byte[] wire = message.toWire();
        Message fromWire = new Message(wire);
        Record fromWireQuestion = fromWire.getQuestion();
        assertNotNull(fromWireQuestion, "Deserialized message should contain a question");
        assertEquals(questionRecord.getName(), fromWireQuestion.getName(), "Deserialized question name must match the original");
        assertEquals(questionRecord.getType(), fromWireQuestion.getType(), "Deserialized question type must match the original");
    }

    @Test
    void getQuestion_questionSectionContainsNullRecord_returnsNull() throws Exception {
        // Use reflection to inject a List containing a single null element into the QUESTION slot.
        @SuppressWarnings("unchecked")
        List<Record>[] // 4 = number of sections
        sections = (List<Record>[]) new List[4];
        sections[Section.QUESTION] = Collections.singletonList(null);
        setPrivateSections(message, sections);
        assertNull(message.getQuestion(), "When the first element of the QUESTION list is null, getQuestion() must return null");
    }

    @Test
    void getQuestion_questionSectionIsNullArrayEntry_returnsNull() throws Exception {
        // Simulate a null entry for the QUESTION section (the array exists, but the slot is null).
        @SuppressWarnings("unchecked")
        List<Record>[] // 4 = number of sections
        sections = (List<Record>[]) new List[4];
        // Do NOT initialise sections[Section.QUESTION]; it stays null.
        setPrivateSections(message, sections);
        assertNull(message.getQuestion(), "When the QUESTION section array entry is null, getQuestion() should return null");
    }

    @Test
    void getQuestion_messageCreatedViaNewQuery_returnsQueryRecord() {
        // newQuery should place the supplied record into the QUESTION section.
        Message query = Message.newQuery(questionRecord);
        Record result = query.getQuestion();
        assertNotNull(result, "newQuery should produce a non‑null question");
        assertEquals(questionRecord, result, "The query record must be returned by getQuestion()");
    }

    @Test
    void getQuestion_messageCreatedViaNewUpdate_returnsNull() {
        // Update messages do not have a QUESTION section.
        Name zone = Name.fromConstantString("example.com.");
        Message update = Message.newUpdate(zone);
        assertNull(update.getQuestion(), "Update messages should have no QUESTION section, thus getQuestion() returns null");
    }

    @Test
    void getQuestion_questionSectionBecomesEmptyAfterRemoval_returnsNull() {
        // Add then remove all records; the method must return null afterwards.
        message.addRecord(questionRecord, Section.QUESTION);
        assertNotNull(message.getQuestion(), "Pre‑condition: question must be present");
        message.removeAllRecords(Section.QUESTION);
        assertNull(message.getQuestion(), "After removing all QUESTION records, getQuestion() must return null");
    }

    @Test
    void getQuestion_repeatedCallsReturnSameInstance() {
        message.addRecord(questionRecord, Section.QUESTION);
        Record first = message.getQuestion();
        Record second = message.getQuestion();
        assertSame(first, second, "Multiple invocations should return the exact same Record instance");
    }

    @Test
    void getQuestion_removeSoleRecordViaRemoveRecord_returnsNull() {
        message.addRecord(questionRecord, Section.QUESTION);
        assertTrue(message.removeRecord(questionRecord, Section.QUESTION), "removeRecord should succeed for the sole question");
        assertNull(message.getQuestion(), "After removal of the only question record, getQuestion() must return null");
    }

    @Test
    void getQuestion_sectionsArrayIsNull_throwsNullPointerException() throws Exception {
        // Set the entire sections array to null via reflection.
        setPrivateSections(message, null);
        assertThrows(NullPointerException.class, message::getQuestion, "When the internal sections array is null, getQuestion() should throw NPE");
    }

    @Test
    void getQuestion_sectionsArrayTooShort_throwsArrayIndexOutOfBoundsException() throws Exception {
        // Create an array shorter than Section.QUESTION (which is index 0) – length 0.
        @SuppressWarnings("unchecked")
        List<Record>[] shortArray = (List<Record>[]) new List[0];
        setPrivateSections(message, shortArray);
        assertThrows(ArrayIndexOutOfBoundsException.class, message::getQuestion, "When the sections array is too short, getQuestion() should throw AIOOBE");
    }

    /* -------------------------------------------------------------------------
   * Helper method to set the private `sections` field via reflection.
   * ----------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static void setPrivateSections(Message msg, List<Record>[] sections) throws NoSuchFieldException, IllegalAccessException {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(msg, sections);
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
 

api document of method getQuestion(): 

   * Returns the first record in the QUESTION section.
   *
   * @see Record
   * @see Section
   

return type: 
org.xbill.DNS.Record

dependent classes: 
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.QUESTION;
	
class `org.xbill.DNS.Message`:
	dependent fields:
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
	related functions:
		method `public List<RRset> getSectionRRsets(int)` returns `java.util.List<org.xbill.DNS.RRset>`, related with `org.xbill.DNS.Message.getQuestion()`, api document: 
   * Returns an array containing all records in the given section grouped into RRsets.
   *
   * @see RRset
   * @see Section
   ;
		method `public TSIGRecord getTSIG()` returns `org.xbill.DNS.TSIGRecord`, related with `org.xbill.DNS.Message.getQuestion()`, api document: 
   * Returns the TSIG record from the ADDITIONAL section, if one is present.
   *
   * @see TSIGRecord
   * @see TSIG
   * @see Section
   ;
		method `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.getQuestion()`, api document: 
   * Determines if an RRset with the given name and type is already present in the given section.
   *
   * @see RRset
   * @see Section
   ;
class `org.xbill.DNS.dnssec.SMessage`:
	related functions:
		method `public int getCount(int)` returns `int`, related with `org.xbill.DNS.Message.getQuestion()`, api document: 
   * Gets the number of records.
   *
   * @param section The section for which the records are counted.
   * @return The number of records for the queried section.
   ;
class `org.xbill.DNS.dnssec.SRRset`:
	related functions:
		method `public Name getSignerName()` returns `org.xbill.DNS.Name`, related with `org.xbill.DNS.Message.getQuestion()`, api document:  The "signer" name for this {@link SRRset}, if signed, or {@code null} if not. ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 

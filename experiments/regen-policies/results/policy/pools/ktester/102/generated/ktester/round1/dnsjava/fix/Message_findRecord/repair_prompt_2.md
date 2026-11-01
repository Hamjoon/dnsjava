


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

class Message_findRecord_Test {

    /**
     * Message instance used in each test
     */
    private Message message;

    /**
     * A generic A‑record used for the tests
     */
    private Record aRecord;

    @BeforeEach
    void setupBeforeEach() throws TextParseException {
        // Create a fresh Message with an empty header
        message = new Message();
        // Build a simple A‑record: www.example.com.  IN  A  127.0.0.1
        Name name = Name.fromConstantString("www.example.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent
        message = null;
        aRecord = null;
    }

    @Test
    void findRecord_EmptySection_ReturnsFalse() {
        // Section.ANSWER is empty at this point
        boolean found = message.findRecord(aRecord, Section.ANSWER);
        assertFalse(found, "findRecord should return false when the section is empty");
    }

    @Test
    void findRecord_RecordPresentInSection_ReturnsTrue() {
        // Add the record to the QUESTION section
        message.addRecord(aRecord, Section.QUESTION);
        // The same record should now be found in the QUESTION section
        assertTrue(message.findRecord(aRecord, Section.QUESTION), "findRecord should return true when the record is present in the given section");
    }

    @Test
    void findRecord_RecordPresentInOtherSection_ReturnsFalse() {
        // Add the record to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);
        // Query a different section (QUESTION) – should be false
        assertFalse(message.findRecord(aRecord, Section.QUESTION), "findRecord should return false when the record exists but in a different section");
    }

    @Test
    void findRecord_InvalidSection_ThrowsIllegalArgumentException() {
        // Section.check throws IllegalArgumentException for out‑of‑range values
        // valid sections are 0‑3
        int invalidSection = 5;
        assertThrows(IllegalArgumentException.class, () -> message.findRecord(aRecord, invalidSection), "findRecord should throw IllegalArgumentException for an invalid section index");
    }

    @Test
    void findRecord_NullSectionArray_TreatedAsEmpty() {
        // Construct a Message via the DNSInput constructor that leaves a section null.
        // The simplest way is to use the no‑arg constructor and then manually clear a section.
        // Since we must not access private fields, we simulate the situation by removing all records.
        // ensures the internal list is null or empty
        message.removeAllRecords(Section.ADDITIONAL);
        // The method should safely return false even if the internal list is null.
        assertFalse(message.findRecord(aRecord, Section.ADDITIONAL), "findRecord should return false when the internal list for the section is null/empty");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_NullRecord_ListPresent_ReturnsFalse() {
        // Initialise a non‑null list by adding aRecord to QUESTION
        message.addRecord(aRecord, Section.QUESTION);
        // Pass a null record – List.contains(null) is false
        assertFalse(message.findRecord(null, Section.QUESTION), "findRecord should return false when the supplied record is null even if the section list exists");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_NullRecord_NullList_ReturnsFalse() {
        // No record has been added to QUESTION, therefore sections[QUESTION] is null
        assertFalse(message.findRecord(null, Section.QUESTION), "findRecord should safely return false when both the record and the internal list are null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_DuplicateRecords_ReturnsTrue() {
        // Add the same record twice to ANSWER
        message.addRecord(aRecord, Section.ANSWER);
        // duplicate
        message.addRecord(aRecord, Section.ANSWER);
        assertTrue(message.findRecord(aRecord, Section.ANSWER), "findRecord must return true when the record appears more than once in the section");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_IdenticalButDifferentInstance_ReturnsTrue() throws Exception {
        // Add aRecord (created in @BeforeEach) to ADDITIONAL
        message.addRecord(aRecord, Section.ADDITIONAL);
        // Create a new ARecord with the same fields
        Name name = Name.fromConstantString("www.example.");
        Record another = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        assertTrue(message.findRecord(another, Section.ADDITIONAL), "findRecord should return true for a different Record instance that is equal according to Record.equals");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_SameNameTypeDifferentData_ReturnsFalse() throws Exception {
        // Store aRecord (127.0.0.1) in AUTHORITY
        message.addRecord(aRecord, Section.AUTHORITY);
        // Create a record with same name/type but different address
        Name name = Name.fromConstantString("www.example.");
        Record different = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("9.9.9.9"));
        assertFalse(message.findRecord(different, Section.AUTHORITY), "findRecord must return false when the record differs in rdata even if name and type match");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_RecordInDifferentSection_ReturnsFalse() {
        // Add aRecord to ANSWER
        message.addRecord(aRecord, Section.ANSWER);
        // Query AUTHORITY – should be false
        assertFalse(message.findRecord(aRecord, Section.AUTHORITY), "findRecord must only look at the supplied section");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_InvalidSection_Negative_ThrowsIllegalArgumentException() {
        int invalidSection = -1;
        assertThrows(IllegalArgumentException.class, () -> message.findRecord(aRecord, invalidSection), "findRecord should reject negative section indexes");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_InvalidSection_OutOfRange_ThrowsIllegalArgumentException() {
        // valid sections are 0‑3
        int invalidSection = 10;
        assertThrows(IllegalArgumentException.class, () -> message.findRecord(aRecord, invalidSection), "findRecord should reject section indexes larger than the maximum allowed");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_EmptyInitializedList_ReturnsFalse() throws Exception {
        // Force an empty, non‑null list via reflection
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(message);
        // Ensure the array itself exists; if it is null, create a new one.
        if (sections == null) {
            // The number of sections is always 4 (QUESTION, ANSWER, AUTHORITY, ADDITIONAL)
            sections = (List<Record>[]) new List[4];
            sectionsField.set(message, sections);
        }
        // Initialise an empty ArrayList for SECTION.ANSWER
        sections[Section.ANSWER] = new ArrayList<>();
        // The list is empty – method must return false
        Record someRecord = new ARecord(Name.fromString("example.com."), DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
        assertFalse(message.findRecord(someRecord, Section.ANSWER), "findRecord should return false when the section list exists but contains no records");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void findRecord_UninitializedSectionsArray_ThrowsNullPointerException() throws Exception {
        // Set the private 'sections' field to null via reflection
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(message, null);
        Record someRecord = new ARecord(Name.fromString("example.com."), DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
        assertThrows(NullPointerException.class, () -> message.findRecord(someRecord, Section.ANSWER), "When the internal sections array is null, findRecord should propagate a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void findRecord_SectionListNull_ReturnsFalse() {
//         // No records have been added to SECTION.ADDITIONAL, so sections[ADDITIONAL] stays null
//         Record someRecord = new ARecord(Name.fromString("example.com."), DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
//         assertFalse(message.findRecord(someRecord, Section.ADDITIONAL), "findRecord must return false when the internal list for the section is null");
//     }
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
 

api document of method findRecord(Record, int): 

   * Determines if the given record is already present in the given section.
   *
   * @see Record
   * @see Section
   

return type: 
boolean

dependent classes: 
class `org.xbill.DNS.Record`:
	
class `org.xbill.DNS.Message`:
	dependent fields:
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
	related functions:
		method `public void removeAllRecords(int)` returns `void`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Removes all records from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public boolean findRecord(Record)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Determines if the given record is already present in any section.
   *
   * @see Record
   * @see Section
   ;
		method `public List<RRset> getSectionRRsets(int)` returns `java.util.List<org.xbill.DNS.RRset>`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Returns an array containing all records in the given section grouped into RRsets.
   *
   * @see RRset
   * @see Section
   ;
		method `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Determines if an RRset with the given name and type is already present in the given section.
   *
   * @see RRset
   * @see Section
   ;
		method `public boolean removeRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Removes a record from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public List<Record> getSection(int)` returns `java.util.List<org.xbill.DNS.Record>`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Returns all records in the given section, or an empty list if the section is empty.
   *
   * @see Record
   * @see Section
   ;
		method `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.findRecord(Record, int)`, api document: 
   * Adds a record to a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 

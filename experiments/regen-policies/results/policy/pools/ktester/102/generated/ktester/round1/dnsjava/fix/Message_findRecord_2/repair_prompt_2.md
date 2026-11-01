


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import java.lang.reflect.Field;
import java.util.*;
// import org.junit.*;
// import static org.junit.Assert.*;

class Message_findRecord_2_Test {

    private Message message;

    private Record aRecord;

    private Record txtRecord;

//     @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a fresh Message for each test
        message = new Message();
        // Prepare a couple of records that can be used in the tests
        Name name = Name.fromConstantString("www.example.com.");
        aRecord = Record.newRecord(name, Type.A, DClass.IN);
        txtRecord = Record.newRecord(name, Type.TXT, DClass.IN);
    }

//     @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        message = null;
        aRecord = null;
        txtRecord = null;
    }

//     private static Stream<Arguments> provideFoundInVariousSections() {
//         return // target in ANSWER
//         Stream.// target in ANSWER
//         of(// target in AUTHORITY, ANSWER null
//         Arguments.of(Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN), Section.ANSWER, "The target record is added to the ANSWER section. The loop checks Section.ANSWER first, finds a non‑null list containing the record, and returns true immediately."), // target in ADDITIONAL, previous sections empty or null
//         Arguments.of(Record.newRecord(Name.fromConstantString("example.org."), Type.MX, DClass.IN), Section.AUTHORITY, "The ANSWER section is null, the AUTHORITY section contains the target record. The method skips the null ANSWER, evaluates AUTHORITY, finds the record and returns true."), // target in ADDITIONAL, ANSWER empty list, AUTHORITY non‑null but not containing
//         Arguments.of(Record.newRecord(Name.fromConstantString("example.net."), Type.TXT, DClass.IN), Section.ADDITIONAL, "Both ANSWER and AUTHORITY sections are either null or do not contain the record, but the ADDITIONAL section holds the record. The loop reaches ADDITIONAL, finds the record and returns true."), Arguments.of(Record.newRecord(Name.fromConstantString("example.edu."), Type.AAAA, DClass.IN), Section.ADDITIONAL, "The ANSWER section contains an empty list, the AUTHORITY section contains a non‑null list that does not contain the record, and the ADDITIONAL section contains the record. The method correctly continues past empty/non‑matching sections and finally returns true."));
//     }

//     private static Stream<Arguments> provideNotFoundCases() {
//         return // all sections null
//         Stream.// all sections null
//         of(// sections non‑null but without the record
//         Arguments.of(Record.newRecord(Name.fromConstantString("missing.com."), Type.SRV, DClass.IN), "All three sections (ANSWER, AUTHORITY, ADDITIONAL) are null. The loop never enters the inner condition and returns false after completing all iterations."), // empty lists and null sections
//         Arguments.of(Record.newRecord(Name.fromConstantString("absent.org."), Type.NS, DClass.IN), "Each section is non‑null but contains lists that do not include the target record. The method checks each list, finds `contains(r)` false each time, and finally returns false."), Arguments.of(Record.newRecord(Name.fromConstantString("empty.net."), Type.CNAME, DClass.IN), "ANSWER section is an empty list, AUTHORITY section is null, ADDITIONAL section is an empty list. No `contains` check succeeds, so the method returns false."));
//     }

//     private static Stream<Arguments> provideMultipleSectionCases() {
//         return Stream.of(Arguments.of(Record.newRecord(Name.fromConstantString("multi.com."), Type.A, DClass.IN), "The target record is added to both the ANSWER and AUTHORITY sections. The method should return true after checking the ANSWER section and must not continue to later sections."), Arguments.of(Record.newRecord(Name.fromConstantString("multi.org."), Type.MX, DClass.IN), "The target record appears in ANSWER, AUTHORITY and ADDITIONAL sections simultaneously. The loop should stop at the first match (ANSWER) and return true."));
//     }

//     private static Stream<Arguments> provideNullRecordCases() {
//         return Stream.of(Arguments.of("A null Record is passed. All non‑null sections contain regular records, but `contains(null)` never matches, so the method should safely return false without throwing an exception."), Arguments.of("All three sections are null and the argument is null. The method should simply iterate, skip null sections and finally return false."));
//     }

    /**
     * Clears all sections (sets each entry to {@code null}) using reflection.
     */
    private static void clearAllSections(Message msg) throws Exception {
        @SuppressWarnings("unchecked")
        List<Record>[] empty = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        setSectionsArray(msg, empty);
    }

    /**
     * Sets a specific section list via reflection.
     *
     * @param msg     Message instance
     * @param section Section constant (e.g. {@link Section#ANSWER})
     * @param list    List to store in that section (may be {@code null})
     */
    private static void setSectionList(Message msg, int section, List<Record> list) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) f.get(msg);
        if (sections == null) {
            sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
            f.set(msg, sections);
        }
        sections[section] = list;
    }

    /**
     * Directly replaces the whole {@code sections} array.
     *
     * @param msg      Message instance
     * @param newArray New array (may be {@code null})
     */
    private static void setSectionsArray(Message msg, List<Record>[] newArray) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(msg, newArray);
    }

//     @Test
//     void findRecord_returnsTrueWhenRecordIsInAnswerSection() {
//         message.addRecord(aRecord, Section.ANSWER);
//         assertTrue("findRecord should return true for a record present in the ANSWER section", message.findRecord(aRecord));
//     }

//     @Test
//     void findRecord_returnsFalseWhenRecordIsNotPresent() {
//         assertFalse("findRecord should return false when the record is not present in any section", message.findRecord(txtRecord));
//     }

//     @Test
//     void findRecord_checksAllSections() {
//         message.addRecord(aRecord, Section.QUESTION);
//         message.addRecord(aRecord, Section.ADDITIONAL);
//         assertTrue("findRecord should locate the record regardless of which section it resides in", message.findRecord(aRecord));
//     }

//     @Test
//     void findRecord_returnsFalseWhenRecordExistsInDifferentSection() {
//         message.addRecord(txtRecord, Section.ANSWER);
//         assertFalse("findRecord should return false when the exact Record instance is not present", message.findRecord(aRecord));
//     }

//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("provideFoundInVariousSections")
//     void findRecord_returnsTrueWhenRecordIsFoundInVariousSections(Record target, int sectionToAdd, String description) throws Exception {
//         // target in ANSWER
//         Record target = Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN);
//         clearAllSections(message);
//         message.addRecord(target, Section.ANSWER);
//         assertTrue(message.findRecord(target));
//         // target in AUTHORITY (ANSWER null)
//         target = Record.newRecord(Name.fromConstantString("example.org."), Type.MX, DClass.IN);
//         clearAllSections(message);
//         message.addRecord(target, Section.AUTHORITY);
//         assertTrue(message.findRecord(target));
//         // target in ADDITIONAL (ANSWER & AUTHORITY empty/null)
//         target = Record.newRecord(Name.fromConstantString("example.net."), Type.TXT, DClass.IN);
//         clearAllSections(message);
//         message.addRecord(target, Section.ADDITIONAL);
//         assertTrue(message.findRecord(target));
//         // target in ADDITIONAL, ANSWER empty list, AUTHORITY non‑null but not containing
//         target = Record.newRecord(Name.fromConstantString("example.edu."), Type.AAAA, DClass.IN);
//         clearAllSections(message);
//         setSectionList(message, Section.ANSWER, Collections.emptyList());
//         setSectionList(message, Section.AUTHORITY, Collections.singletonList(Record.newRecord(Name.fromConstantString("other.edu."), Type.A, DClass.IN)));
//         message.addRecord(target, Section.ADDITIONAL);
//         assertTrue(message.findRecord(target));
//     }

//     @ParameterizedTest(name = "[{index}] {1}")
//     @MethodSource("provideNotFoundCases")
//     void findRecord_returnsFalseWhenRecordIsAbsent(Record target, String description) throws Exception {
//         // all sections null
//         clearAllSections(message);
//         Record target = Record.newRecord(Name.fromConstantString("missing.com."), Type.SRV, DClass.IN);
//         assertFalse(message.findRecord(target));
//         // sections non‑null but without the record
//         clearAllSections(message);
//         Record unrelated = Record.newRecord(Name.fromConstantString("other.com."), Type.A, DClass.IN);
//         message.addRecord(unrelated, Section.ANSWER);
//         setSectionList(message, Section.ADDITIONAL, Collections.emptyList());
//         target = Record.newRecord(Name.fromConstantString("absent.org."), Type.NS, DClass.IN);
//         assertFalse(message.findRecord(target));
//         // empty lists and null sections
//         clearAllSections(message);
//         setSectionList(message, Section.ANSWER, Collections.emptyList());
//         setSectionList(message, Section.AUTHORITY, null);
//         setSectionList(message, Section.ADDITIONAL, Collections.emptyList());
//         target = Record.newRecord(Name.fromConstantString("empty.net."), Type.CNAME, DClass.IN);
//         assertFalse(message.findRecord(target));
//     }

//     @ParameterizedTest(name = "[{index}] {1}")
//     @MethodSource("provideMultipleSectionCases")
//     void findRecord_earlyExitWhenRecordInMultipleSections(Record target, String description) throws Exception {
//         Record target = Record.newRecord(Name.fromConstantString("multi.com."), Type.A, DClass.IN);
//         message.addRecord(target, Section.ANSWER);
//         message.addRecord(target, Section.AUTHORITY);
//         message.addRecord(target, Section.ADDITIONAL);
//         assertTrue("The method should stop at the first match (ANSWER) and return true", message.findRecord(target));
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("provideNullRecordCases")
//     void findRecord_returnsFalseWhenArgumentIsNull(String description) throws Exception {
//         clearAllSections(message);
//         Record dummy = Record.newRecord(Name.fromConstantString("dummy.com."), Type.A, DClass.IN);
//         message.addRecord(dummy, Section.ANSWER);
//         message.addRecord(dummy, Section.AUTHORITY);
//         message.addRecord(dummy, Section.ADDITIONAL);
//         assertFalse(message.findRecord((Record) null));
//     }

//     @Test
//     void findRecord_returnsTrueWhenListContainsNullBeforeTarget() throws Exception {
//         Record target = Record.newRecord(Name.fromConstantString("nullentry.com."), Type.TXT, DClass.IN);
//         List<Record> list = new ArrayList<>(Arrays.asList(null, target));
//         setSectionList(message, Section.ANSWER, list);
//         assertTrue(message.findRecord(target));
//     }

//     @Test
//     void findRecord_returnsFalseWhenOnlyNullInList() throws Exception {
//         List<Record> list = new ArrayList<>(Collections.singletonList(null));
//         setSectionList(message, Section.ANSWER, list);
//         Record target = Record.newRecord(Name.fromConstantString("onlynull.com."), Type.AAAA, DClass.IN);
//         assertFalse(message.findRecord(target));
//     }

//     @Test
//     void findRecord_findsRecordInAdditionalAfterNullAndEmptySections() throws Exception {
//         setSectionList(message, Section.AUTHORITY, Collections.emptyList());
//         Record target = Record.newRecord(Name.fromConstantString("last.com."), Type.SRV, DClass.IN);
//         message.addRecord(target, Section.ADDITIONAL);
//         assertTrue(message.findRecord(target));
//     }

//     @Test
//     void findRecord_throwsNullPointerExceptionWhenSectionsArrayIsNull() throws Exception {
//         setSectionsArray(message, null);
//         Record target = Record.newRecord(Name.fromConstantString("test.com."), Type.A, DClass.IN);
//         try {
//             message.findRecord(target);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

//     @Test
//     void findRecord_throwsArrayIndexOutOfBoundsExceptionWhenSectionsArrayTooSmall() throws Exception {
//         @SuppressWarnings("unchecked")
//         List<Record>[] tiny = (List<Record>[]) new List[2];
//         setSectionsArray(message, tiny);
//         Record target = Record.newRecord(Name.fromConstantString("tiny.com."), Type.TXT, DClass.IN);
//         try {
//             message.findRecord(target);
//             fail("Expected ArrayIndexOutOfBoundsException");
//         } catch (ArrayIndexOutOfBoundsException ignored) {
//         }
//     }

//     @Test
//     void findRecord_propagatesExceptionFromRecordEquals() throws Exception {
//         // Mockito is available in the classpath of the original project.
//         Record problematic = org.mockito.Mockito.mock(Record.class);
//         org.mockito.Mockito.when(problematic.equals(org.mockito.Mockito.any())).thenThrow(new RuntimeException("equals exploded"));
//         Record dummy = Record.newRecord(Name.fromConstantString("dummy.com."), Type.A, DClass.IN);
//         message.addRecord(dummy, Section.ANSWER);
//         try {
//             message.findRecord(problematic);
//             fail("Expected RuntimeException");
//         } catch (RuntimeException e) {
//             assertEquals("equals exploded", e.getMessage());
//         }
//     }

//     @Before
//     public void setup() throws Exception {
//         message = new Message();
//         Name name = Name.fromConstantString("www.example.com.");
//         aRecord = Record.newRecord(name, Type.A, DClass.IN);
//         txtRecord = Record.newRecord(name, Type.TXT, DClass.IN);
//     }

//     @After
//     public void teardown() {
//         message = null;
//         aRecord = null;
//         txtRecord = null;
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
 

api document of method findRecord(Record): 

   * Determines if the given record is already present in any section.
   *
   * @see Record
   * @see Section
   

return type: 
boolean

dependent classes: 
class `org.xbill.DNS.Record`:
	
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.ADDITIONAL;
		int org.xbill.DNS.Section.ANSWER;
	
class `org.xbill.DNS.Message`:
	dependent fields:
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
	related functions:
		method `public boolean findRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record)`, api document: 
   * Determines if the given record is already present in the given section.
   *
   * @see Record
   * @see Section
   ;
		method `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.findRecord(Record)`;
		method `public boolean findRRset(Name, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record)`, api document: 
   * Determines if an RRset with the given name and type is already present in any section.
   *
   * @see RRset
   * @see Section
   ;
		method `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRecord(Record)`, api document: 
   * Determines if an RRset with the given name and type is already present in the given section.
   *
   * @see RRset
   * @see Section
   ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 




You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class Message_removeRecord_Test {

    private Message message;

    private Record aRecord;

    private Record txtRecord;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test
        message = new Message();
        // Create simple records to work with.
        // Record.newRecord creates a generic Record (type A for the first, TXT for the second)
        aRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.A, DClass.IN);
        txtRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.TXT, DClass.IN);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        message = null;
        aRecord = null;
        txtRecord = null;
    }

    /**
     * Replace the internal {@code sections} array via reflection.
     */
    private void setSectionsArray(List<Record>[] sections) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(message, sections);
    }

    /**
     * Replace the internal {@code header} field via reflection.
     */
    private void setHeader(Header hdr) throws Exception {
        Field f = Message.class.getDeclaredField("header");
        f.setAccessible(true);
        f.set(message, hdr);
    }

    @Test
    void removeRecord_successfulRemoval_decrementsHeaderCount() {
        // GIVEN: a record added to the ANSWER section
        message.addRecord(aRecord, Section.ANSWER);
        Header header = message.getHeader();
        assertEquals(1, header.getCount(Section.ANSWER), "Header count should be 1 after adding the record");
        // WHEN: we remove the same record
        boolean removed = message.removeRecord(aRecord, Section.ANSWER);
        // THEN: removal succeeds, header count is decremented, and the section is empty
        assertTrue(removed, "removeRecord should return true when the record exists");
        assertEquals(0, header.getCount(Section.ANSWER), "Header count should be decremented after removal");
        assertTrue(message.getSection(Section.ANSWER).isEmpty(), "Section should be empty after the record is removed");
    }

    @Test
    void removeRecord_recordNotPresent_returnsFalseAndHeaderUnchanged() {
        // GIVEN: a record added to the ANSWER section, but we will try to remove a different record
        message.addRecord(aRecord, Section.ANSWER);
        Header header = message.getHeader();
        assertEquals(1, header.getCount(Section.ANSWER));
        // WHEN: we attempt to remove a record that was never added
        boolean removed = message.removeRecord(txtRecord, Section.ANSWER);
        // THEN: removal fails, header count stays the same, and the original record remains
        assertFalse(removed, "removeRecord should return false when the record is absent");
        assertEquals(1, header.getCount(Section.ANSWER), "Header count must remain unchanged when removal fails");
        assertEquals(1, message.getSection(Section.ANSWER).size(), "The original record should still be present in the section");
        assertSame(aRecord, message.getSection(Section.ANSWER).get(0));
    }

    @Test
    void removeRecord_sectionNeverInitialized_returnsFalse() {
        // GIVEN: a fresh Message with no sections initialized (no records added)
        // No explicit action needed – the constructor leaves all section lists null.
        // WHEN: we try to remove a record from a section that has never been created
        boolean removed = message.removeRecord(aRecord, Section.AUTHORITY);
        // THEN: removal should simply return false without throwing
        assertFalse(removed, "removeRecord must return false when the section list is null");
        // Header count for that section should still be zero
        assertEquals(0, message.getHeader().getCount(Section.AUTHORITY));
    }

    @Test
    void removeRecord_invalidSection_throwsIllegalArgumentException() {
        // GIVEN: a valid record added to a valid section
        message.addRecord(aRecord, Section.ADDITIONAL);
        // WHEN / THEN: passing an invalid section index should cause Section.check to throw
        assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, 5), "Section.check should reject an out‑of‑range section index");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_invalidUpperBoundSection_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, 5));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_emptySectionList_returnsFalse() throws Exception {
        // GIVEN – initialise an empty mutable list for SECTION=2 (ANSWER)
        List<Record>[] sections = new List[4];
        sections[Section.ANSWER] = new ArrayList<>();
        setSectionsArray(sections);
        // WHEN
        boolean removed = message.removeRecord(aRecord, Section.ANSWER);
        // THEN
        assertFalse(removed);
        // Header should stay untouched – count remains 0
        assertEquals(0, message.getHeader().getCount(Section.ANSWER));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_duplicateRecord_removesOneAndDecrementsHeaderOnce() throws Exception {
        // GIVEN – list with the same mock record twice
        Record dup = mock(Record.class);
        List<Record> list = new ArrayList<>(Arrays.asList(dup, dup));
        List<Record>[] sections = new List[4];
        sections[Section.AUTHORITY] = list;
        setSectionsArray(sections);
        // Ensure header count reflects two records
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.AUTHORITY)).thenReturn(2);
        setHeader(mockHeader);
        // WHEN
        boolean removed = message.removeRecord(dup, Section.AUTHORITY);
        // THEN
        assertTrue(removed);
        // one occurrence remains
        assertEquals(1, list.size());
        verify(mockHeader, times(1)).decCount(Section.AUTHORITY);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_nullRecordInList_successfulRemoval() throws Exception {
        // GIVEN – list contains a single null entry
        List<Record> list = new ArrayList<>(Collections.singletonList(null));
        List<Record>[] sections = new List[4];
        sections[Section.ADDITIONAL] = list;
        setSectionsArray(sections);
        // Mock header to verify decCount call
        Header mockHeader = mock(Header.class);
        setHeader(mockHeader);
        // WHEN
        boolean removed = message.removeRecord(null, Section.ADDITIONAL);
        // THEN
        assertTrue(removed);
        assertTrue(list.isEmpty());
        verify(mockHeader, times(1)).decCount(Section.ADDITIONAL);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_immutableSectionList_throwsUnsupportedOperationException() throws Exception {
        // GIVEN – immutable list for section 0 (QUESTION)
        List<Record>[] sections = new List[4];
        sections[Section.QUESTION] = Collections.emptyList();
        setSectionsArray(sections);
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> message.removeRecord(aRecord, Section.QUESTION));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_nullSectionsArray_throwsNullPointerException() throws Exception {
        // GIVEN – force the internal sections array to null
        setSectionsArray(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> message.removeRecord(aRecord, Section.ANSWER));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_nullHeader_throwsNullPointerException() throws Exception {
        // GIVEN – a mutable list containing the record
        List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
        List<Record>[] sections = new List[4];
        sections[Section.AUTHORITY] = list;
        setSectionsArray(sections);
        // make header null
        setHeader(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> message.removeRecord(aRecord, Section.AUTHORITY));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_headerDecCountThrows_IllegalArgumentException() throws Exception {
        // GIVEN – list with the record
        List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
        List<Record>[] sections = new List[4];
        sections[Section.ANSWER] = list;
        setSectionsArray(sections);
        // Mock header whose decCount throws
        Header mockHeader = mock(Header.class);
        doThrow(new IllegalArgumentException("count underflow")).when(mockHeader).decCount(Section.ANSWER);
        setHeader(mockHeader);
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, Section.ANSWER));
        assertEquals("count underflow", ex.getMessage());
        // The record has already been removed from the list (method performs remove before decCount)
        assertTrue(list.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_sectionsArrayTooShort_throwsArrayIndexOutOfBoundsException() throws Exception {
        // GIVEN – sections array length = 2 (valid indices 0,1), we will use index 3
        List<Record>[] shortArray = new List[2];
        setSectionsArray(shortArray);
        // WHEN / THEN
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.removeRecord(aRecord, 3));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] removeRecord null-section-list returns false")
    @CsvSource({ // record value is irrelevant because section list is null
    "mockRecordA,2,false", "mockRecordB,1,false" })
    void removeRecord_nullSectionList_cases(String recordName, int section, boolean expected) throws Exception {
        // Resolve a mock Record (the actual identity does not matter)
        Record mockRecord = mock(Record.class, recordName);
        // Ensure the required section list is null, others stay untouched
        List<Record>[] sections = new List[4];
        // leave sections[section] == null
        setSectionsArray(sections);
        boolean result = message.removeRecord(mockRecord, section);
        assertEquals(expected, result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_sectionListWithoutRecord_returnsFalseAndNoHeaderDec() throws Exception {
        // GIVEN – mutable list with a different mock record
        Record present = mock(Record.class, "presentRecord");
        Record absent = mock(Record.class, "absentRecord");
        List<Record> list = new ArrayList<>(Collections.singletonList(present));
        List<Record>[] sections = new List[4];
        sections[Section.ANSWER] = list;
        setSectionsArray(sections);
        Header mockHeader = mock(Header.class);
        setHeader(mockHeader);
        // WHEN
        boolean removed = message.removeRecord(absent, Section.ANSWER);
        // THEN
        assertFalse(removed);
        verify(mockHeader, never()).decCount(anyInt());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_sectionContainsRecord_returnsTrueAndCallsHeader() throws Exception {
        // GIVEN – mutable list with the record
        List<Record> list = new ArrayList<>(Collections.singletonList(aRecord));
        List<Record>[] sections = new List[4];
        sections[Section.ADDITIONAL] = list;
        setSectionsArray(sections);
        Header mockHeader = mock(Header.class);
        setHeader(mockHeader);
        // WHEN
        boolean removed = message.removeRecord(aRecord, Section.ADDITIONAL);
        // THEN
        assertTrue(removed);
        assertTrue(list.isEmpty());
        verify(mockHeader, times(1)).decCount(Section.ADDITIONAL);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeRecord_negativeSection_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.removeRecord(aRecord, -1));
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
 

api document of method removeRecord(Record, int): 

   * Removes a record from a section of the Message, and adjusts the header.
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
		org.xbill.DNS.Header org.xbill.DNS.Message.header;
	related functions:
		method `public void removeAllRecords(int)` returns `void`, related with `org.xbill.DNS.Message.removeRecord(Record, int)`, api document: 
   * Removes all records from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public boolean findRRset(Name, int, int)` returns `boolean`, related with `org.xbill.DNS.Message.removeRecord(Record, int)`, api document: 
   * Determines if an RRset with the given name and type is already present in the given section.
   *
   * @see RRset
   * @see Section
   ;
		method `public boolean findRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.removeRecord(Record, int)`, api document: 
   * Determines if the given record is already present in the given section.
   *
   * @see Record
   * @see Section
   ;
		method `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.removeRecord(Record, int)`, api document: 
   * Adds a record to a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `public List<RRset> getSectionRRsets(int)` returns `java.util.List<org.xbill.DNS.RRset>`, related with `org.xbill.DNS.Message.removeRecord(Record, int)`, api document: 
   * Returns an array containing all records in the given section grouped into RRsets.
   *
   * @see RRset
   * @see Section
   ;
		method `public void setHeader(Header)` returns `void`, related with `org.xbill.DNS.Message.removeRecord(Record, int)`, api document: 
   * Replaces the Header with a new one.
   *
   * @see Header
   ;
		method `public List<Record> getSection(int)` returns `java.util.List<org.xbill.DNS.Record>`, related with `org.xbill.DNS.Message.removeRecord(Record, int)`, api document: 
   * Returns all records in the given section, or an empty list if the section is empty.
   *
   * @see Record
   * @see Section
   ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 

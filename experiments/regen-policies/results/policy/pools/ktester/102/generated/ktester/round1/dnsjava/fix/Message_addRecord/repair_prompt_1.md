


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class Message_addRecord_Test {

    private Message message;

    private Header header;

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor which creates a fresh Header and empty sections.
        message = new Message();
        header = message.getHeader();
    }

    /**
     * Sets a private field on the given target object.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Helper that puts a specific List instance into {@code message.sections[section]}.
     */
    private static void setSectionList(Message msg, int section, List<Record> list) throws Exception {
        // The {@code sections} field is a private array of List<Record>.
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) sectionsField.get(msg);
        if (sections == null) {
            // initialise the array if the constructor didn't do it (unlikely)
            // The library does not expose SECTION_COUNT, use the known number of sections (4)
            sections = (List<Record>[]) new List[4];
            sectionsField.set(msg, sections);
        }
        sections[section] = list;
    }

    @Test
    void addRecordCreatesSectionAndIncrementsCount() throws UnknownHostException {
        // Given
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        int section = Section.ANSWER;
        // Pre‑condition checks
        assertEquals(0, header.getCount(section));
        assertTrue(message.getSection(section).isEmpty());
        // When
        message.addRecord(aRecord, section);
        // Then
        assertEquals(1, header.getCount(section), "Header count for the section should be incremented");
        assertEquals(1, message.getSection(section).size(), "Section should contain exactly one record");
        assertSame(aRecord, message.getSection(section).get(0), "The added record should be stored in the section");
        // also verify that the concrete implementation is a LinkedList (the default)
        assertTrue(message.getSection(section) instanceof java.util.LinkedList, "A newly created section should be a LinkedList");
    }

    @Test
    void addRecordAppendsWhenSectionAlreadyExists() throws UnknownHostException {
        // Given
        Name name = Name.fromConstantString("example.org.");
        Record first = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("10.0.0.1"));
        Record second = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("10.0.0.2"));
        int section = Section.AUTHORITY;
        // Add the first record
        message.addRecord(first, section);
        assertEquals(1, header.getCount(section));
        assertEquals(1, message.getSection(section).size());
        // When – add a second record to the same section
        message.addRecord(second, section);
        // Then
        assertEquals(2, header.getCount(section), "Header count should reflect two records");
        assertEquals(2, message.getSection(section).size(), "Section should contain two records");
        assertSame(first, message.getSection(section).get(0), "First record should stay at index 0");
        assertSame(second, message.getSection(section).get(1), "Second record should be appended at index 1");
    }

    @Test
    void addRecordInvalidSectionThrowsException() {
        // Given
        Record dummy = Record.newRecord(Name.fromConstantString("invalid.example."), Type.A, DClass.IN);
        // valid sections are 0‑3
        int invalidSection = 4;
        // When / Then
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.addRecord(dummy, invalidSection), "Adding a record to an invalid section index should throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void addRecordNullRecordThrowsNullPointerException() {
        Record nullRecord = null;
        int section = Section.QUESTION;
        assertThrows(NullPointerException.class, () -> message.addRecord(nullRecord, section), "Adding a null Record should result in NullPointerException");
    }

    @Test
    void addRecordWithNullHeaderThrowsNullPointerException() throws Exception {
        // Force the internal header to null via reflection.
        setPrivateField(message, "header", null);
        Name name = Name.fromConstantString("nullheader.example.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        int section = Section.AUTHORITY;
        assertThrows(NullPointerException.class, () -> message.addRecord(aRecord, section), "When Message.header is null, addRecord should throw NullPointerException");
    }

    @Test
    void addRecordWithNullSectionsArrayThrowsNullPointerException() throws Exception {
        // Force the internal sections array to null.
        setPrivateField(message, "sections", null);
        Name name = Name.fromConstantString("nullsections.example.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("192.0.2.55"));
        int section = Section.ANSWER;
        assertThrows(NullPointerException.class, () -> message.addRecord(aRecord, section), "When Message.sections is null, addRecord should throw NullPointerException");
    }

    @Test
    void addRecordUsesExistingArrayListImplementation() throws Exception {
        // Pre‑populate the ANSWER section with an ArrayList (not the default LinkedList).
        List<Record> arrayList = new ArrayList<>();
        setSectionList(message, Section.ANSWER, arrayList);
        Name name = Name.fromConstantString("arraylist.example.");
        Record aRecord = new ARecord(name, DClass.IN, 300, InetAddress.getByName("192.0.2.10"));
        int section = Section.ANSWER;
        // When
        message.addRecord(aRecord, section);
        // Then
        assertSame(arrayList, message.getSection(section), "The original ArrayList instance must be reused");
        assertEquals(1, arrayList.size(), "Record must be appended to the existing list");
        assertEquals(1, header.getCount(section), "Header count must be incremented");
    }

    @Test
    void addRecordToUnmodifiableListThrowsUnsupportedOperationException() throws Exception {
        // Prepare an unmodifiable list in the ANSWER section.
        List<Record> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        setSectionList(message, Section.ANSWER, unmodifiable);
        Name name = Name.fromConstantString("unmod.example.");
        Record aRecord = new ARecord(name, DClass.IN, 0, InetAddress.getByName("192.0.2.20"));
        int section = Section.ANSWER;
        assertThrows(UnsupportedOperationException.class, () -> message.addRecord(aRecord, section), "Adding to an unmodifiable list should raise UnsupportedOperationException");
    }

    @Test
    void addRecordToQuestionSectionWorksLikeOtherSections() throws UnknownHostException {
        Name name = Name.fromConstantString("lookup.example.");
        // question records have null rdata – disambiguate the constructor
        Record question = new ARecord(name, DClass.IN, 0, (InetAddress) null);
        int section = Section.QUESTION;
        assertEquals(0, header.getCount(section));
        assertTrue(message.getSection(section).isEmpty());
        message.addRecord(question, section);
        assertEquals(1, header.getCount(section), "Question count must be incremented");
        assertEquals(1, message.getSection(section).size());
        assertSame(question, message.getSection(section).get(0));
    }

    @Test
    void addRecordHeaderIncCountThrowsIllegalArgumentException() throws Exception {
        // Create a custom Header that throws IllegalArgumentException for SECTION_AUTHORITY.
        Header explodingHeader = new Header() {

            @Override
            public void incCount(int section) {
                if (section == Section.AUTHORITY) {
                    throw new IllegalArgumentException("bad section");
                }
                super.incCount(section);
            }
        };
        // Replace the Message's header with our custom one.
        message.setHeader(explodingHeader);
        Name name = Name.fromConstantString("badheader.example.");
        Record aRecord = new ARecord(name, DClass.IN, 0, InetAddress.getByName("192.0.2.99"));
        int section = Section.AUTHORITY;
        assertThrows(IllegalArgumentException.class, () -> message.addRecord(aRecord, section), "Header.incCount should propagate IllegalArgumentException");
    }

    @ParameterizedTest
    @CsvSource({ // negative index
    // out‑of‑range index (equal to the length of the internal sections array)
    "-1", "4" })
    void addRecordOutOfBoundsSectionThrowsException(int invalidSection) throws UnknownHostException {
        Record aRecord = new ARecord(Name.fromConstantString("outofbounds.example."), DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> message.addRecord(aRecord, invalidSection), "Invalid section index should cause ArrayIndexOutOfBoundsException");
    }

    @Test
    void addRecordMultipleTimesCumulativeHeaderCount() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        Record a1 = new ARecord(name, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        Record a2 = new AAAARecord(name, DClass.IN, 7200, InetAddress.getByName("2001:db8::1"));
        int section = Section.ANSWER;
        // first add
        message.addRecord(a1, section);
        assertEquals(1, header.getCount(section));
        assertEquals(1, message.getSection(section).size());
        // second add
        message.addRecord(a2, section);
        assertEquals(2, header.getCount(section));
        assertEquals(2, message.getSection(section).size());
        assertSame(a1, message.getSection(section).get(0));
        assertSame(a2, message.getSection(section).get(1));
    }

    @Test
    void addRecordMaximumValidSectionIndexBoundary() throws UnknownHostException {
        // SECTION_ADDITIONAL is the highest valid index (3)
        int section = Section.ADDITIONAL;
        Name name = Name.fromConstantString("boundary.example.");
        Record cname = new CNAMERecord(name, DClass.IN, 0, Name.fromConstantString("target.example."));
        assertEquals(0, header.getCount(section));
        assertTrue(message.getSection(section).isEmpty());
        message.addRecord(cname, section);
        assertEquals(1, header.getCount(section));
        assertEquals(1, message.getSection(section).size());
        assertSame(cname, message.getSection(section).get(0));
    }

    @Test
    void addRecordVerifiesHeaderIncCountInvocationWithMockito() {
        // Create a spy of the real Header so we can verify the interaction.
        Header spyHeader = spy(message.getHeader());
        message.setHeader(spyHeader);
        Name name = Name.fromConstantString("mockheader.example.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddress.getLoopbackAddress());
        int section = Section.ANSWER;
        message.addRecord(aRecord, section);
        // Verify incCount was called exactly once with the proper argument.
        verify(spyHeader, times(1)).incCount(section);
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
 

api document of method addRecord(Record, int): 

   * Adds a record to a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   

dependent classes: 
class `org.xbill.DNS.Record`:
	
class `org.xbill.DNS.Message`:
	dependent fields:
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
		org.xbill.DNS.Header org.xbill.DNS.Message.header;
	related functions:
		method ` void toWire(DNSOutput)` returns `void`, related with `org.xbill.DNS.Message.addRecord(Record, int)`;
		method `public List<RRset> getSectionRRsets(int)` returns `java.util.List<org.xbill.DNS.RRset>`, related with `org.xbill.DNS.Message.addRecord(Record, int)`, api document: 
   * Returns an array containing all records in the given section grouped into RRsets.
   *
   * @see RRset
   * @see Section
   ;
		method `public void setHeader(Header)` returns `void`, related with `org.xbill.DNS.Message.addRecord(Record, int)`, api document: 
   * Replaces the Header with a new one.
   *
   * @see Header
   ;
		method `public boolean removeRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.addRecord(Record, int)`, api document: 
   * Removes a record from a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
class `org.xbill.DNS.SVCBBase.ParameterIpv4Hint`:
	related functions:
		method `public List<Inet4Address> getAddresses()` returns `java.util.List<java.net.Inet4Address>`, related with `org.xbill.DNS.Message.addRecord(Record, int)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 

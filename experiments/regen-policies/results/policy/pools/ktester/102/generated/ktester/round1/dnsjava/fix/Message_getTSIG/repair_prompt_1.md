


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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class Message_getTSIG_Test {

    /**
     * When the message contains no records in the ADDITIONAL section,
     * {@code getTSIG()} must return {@code null}.
     */
    @Test
    void getTSIG_noAdditionalSection_returnsNull() {
        // empty message
        Message m = new Message();
        assertNull(m.getTSIG(), "TSIG should be null when no additional records exist");
    }

    /**
     * When the ADDITIONAL section has records but the last record is not a TSIG,
     * {@code getTSIG()} must return {@code null}.
     */
    @Test
    void getTSIG_nonTSIGLastRecord_returnsNull() throws IOException {
        Message m = new Message();
        // add a dummy A record to the ADDITIONAL section
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = Record.newRecord(name, Type.A, DClass.IN);
        m.addRecord(aRecord, Section.ADDITIONAL);
        // verify that the record really is present
        assertEquals(1, m.getHeader().getCount(Section.ADDITIONAL));
        assertEquals(Type.A, m.getSection(Section.ADDITIONAL).get(0).getType());
        // the last record is not a TSIG → should be null
        assertNull(m.getTSIG(), "TSIG should be null when last additional record is not a TSIG");
    }

    /**
     * When a TSIG record is added (as the last record of the ADDITIONAL section)
     * {@code getTSIG()} must return that {@link TSIGRecord}.
     */
    @Test
    void getTSIG_tsigPresent_returnsTSIGRecord() throws IOException {
        Message m = new Message();
        // create a TSIG key – the constructor matches the one provided by dnsjava
        Name keyName = Name.fromConstantString("mykey.");
        // TSIG(Name, String algorithm, String secret) is a valid constructor
        TSIG tsigKey = new TSIG(keyName, "hmac-md5", "secret");
        // add the TSIG to the message using the public API
        m.setTSIG(tsigKey);
        // sanity check – the header should now count one additional record
        assertEquals(1, m.getHeader().getCount(Section.ADDITIONAL));
        // the method under test should return the TSIGRecord we just added
        TSIGRecord tsigRecord = m.getTSIG();
        assertNotNull(tsigRecord, "TSIGRecord should be returned when TSIG is present");
        assertEquals(Type.TSIG, tsigRecord.getType(), "Returned record must be of type TSIG");
        // the key stored in the TSIGRecord must be the same key we supplied
        assertEquals(keyName, tsigRecord.getName(), "TSIGRecord name should match the key name");
    }

    /**
     * If the internal {@code header} field is {@code null}, {@code getTSIG()}
     * should throw a {@link NullPointerException}.
     */
    @Test
    void getTSIG_headerIsNull_throwsNPE() throws Exception {
        Message m = new Message();
        // Force the private header field to null
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(m, null);
        assertThrows(NullPointerException.class, m::getTSIG, "Calling getTSIG() with a null header must throw NPE");
    }

    /**
     * If the internal {@code sections} array itself is {@code null},
     * {@code getTSIG()} should throw a {@link NullPointerException}.
     */
    @Test
    void getTSIG_sectionsArrayIsNull_throwsNPE() throws Exception {
        Message m = new Message();
        // Set the private sections array to null
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, null);
        assertThrows(NullPointerException.class, m::getTSIG, "Calling getTSIG() with a null sections array must throw NPE");
    }

    /**
     * If the {@code sections} array is shorter than {@link Section#ADDITIONAL},
     * accessing {@code sections[ADDITIONAL]} should raise an
     * {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    void getTSIG_sectionsArrayTooShort_throwsAIOOBE() throws Exception {
        Message m = new Message();
        // Create an array that is deliberately too short (length = ADDITIONAL)
        // valid indices are 0..ADDITIONAL-1
        int tooShort = Section.ADDITIONAL;
        @SuppressWarnings("unchecked")
        List<Record>[] shortArray = (List<Record>[]) Array.newInstance(List.class, tooShort);
        // assign the short array
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, shortArray);
        // Header must report at least one additional record to trigger the access
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(m, mockHeader);
        assertThrows(ArrayIndexOutOfBoundsException.class, m::getTSIG, "Accessing a too‑short sections array must throw AIOOBE");
    }

    /**
     * When the header reports a non‑zero ADDITIONAL count that is larger than
     * the actual size of the list, {@code getTSIG()} should throw an
     * {@link IndexOutOfBoundsException}.
     */
    @Test
    void getTSIG_headerCountGreaterThanListSize_throwsIOOBE() throws Exception {
        Message m = new Message();
        // Prepare a proper sections array with an empty list for ADDITIONAL
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        // empty list
        sections[Section.ADDITIONAL] = new ArrayList<>();
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, sections);
        // Mock header to claim there is 1 additional record
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(m, mockHeader);
        assertThrows(IndexOutOfBoundsException.class, m::getTSIG, "Header count larger than list size must cause IOBE");
    }

    /**
     * If the last element of the ADDITIONAL list is {@code null},
     * {@code getTSIG()} should throw a {@link NullPointerException}
     * when it tries to read {@code rec.type}.
     */
    @Test
    void getTSIG_lastElementNull_throwsNPE() throws Exception {
        Message m = new Message();
        // List with a single null entry
        List<Record> list = new ArrayList<>();
        list.add(null);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, sections);
        // Header reports count = 1
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(m, mockHeader);
        assertThrows(NullPointerException.class, m::getTSIG, "Null last element must cause NPE when accessing its type field");
    }

    /**
     * If the last record has {@code type == Type.TSIG} but is not an instance of
     * {@link TSIGRecord}, the cast performed in {@code getTSIG()} should throw a
     * {@link ClassCastException}.
     */
    @Test
    void getTSIG_lastRecordNotTSIGRecord_throwsClassCastException() throws Exception {
        Message m = new Message();
        // Create a mock Record that is NOT a TSIGRecord but has type TSIG
        Record mockRecord = mock(Record.class);
        // The field {@code type} is public; set it via reflection
        Field typeField = Record.class.getDeclaredField("type");
        typeField.setAccessible(true);
        typeField.setInt(mockRecord, Type.TSIG);
        // Place the mock into the additional list
        List<Record> list = new ArrayList<>();
        list.add(mockRecord);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = (List<Record>[]) new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = list;
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(m, sections);
        // Header reports count = 1
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        Field headerField = Message.class.getDeclaredField("header");
        headerField.setAccessible(true);
        headerField.set(m, mockHeader);
        assertThrows(ClassCastException.class, m::getTSIG, "A non‑TSIGRecord with type TSIG must cause ClassCastException");
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
 

api document of method getTSIG(): 

   * Returns the TSIG record from the ADDITIONAL section, if one is present.
   *
   * @see TSIGRecord
   * @see TSIG
   * @see Section
   

return type: 
org.xbill.DNS.TSIGRecord

dependent classes: 
class `org.xbill.DNS.Record`:
	dependent fields:
		int org.xbill.DNS.Record.type;
	
class `org.xbill.DNS.Message`:
	dependent fields:
		java.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;
		org.xbill.DNS.Header org.xbill.DNS.Message.header;
	related functions:
		method `public Record getQuestion()` returns `org.xbill.DNS.Record`, related with `org.xbill.DNS.Message.getTSIG()`, api document: 
   * Returns the first record in the QUESTION section.
   *
   * @see Record
   * @see Section
   ;
		method `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.getTSIG()`, api document: 
   * Adds a record to a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;
		method `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.getTSIG()`;
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.ADDITIONAL;
	
class `org.xbill.DNS.Type`:
	dependent fields:
		int org.xbill.DNS.Type.TSIG;
	
class `org.xbill.DNS.SetResponse`:
	related functions:
		method `public RRset getNS()` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Message.getTSIG()`, api document:  If the query hit a delegation point, return the NS set. ;
class `org.xbill.DNS.ExtendedResolver`:
	related functions:
		method `public Resolver getResolver(int)` returns `org.xbill.DNS.Resolver`, related with `org.xbill.DNS.Message.getTSIG()`, api document:  Returns the nth resolver used by this ExtendedResolver ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 

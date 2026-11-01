


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;

@ExtendWith(MockitoExtension.class)
class Message_getHeader_Test {

    /**
     * Verify that the no‑arg constructor creates a non‑null Header and that the
     * Header’s ID is within the valid unsigned‑16‑bit range.
     */
    @Test
    void getHeader_noArgConstructor_returnsValidHeader() {
        // uses Header()
        Message msg = new Message();
        Header hdr = msg.getHeader();
        assertNotNull(hdr, "Header should not be null");
        int id = hdr.getID();
        assertTrue(id >= 0 && id <= 0xFFFF, "Header ID should be within unsigned 16‑bit range, but was " + id);
    }

    /**
     * Verify that the constructor taking an explicit ID creates a Header whose
     * ID matches the supplied value.
     */
    @Test
    void getHeader_intArgConstructor_matchesSuppliedId() {
        int expectedId = 0x1234;
        Message msg = new Message(expectedId);
        Header hdr = msg.getHeader();
        assertNotNull(hdr, "Header should not be null");
        assertEquals(expectedId, hdr.getID(), "Header ID should match the value supplied to the constructor");
    }

    /**
     * Verify that {@code setHeader(Header)} replaces the existing Header
     * instance and that {@code getHeader()} returns the newly set instance.
     */
    @Test
    void setHeader_replacesExistingHeader() {
        // starts with a random Header
        Message msg = new Message();
        Header original = msg.getHeader();
        Header replacement = new Header(0xABCD);
        msg.setHeader(replacement);
        Header after = msg.getHeader();
        assertSame(replacement, after, "After calling setHeader, getHeader should return the exact Header instance that was set");
        assertNotSame(original, after, "The Header instance should have been replaced, not mutated");
        assertEquals(0xABCD, after.getID(), "The new Header should retain the ID it was constructed with");
    }

    /**
     * Verify that a Message created from a {@link DNSInput} (i.e. from wire
     * data) correctly exposes the Header that was parsed from the input.
     *
     * The wire data is a minimal DNS query with ID 0x1a2b.  The test builds the
     * byte array manually to avoid any external resources.
     */
    @Test
    void getHeader_fromWireData_matchesParsedId() throws Exception {
        // Header fields (ID, flags, QDCOUNT, ANCOUNT, NSCOUNT, ARCOUNT)
        // ID = 0x1a2b, flags = 0x0100 (standard query), all counts = 0
        byte[] wire = { // ID
        0x1a, // ID
        0x2b, // flags
        0x01, // flags
        0x00, // QDCOUNT
        0x00, // QDCOUNT
        0x00, // ANCOUNT
        0x00, // ANCOUNT
        0x00, // NSCOUNT
        0x00, // NSCOUNT
        0x00, // ARCOUNT
        0x00, // ARCOUNT
        0x00 };
        Message msg = new Message(wire);
        Header hdr = msg.getHeader();
        assertNotNull(hdr, "Header should be parsed and not null");
        assertEquals(0x1a2b, hdr.getID(), "Header ID parsed from wire data should be 0x1a2b");
    }

    /**
     * {@code Message()} creates a message with no Header assigned.
     * {@link Message#getHeader()} is therefore expected to return {@code null}.
     */
    @Test
    void GetHeader_NoHeaderSet_returnsNull() {
        // default ctor
        Message msg = new Message();
        Header hdr = msg.getHeader();
        assertNull(hdr, "A newly created Message should have a null Header");
    }

    /**
     * After a non‑null {@link Header} is set via {@code setHeader},
     * {@code getHeader()} must return exactly that instance.
     */
    @Test
    void GetHeader_AfterSetHeader_returnsProvidedHeader() {
        Message msg = new Message();
        Header newHeader = new Header(0x1A2B);
        msg.setHeader(newHeader);
        Header obtained = msg.getHeader();
        assertNotNull(obtained, "Header should be non‑null after setHeader");
        assertSame(newHeader, obtained, "getHeader should return the same Header instance that was set");
    }

    /**
     * The Header returned by {@code getHeader()} must be the exact same object
     * supplied to {@code setHeader()}, confirming no defensive copy is made.
     */
    @Test
    void GetHeader_ReturnsSameInstance() {
        Message msg = new Message();
        Header hdr = new Header(0xDEAD);
        msg.setHeader(hdr);
        Header first = msg.getHeader();
        Header second = msg.getHeader();
        assertSame(first, second, "Multiple calls to getHeader should return the same Header reference");
        assertSame(hdr, first, "The Header instance returned must be the one that was set");
    }

    /**
     * Passing {@code null} to {@code setHeader} must clear the internal
     * Header reference so that subsequent {@code getHeader()} calls return
     * {@code null}.
     */
    @Test
    void GetHeader_SetNullHeader_resultsInNull() {
        Message msg = new Message();
        // Ensure we have a non‑null Header first
        msg.setHeader(new Header(0x1234));
        assertNotNull(msg.getHeader(), "Pre‑condition: Header should be non‑null");
        // Now clear it
        msg.setHeader(null);
        assertNull(msg.getHeader(), "After setHeader(null) the Header should be cleared (null)");
    }

    /**
     * Verify that the Header instance returned by {@code getHeader()} is live.
     * Changing a mutable property (e.g., the ID) on the retrieved Header must be
     * reflected by subsequent {@code getHeader()} calls.
     */
    @Test
    void GetHeader_MutabilityCheck_reflectsChanges() {
        Message msg = new Message();
        Header hdr = new Header(0x0000);
        msg.setHeader(hdr);
        Header retrieved = msg.getHeader();
        assertNotNull(retrieved, "Header should not be null before mutation");
        // mutate ID
        retrieved.setID(0xBEEF);
        // Retrieve again and verify the change persisted
        Header afterMutation = msg.getHeader();
        assertEquals(0xBEEF, afterMutation.getID(), "Mutating the Header should be visible through subsequent getHeader calls");
    }

    /**
     * The default constructor creates a Message with a non‑null Header.
     */
    @Test
    void GetHeader_DefaultConstructor_returnsNonNullHeader() {
        Message msg = new Message();
        Header hdr = msg.getHeader();
        assertNotNull(hdr, "A newly created Message should have a non‑null Header");
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
 

api document of method getHeader(): 

   * Retrieves the Header.
   *
   * @see Header
   

return type: 
org.xbill.DNS.Header

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		org.xbill.DNS.Header org.xbill.DNS.Message.header;
	related functions:
		method ` void toWire(DNSOutput)` returns `void`, related with `org.xbill.DNS.Message.getHeader()`;
		method `public void setHeader(Header)` returns `void`, related with `org.xbill.DNS.Message.getHeader()`, api document: 
   * Replaces the Header with a new one.
   *
   * @see Header
   ;
		method `public TSIGRecord getTSIG()` returns `org.xbill.DNS.TSIGRecord`, related with `org.xbill.DNS.Message.getHeader()`, api document: 
   * Returns the TSIG record from the ADDITIONAL section, if one is present.
   *
   * @see TSIGRecord
   * @see TSIG
   * @see Section
   ;
		method `public static Message newQuery(Record)` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.getHeader()`, api document: 
   * Creates a new Message with a random Message ID suitable for sending as a query.
   *
   * @param r A record containing the question
   ;
		method `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.getHeader()`, api document: 
   * Adds a record to a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 

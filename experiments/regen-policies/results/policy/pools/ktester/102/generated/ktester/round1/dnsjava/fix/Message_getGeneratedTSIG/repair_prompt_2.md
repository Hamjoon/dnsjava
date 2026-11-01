


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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Date;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class Message_getGeneratedTSIG_Test {

    private Message message;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message for each test case.
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        message = null;
    }

    /**
     * Helper that extracts the MAC byte array from a {@link TSIGRecord} via reflection.
     */
    private static byte[] macOf(TSIGRecord record) {
        try {
            Field macField = TSIGRecord.class.getDeclaredField("mac");
            macField.setAccessible(true);
            return (byte[]) macField.get(record);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to access MAC field of TSIGRecord", e);
        }
    }

    /**
     * By default a newly constructed {@link Message} does not contain a generated TSIG.
     */
    @Test
    void getGeneratedTSIG_withoutSettingTSIG_returnsNull() {
        assertNull(message.getGeneratedTSIG(), "A fresh Message should have no generated TSIG");
    }

    /**
     * After attaching a TSIG key with {@code setTSIG(TSIG)} the message should expose a
     * non‑null {@link TSIGRecord} via {@code getGeneratedTSIG()}.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIG_returnsRecord() throws IOException {
        // Prepare a simple TSIG key.
        Name keyName = Name.fromString("testkey.");
        Name algorithm = Name.fromString("hmac-md5.sig-alg.reg.int.");
        byte[] secret = "secret".getBytes();
        TSIG tsigKey = new TSIG(keyName, algorithm, secret);
        // Attach the TSIG to the message. This call populates the generatedTsig field.
        message.setTSIG(tsigKey);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should be non‑null after setTSIG");
        // Verify that the generated record carries the same key name.
        assertEquals(keyName, generated.getName(), "Generated TSIG record should use the TSIG key name");
    }

    /**
     * The overloaded {@code setTSIG(TSIG, int, TSIGRecord)} also results in a generated
     * TSIG record being available.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIGWithError_returnsRecord() throws IOException {
        Name keyName = Name.fromString("testkey2.");
        Name algorithm = Name.fromString("hmac-sha256.");
        byte[] secret = "anotherSecret".getBytes();
        TSIG tsigKey = new TSIG(keyName, algorithm, secret);
        // Use an error code of 0 (NOERROR) and no prior query TSIG.
        int error = 0;
        TSIGRecord queryTsig = null;
        // Call the overloaded method.
        message.setTSIG(tsigKey, error, queryTsig);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG should be non‑null after overloaded setTSIG");
        assertEquals(keyName, generated.getName(), "Generated TSIG record should reflect the TSIG key name");
    }

    /**
     * Supplying a {@code null} TSIG key must leave {@code generatedTsig} untouched
     * (i.e. still {@code null}).
     */
    @Test
    void getGeneratedTSIG_afterSetTSIG_nullKey_returnsNull() throws IOException {
        message.setTSIG((TSIG) null);
        assertNull(message.getGeneratedTSIG(), "Calling setTSIG(null) should not create a generated TSIG");
    }

    /**
     * Verify that the error field stored in the generated TSIG matches the error
     * code supplied to {@code setTSIG(TSIG, int, TSIGRecord)} when the query TSIG is {@code null}.
     */
    @ParameterizedTest
    @CsvSource({ // NOERROR
    // FORMERR (example)
    // NOTAUTH (example)
    "0", "1", "5" })
    void getGeneratedTSIG_afterSetTSIGWithError_nullQueryTsig_errorMatches(int errorCode) throws IOException {
        Name keyName = Name.fromString("errorkey.");
        Name algorithm = Name.fromString("hmac-sha256.");
        TSIG tsigKey = new TSIG(keyName, algorithm, "secret".getBytes());
        // queryTsig is null → MAC should be null in the generated record.
        message.setTSIG(tsigKey, errorCode, null);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG must be present");
        assertEquals(errorCode, generated.getError(), "Generated TSIG error field should match the supplied error code");
        assertNull(macOf(generated), "When queryTSIG is null the generated MAC should be null");
    }

    /**
     * When a non‑null query TSIG is supplied, the generated TSIG must copy its MAC
     * and store the supplied error code.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIGWithError_andQueryTsig_copiesMacAndError() throws IOException {
        Name keyName = Name.fromString("copykey.");
        Name algorithm = Name.fromString("hmac-sha256.");
        TSIG tsigKey = new TSIG(keyName, algorithm, "secret".getBytes());
        // Build a query TSIGRecord with a known MAC.
        byte[] mac = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        TSIGRecord queryTsig = new TSIGRecord(// name
        keyName, // dclass
        DClass.IN, // ttl (irrelevant for TSIG)
        0L, // algorithm
        algorithm, // time signed
        new Date(), // fudge
        0, // MAC
        mac, // original ID
        0, // error (NOERROR)
        0, // other data
        null);
        // arbitrary non‑zero error
        int errorCode = 9;
        message.setTSIG(tsigKey, errorCode, queryTsig);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG must be present");
        assertEquals(errorCode, generated.getError(), "Generated TSIG error field should match the supplied error code");
        assertArrayEquals(mac, macOf(generated), "Generated TSIG MAC must be copied from the query TSIG");
    }

    /**
     * Consecutive calls to {@code setTSIG} must overwrite the previous generated TSIG.
     */
    @Test
    void getGeneratedTSIG_afterConsecutiveSetTSIG_calls_lastOneWins() throws IOException {
        // First call – simple keyA
        Name keyA = Name.fromString("keyA.");
        Name algA = Name.fromString("hmac-md5.");
        TSIG tsigA = new TSIG(keyA, algA, "a".getBytes());
        message.setTSIG(tsigA);
        // Second call – keyB with error code 5 and null query TSIG
        Name keyB = Name.fromString("keyB.");
        Name algB = Name.fromString("hmac-sha256.");
        TSIG tsigB = new TSIG(keyB, algB, "b".getBytes());
        int error = 5;
        message.setTSIG(tsigB, error, null);
        TSIGRecord generated = message.getGeneratedTSIG();
        assertNotNull(generated, "Generated TSIG must reflect the second setTSIG call");
        assertEquals(keyB, generated.getName(), "Generated TSIG should use the name of the last key (keyB)");
        assertEquals(error, generated.getError(), "Generated TSIG error must match the error supplied in the last call");
        // MAC should be null because the last call used a null query TSIG.
        assertNull(macOf(generated), "Generated TSIG MAC should be null after the last call used a null query TSIG");
    }

    /**
     * After a successful {@code setTSIG} call, invoking {@code setTSIG(null)} must clear
     * the generated TSIG.
     */
    @Test
    void getGeneratedTSIG_afterSetTSIG_thenClear_returnsNull() throws IOException {
        Name keyName = Name.fromString("clearkey.");
        Name algorithm = Name.fromString("hmac-md5.");
        TSIG tsigKey = new TSIG(keyName, algorithm, "secret".getBytes());
        message.setTSIG(tsigKey);
        assertNotNull(message.getGeneratedTSIG(), "Precondition: generated TSIG must be present before clearing");
        // Clear the TSIG.
        message.setTSIG((TSIG) null);
        assertNull(message.getGeneratedTSIG(), "Generated TSIG should be cleared after setTSIG(null)");
    }

    /**
     * Cloning a signed message must preserve the generated TSIG record.
     */
    @Test
    void getGeneratedTSIG_afterCloningSignedMessage_preservesRecord() throws IOException, CloneNotSupportedException {
        Name keyName = Name.fromString("clonekey.");
        Name algorithm = Name.fromString("hmac-sha256.");
        TSIG tsigKey = new TSIG(keyName, algorithm, "cloneSecret".getBytes());
        int error = 0;
        message.setTSIG(tsigKey, error, null);
        TSIGRecord originalGenerated = message.getGeneratedTSIG();
        assertNotNull(originalGenerated, "Original message must have a generated TSIG before cloning");
        // Clone the message.
        Message cloned = (Message) message.clone();
        TSIGRecord clonedGenerated = cloned.getGeneratedTSIG();
        assertNotNull(clonedGenerated, "Cloned message should retain the generated TSIG");
        // Compare the essential fields.
        assertEquals(originalGenerated.getName(), clonedGenerated.getName(), "Cloned TSIG record name should match the original");
        assertEquals(originalGenerated.getAlgorithm(), clonedGenerated.getAlgorithm(), "Cloned TSIG algorithm should match the original");
        assertEquals(originalGenerated.getError(), clonedGenerated.getError(), "Cloned TSIG error code should match the original");
        assertArrayEquals(macOf(originalGenerated), macOf(clonedGenerated), "Cloned TSIG MAC should match the original");
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
 

api document of method getGeneratedTSIG(): 

   * Gets the generated {@link TSIGRecord}. Only valid if the messages has been converted to wire
   * format with {@link #toWire(int)} before.
   *
   * @return A generated TSIG record or {@code null}.
   

return type: 
org.xbill.DNS.TSIGRecord

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		org.xbill.DNS.TSIGRecord org.xbill.DNS.Message.generatedTsig;
	related functions:
		method ` TSIGRecord getGeneratedTSIG()` returns `org.xbill.DNS.TSIGRecord`, related with `org.xbill.DNS.Message.getGeneratedTSIG()`, api document: 
   * Gets the generated {@link TSIGRecord}. Only valid if the messages has been converted to wire
   * format with {@link #toWire(int)} before.
   *
   * @return A generated TSIG record or {@code null}.
   ;
		method `private boolean toWire(DNSOutput, int)` returns `boolean`, related with `org.xbill.DNS.Message.getGeneratedTSIG()`;
		method `public Message clone()` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.getGeneratedTSIG()`, api document: 
   * Creates a copy of this Message. This is done by the Resolver before adding TSIG and OPT
   * records, for example.
   *
   * @see Resolver
   * @see TSIGRecord
   * @see OPTRecord
   ;
class `org.xbill.DNS.ExtendedFlags`:
	related functions:
		method `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.getGeneratedTSIG()`, api document:  Converts a numeric extended flag into a String ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 

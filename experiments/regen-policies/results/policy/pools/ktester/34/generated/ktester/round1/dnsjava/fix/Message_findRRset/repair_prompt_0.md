
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.net.UnknownHostException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class Message_findRRset_Test {

    /**
     * Re‑usable name used in the tests.
     */
    private static Name TEST_NAME;

    /**
     * Message instance created for each test.
     */
    private Message message;

    @BeforeAll
    static void setupBeforeAll() throws TextParseException {
        // a simple absolute name – the library guarantees that this never throws for a valid DNS name
        TEST_NAME = Name.fromConstantString("example.com.");
    }

    @BeforeEach
    void setupBeforeEach() {
        // a fresh Message with an empty header for every test case
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up; the Message instance will be GC‑ed
        message = null;
    }

    /* --------------------------------------------------------------
     *  Helper methods for reflection manipulation of the private
     *  `sections` field.
     *  -------------------------------------------------------------- */
    private static List<Record>[] getSectionsArray(Message msg) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        //noinspection unchecked
        return (List<Record>[]) f.get(msg);
    }

    private static void setSectionsArray(Message msg, List<Record>[] arr) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(msg, arr);
    }

    private static void setSectionList(Message msg, int section, List<Record> list) throws Exception {
        List<Record>[] sections = getSectionsArray(msg);
        if (sections == null) {
            // initialise the array with the size defined by Section (0‑3)
            sections = new List[Section.SECTION_COUNT];
            setSectionsArray(msg, sections);
        }
        sections[section] = list;
    }

    /* --------------------------------------------------------------
     *  Valid scenarios – parameterised to keep the test suite concise.
     *  -------------------------------------------------------------- */
    private static Stream<Arguments> validScenarioProvider() throws Exception {
        // 1) null section (ANSWER) → false
        // no records added, sections[ANSWER] stays null
        Message m1 = new Message();
        // 2) empty AUTHORITY list → false
        Message m2 = new Message();
        setSectionList(m2, Section.AUTHORITY, new ArrayList<>());
        // 3) non‑matching record in ADDITIONAL → false
        Message m3 = new Message();
        Record other = Record.newRecord(Name.fromConstantString("other.net."), Type.MX, DClass.IN, 3600, new MXRecord(Name.fromConstantString("mail.other.net."), DClass.IN, 3600, 10));
        m3.addRecord(other, Section.ADDITIONAL);
        // 4) matching A record in ANSWER → true
        Message m4 = new Message();
        Record a = Record.newRecord(TEST_NAME, Type.A, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        m4.addRecord(a, Section.ANSWER);
        // 5) several records, matching TXT later → true
        Message m5 = new Message();
        Record a2 = Record.newRecord(Name.fromConstantString("example.org."), Type.A, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        Record txt = new TXTRecord(TEST_NAME, DClass.IN, 3600, "sample");
        m5.addRecord(a2, Section.ANSWER);
        m5.addRecord(txt, Section.ANSWER);
        return Stream.of(Arguments.of(m1, TEST_NAME, Type.A, Section.ANSWER, false, "null section (ANSWER) should yield false"), Arguments.of(m2, Name.fromConstantString("example.org."), Type.AAAA, Section.AUTHORITY, false, "empty AUTHORITY list should yield false"), Arguments.of(m3, Name.fromConstantString("example.net."), Type.MX, Section.ADDITIONAL, false, "non‑matching record in ADDITIONAL should yield false"), Arguments.of(m4, TEST_NAME, Type.A, Section.ANSWER, true, "matching A record in ANSWER should yield true"), Arguments.of(m5, TEST_NAME, Type.TXT, Section.ANSWER, true, "matching TXT record after other records should yield true"));
    }

    /**
     * Verify that {@code findRRset} returns {@code true} when a matching record
     * exists in the queried section.
     */
    @Test
    void findRRset_presentInSection_returnsTrue() throws UnknownHostException {
        // arrange – add an A record to the ANSWER section
        Record aRecord = Record.newRecord(TEST_NAME, Type.A, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        message.addRecord(aRecord, Section.ANSWER);
        // act
        boolean found = message.findRRset(TEST_NAME, Type.A, Section.ANSWER);
        // assert
        assertTrue(found, "RRset should be found in the ANSWER section");
    }

    /**
     * Verify that {@code findRRset} returns {@code false} when the section does not
     * contain a matching record, even though the name exists elsewhere.
     */
    @Test
    void findRRset_presentInOtherSection_returnsFalse() throws UnknownHostException {
        // arrange – add an A record to the AUTHORITY section only
        Record aRecord = Record.newRecord(TEST_NAME, Type.A, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
        message.addRecord(aRecord, Section.AUTHORITY);
        // act – query the ANSWER section, which is empty
        boolean found = message.findRRset(TEST_NAME, Type.A, Section.ANSWER);
        // assert
        assertFalse(found, "RRset should not be found in a section that does not contain it");
    }

    /**
     * Verify that {@code findRRset} returns {@code false} when the requested
     * section is empty (i.e., {@code sections[section] == null} internally).
     */
    @Test
    void findRRset_emptySection_returnsFalse() {
        // act – query a section that has never been initialised (e.g., SECTION.ADDITIONAL)
        boolean found = message.findRRset(TEST_NAME, Type.A, Section.ADDITIONAL);
        // assert
        assertFalse(found, "RRset should be false when the section is empty");
    }

    /**
     * Verify that an illegal section index causes {@link Section#check(int)} to
     * throw {@link IllegalArgumentException}.
     */
    @Test
    void findRRset_invalidSection_throwsIllegalArgumentException() {
        // arrange – an out‑of‑range section index (valid values are 0‑3)
        int invalidSection = 5;
        // act & assert
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(TEST_NAME, Type.A, invalidSection), "Calling findRRset with an invalid section should throw IllegalArgumentException");
    }

    /**
     * Verify the overloaded convenience method {@code findRRset(Name, int)} which
     * checks the ANSWER, AUTHORITY and ADDITIONAL sections.
     */
    @Test
    void findRRset_overloadedMethod_checksAllRelevantSections() throws UnknownHostException {
        // arrange – add a TXT record to the AUTHORITY section
        Record txtRecord = new TXTRecord(TEST_NAME, DClass.IN, 3600, "sample");
        message.addRecord(txtRecord, Section.AUTHORITY);
        // act – use the 2‑arg overload; it should locate the record in AUTHORITY
        boolean found = message.findRRset(TEST_NAME, Type.TXT);
        // assert
        assertTrue(found, "Overloaded findRRset should locate the TXT RRset in AUTHORITY");
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @MethodSource("validScenarioProvider")
    void findRRset_validScenarios(Message msg, Name name, int type, int section, boolean expected, String description) {
        boolean result = msg.findRRset(name, type, section);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Invalid input scenarios – each expects an exception.
     *  -------------------------------------------------------------- */
    @Test
    void findRRset_invalidType_negative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(TEST_NAME, -1, Section.ANSWER), "Negative type should trigger IllegalArgumentException");
    }

    @Test
    void findRRset_invalidType_tooLarge_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(TEST_NAME, 70000, Section.ANSWER), "Type larger than max should trigger IllegalArgumentException");
    }

    @Test
    void findRRset_invalidSection_negative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(TEST_NAME, Type.A, -1), "Negative section index should trigger IllegalArgumentException");
    }

    @Test
    void findRRset_invalidSection_tooLarge_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> message.findRRset(TEST_NAME, Type.A, 10), "Section index larger than allowed should trigger IllegalArgumentException");
    }

    @Test
    void findRRset_nullName_throwsNullPointerException() {
        Record a = Record.newRecord(TEST_NAME, Type.A, DClass.IN, 3600, InetAddress.getLoopbackAddress());
        message.addRecord(a, Section.ANSWER);
        assertThrows(NullPointerException.class, () -> message.findRRset(null, Type.A, Section.ANSWER), "Passing null Name should result in NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Additional exception scenarios that require reflection or mocks.
     *  -------------------------------------------------------------- */
    @Test
    void findRRset_internalSectionsArrayNull_throwsNullPointerException() throws Exception {
        // force the private `sections` field to null
        setSectionsArray(message, null);
        assertThrows(NullPointerException.class, () -> message.findRRset(TEST_NAME, Type.A, Section.ANSWER), "Null internal sections array should cause NullPointerException");
    }

    @Test
    void findRRset_recordWithNullName_throwsNullPointerException() throws Exception {
        // mock a Record whose getName() returns null
        Record mockRecord = mock(Record.class);
        when(mockRecord.getName()).thenReturn(null);
        when(mockRecord.getType()).thenReturn(Type.A);
        // place the mock into the ANSWER section
        setSectionList(message, Section.ANSWER, new ArrayList<>(Collections.singletonList(mockRecord)));
        assertThrows(NullPointerException.class, () -> message.findRRset(TEST_NAME, Type.A, Section.ANSWER), "Record with null name should cause NullPointerException");
    }

    @Test
    void findRRset_sectionContainsNullElement_throwsNullPointerException() throws Exception {
        // Initialise the ANSWER list with a null element
        setSectionList(message, Section.ANSWER, new ArrayList<>(Collections.singletonList(null)));
        assertThrows(NullPointerException.class, () -> message.findRRset(TEST_NAME, Type.A, Section.ANSWER), "Null element inside the section list should cause NullPointerException");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_findRRset_Test.java:70: error: cannot find symbol
            sections = new List[Section.SECTION_COUNT];
                                       ^
  symbol:   variable SECTION_COUNT
  location: class Section
src/test/java/org/xbill/DNS/Message_findRRset_Test.java:88: error: no suitable constructor found for MXRecord(Name,int,int,int)
        Record other = Record.newRecord(Name.fromConstantString("other.net."), Type.MX, DClass.IN, 3600, new MXRecord(Name.fromConstantString("mail.other.net."), DClass.IN, 3600, 10));
                                                                                                         ^
    constructor MXRecord.MXRecord() is not applicable
      (actual and formal argument lists differ in length)
    constructor MXRecord.MXRecord(Name,int,long,int,Name) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/org/xbill/DNS/Message_findRRset_Test.java:92: error: incompatible types: InetAddress cannot be converted to byte[]
        Record a = Record.newRecord(TEST_NAME, Type.A, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
                                                                                             ^
src/test/java/org/xbill/DNS/Message_findRRset_Test.java:96: error: incompatible types: InetAddress cannot be converted to byte[]
        Record a2 = Record.newRecord(Name.fromConstantString("example.org."), Type.A, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
                                                                                                                            ^
src/test/java/org/xbill/DNS/Message_findRRset_Test.java:110: error: incompatible types: InetAddress cannot be converted to byte[]
        Record aRecord = Record.newRecord(TEST_NAME, Type.A, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
                                                                                                   ^
src/test/java/org/xbill/DNS/Message_findRRset_Test.java:125: error: incompatible types: InetAddress cannot be converted to byte[]
        Record aRecord = Record.newRecord(TEST_NAME, Type.A, DClass.IN, 3600, InetAddress.getByName("127.0.0.1"));
                                                                                                   ^
src/test/java/org/xbill/DNS/Message_findRRset_Test.java:204: error: incompatible types: InetAddress cannot be converted to byte[]
        Record a = Record.newRecord(TEST_NAME, Type.A, DClass.IN, 3600, InetAddress.getLoopbackAddress());
                                                                                                      ^
Note: src/test/java/org/xbill/DNS/Message_findRRset_Test.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
7 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method findRRset(Name, int, int)': '\n   * Determines if an RRset with the given name and type is already present in the given section.\n   *\n   * @see RRset\n   * @see Section\n   ', 'return type': 'boolean', 'dependent classes': 'class `org.xbill.DNS.Name`:\n\t\nclass `org.xbill.DNS.Message`:\n\tdependent fields:\n\t\tjava.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;\n\trelated functions:\n\t\tmethod `public List<Record> getSection(int)` returns `java.util.List<org.xbill.DNS.Record>`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`, api document: \n   * Returns all records in the given section, or an empty list if the section is empty.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`, api document: \n   * Adds a record to a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public void removeAllRecords(int)` returns `void`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`, api document: \n   * Removes all records from a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`;\n\t\tmethod `public boolean findRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`, api document: \n   * Determines if the given record is already present in the given section.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public boolean removeRecord(Record, int)` returns `boolean`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`, api document: \n   * Removes a record from a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public List<RRset> getSectionRRsets(int)` returns `java.util.List<org.xbill.DNS.RRset>`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`, api document: \n   * Returns an array containing all records in the given section grouped into RRsets.\n   *\n   * @see RRset\n   * @see Section\n   ;\nclass `org.xbill.DNS.Zone`:\n\trelated functions:\n\t\tmethod `private void maybeAddRecord(Record)` returns `void`, related with `org.xbill.DNS.Message.findRRset(Name, int, int)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 



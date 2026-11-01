
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.mockito.MockedStatic;
import java.util.Date;
import java.io.IOException;

class Message_getTSIG_Test {

    /**
     * Simple {@link Record} subclass whose type can be set manually (used for
     * ClassCast tests). Only the abstract methods required by {@link Record}
     * are implemented with minimal stubs.
     */
    private static class PlainRecord extends Record {

        public PlainRecord(Name name, int type, int dclass, long ttl) {
            super(name, type, dclass, ttl);
        }

        @Override
        public Name getAdditionalName() {
            return null;
        }

        @Override
        public Record getObject() {
            return this;
        }

        @Override
        public String rrToString() {
            return "";
        }

        @Override
        public void rrToWire(DNSOutput out, Compression c, boolean canonical) {
            // no‑op for test purposes
        }

        @Override
        public void rdataFromString(Tokenizer st, Name origin) throws IOException {
            // no‑op for test purposes
        }

        @Override
        public void rrFromWire(DNSInput in) throws IOException {
            // no‑op for test purposes
        }
    }

    /**
     * Helper to create a simple TSIG key used for TSIGRecord construction.
     */
    private static TSIG createTestTSIG() {
        // Use the constructor that accepts (Name keyName, Name algorithm, byte[] secret)
        Name keyName = Name.fromString("test-key.");
        Name alg = Name.fromString("hmac-md5.");
        return new TSIG(keyName, alg, new byte[] { 0x00 });
    }

    /**
     * Helper to create a TSIGRecord that can be added to a Message.
     */
    private static TSIGRecord createTestTSIGRecord() {
        Name name = Name.fromConstantString("example.com.");
        Name alg = Name.fromString("hmac-md5.");
        Date now = new Date();
        int fudge = 0;
        byte[] mac = new byte[0];
        int originalID = 0;
        int error = 0;
        byte[] other = new byte[0];
        // The constructor expects an int ttl and an int originalID.
        return new TSIGRecord(name, DClass.IN, 0, alg, now, fudge, mac, originalID, error, other);
    }

    /**
     * Helper to create a generic ARecord for non‑TSIG tests.
     */
    private static ARecord createTestARecord() throws UnknownHostException {
        Name name = Name.fromConstantString("example.com.");
        return new ARecord(name, DClass.IN, 0, InetAddress.getByName("127.0.0.1"));
    }

    /**
     * Set a private field via reflection.
     */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void getTSIG_NoAdditionalSection_ReturnsNull() {
        // empty message, no records
        Message msg = new Message();
        assertNull(msg.getTSIG(), "TSIG should be null when ADDITIONAL count is 0");
    }

    @Test
    void getTSIG_LastAdditionalNotTSIG_ReturnsNull() throws UnknownHostException {
        Message msg = new Message();
        // Add a normal A record to the ADDITIONAL section.
        msg.addRecord(createTestARecord(), Section.ADDITIONAL);
        // Header count for ADDITIONAL is now 1, but the record is not TSIG.
        assertEquals(1, msg.getHeader().getCount(Section.ADDITIONAL));
        assertNull(msg.getTSIG(), "TSIG should be null when last ADDITIONAL record is not TSIG");
    }

    @Test
    void getTSIG_LastAdditionalIsTSIG_ReturnsRecord() {
        Message msg = new Message();
        // First add a normal record (optional, to ensure TSIG is truly the last one).
        msg.addRecord(createTestARecord(), Section.ADDITIONAL);
        // Then add the TSIG record as the final additional record.
        TSIGRecord tsigRec = createTestTSIGRecord();
        msg.addRecord(tsigRec, Section.ADDITIONAL);
        // Verify header count reflects both records.
        assertEquals(2, msg.getHeader().getCount(Section.ADDITIONAL));
        TSIGRecord result = msg.getTSIG();
        assertNotNull(result, "TSIG should be returned when the last ADDITIONAL record is TSIG");
        assertSame(tsigRec, result, "Returned TSIGRecord should be the one that was added");
    }

    @Test
    @DisplayName("Multiple additional records, TSIG not last -> returns null")
    void getTSIG_TSIGNotLast_ReturnsNull() throws Exception {
        Message msg = new Message();
        // A, TSIG, another A – TSIG is not the last record.
        msg.addRecord(createTestARecord(), Section.ADDITIONAL);
        msg.addRecord(createTestTSIGRecord(), Section.ADDITIONAL);
        msg.addRecord(createTestARecord(), Section.ADDITIONAL);
        assertEquals(3, msg.getHeader().getCount(Section.ADDITIONAL));
        assertNull(msg.getTSIG(), "TSIG should be null because the last additional record is not TSIG");
    }

    @Test
    @DisplayName("Header count > actual list size -> IndexOutOfBoundsException")
    void getTSIG_HeaderCountGreaterThanListSize_ThrowsIndexOutOfBounds() throws Exception {
        Message msg = new Message();
        // Prepare a sections array with a single (non‑empty) list of size 1.
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.ADDITIONAL + 1];
        List<Record> additional = new ArrayList<>();
        additional.add(createTestARecord());
        sections[Section.ADDITIONAL] = additional;
        setField(msg, "sections", sections);
        // Mock Header to return count 2 (greater than list size 1).
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(2);
        setField(msg, "header", mockHeader);
        assertThrows(IndexOutOfBoundsException.class, msg::getTSIG, "Accessing index count‑1 should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("Header count zero but internal list non‑empty -> returns null")
    void getTSIG_HeaderCountZeroListNonEmpty_ReturnsNull() throws Exception {
        Message msg = new Message();
        // Internal list contains a record.
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.ADDITIONAL + 1];
        List<Record> additional = new ArrayList<>();
        additional.add(createTestARecord());
        sections[Section.ADDITIONAL] = additional;
        setField(msg, "sections", sections);
        // Header reports zero.
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(0);
        setField(msg, "header", mockHeader);
        assertNull(msg.getTSIG(), "Method should ignore the list and return null when header count is 0");
    }

    @Test
    @DisplayName("Header count smaller than list size, TSIG at counted index -> returns that TSIG")
    void getTSIG_CountSmallerThanList_TSIGAtCountedIndex_ReturnsTSIG() throws Exception {
        Message msg = new Message();
        // Build a list of three records: A, TSIG, A.
        List<Record> additional = new ArrayList<>();
        // index 0
        additional.add(createTestARecord());
        // index 1 (count‑1)
        TSIGRecord tsig = createTestTSIGRecord();
        additional.add(tsig);
        // index 2
        additional.add(createTestARecord());
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = additional;
        setField(msg, "sections", sections);
        // Header reports count = 2 (so we look at index 1).
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(2);
        setField(msg, "header", mockHeader);
        TSIGRecord result = msg.getTSIG();
        assertNotNull(result, "TSIG should be returned from the counted position");
        assertSame(tsig, result, "Returned TSIGRecord must be the one at index count‑1");
    }

    @Test
    @DisplayName("Last element in list is null -> NullPointerException")
    void getTSIG_LastElementNull_ThrowsNPE() throws Exception {
        Message msg = new Message();
        List<Record> additional = new ArrayList<>();
        // only element is null
        additional.add(null);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = additional;
        setField(msg, "sections", sections);
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        setField(msg, "header", mockHeader);
        assertThrows(NullPointerException.class, msg::getTSIG, "Accessing rec.type on a null record should throw NPE");
    }

    @Test
    @DisplayName("Record type TSIG but not a TSIGRecord instance -> ClassCastException")
    void getTSIG_TypeTSIGButNotTSIGRecord_ThrowsClassCast() throws Exception {
        Message msg = new Message();
        // Create a plain Record whose type field is set to Type.TSIG.
        PlainRecord plain = new PlainRecord(Name.fromConstantString("example.com."), Type.TSIG, DClass.IN, 0);
        List<Record> additional = new ArrayList<>();
        additional.add(plain);
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = additional;
        setField(msg, "sections", sections);
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        setField(msg, "header", mockHeader);
        assertThrows(ClassCastException.class, msg::getTSIG, "Casting a non‑TSIGRecord with type TSIG should throw ClassCastException");
    }

    @Test
    @DisplayName("Header is null -> NullPointerException")
    void getTSIG_HeaderNull_ThrowsNPE() throws Exception {
        Message msg = new Message();
        // Ensure sections array is non‑null to reach the header call.
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = new ArrayList<>();
        setField(msg, "sections", sections);
        // Set header to null.
        setField(msg, "header", null);
        assertThrows(NullPointerException.class, msg::getTSIG, "Calling header.getCount on a null header should throw NPE");
    }

    @Test
    @DisplayName("Sections array is null -> NullPointerException")
    void getTSIG_SectionsArrayNull_ThrowsNPE() throws Exception {
        Message msg = new Message();
        // Header with a positive count to force access to sections.
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        setField(msg, "header", mockHeader);
        // Set sections field to null.
        setField(msg, "sections", null);
        assertThrows(NullPointerException.class, msg::getTSIG, "Accessing a null sections array should throw NPE");
    }

    @Test
    @DisplayName("Sections array too short -> ArrayIndexOutOfBoundsException")
    void getTSIG_SectionsArrayTooShort_ThrowsAIOOBE() throws Exception {
        Message msg = new Message();
        // Create an array of length 1 (Section.ADDITIONAL is 3 in dnsjava).
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[1];
        setField(msg, "sections", sections);
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(0);
        setField(msg, "header", mockHeader);
        assertThrows(ArrayIndexOutOfBoundsException.class, msg::getTSIG, "Accessing sections[Section.ADDITIONAL] with a short array should throw AIOOBE");
    }

    @Test
    @DisplayName("Additional section list is null -> NullPointerException")
    void getTSIG_AdditionalListNull_ThrowsNPE() throws Exception {
        Message msg = new Message();
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.ADDITIONAL + 1];
        // explicit null entry
        sections[Section.ADDITIONAL] = null;
        setField(msg, "sections", sections);
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(1);
        setField(msg, "header", mockHeader);
        assertThrows(NullPointerException.class, msg::getTSIG, "Accessing a null List for the ADDITIONAL section should throw NPE");
    }

    @Test
    @DisplayName("Header reports negative count -> IndexOutOfBoundsException")
    void getTSIG_NegativeHeaderCount_ThrowsIOOBE() throws Exception {
        Message msg = new Message();
        List<Record> additional = new ArrayList<>();
        additional.add(createTestARecord());
        @SuppressWarnings("unchecked")
        List<Record>[] sections = new List[Section.ADDITIONAL + 1];
        sections[Section.ADDITIONAL] = additional;
        setField(msg, "sections", sections);
        Header mockHeader = mock(Header.class);
        when(mockHeader.getCount(Section.ADDITIONAL)).thenReturn(-2);
        setField(msg, "header", mockHeader);
        assertThrows(IndexOutOfBoundsException.class, msg::getTSIG, "Negative count should lead to an invalid list index and throw IOOBE");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_getTSIG_Test.java:33: error: method does not override or implement a method from a supertype
        @Override
        ^
Note: src/test/java/org/xbill/DNS/Message_getTSIG_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
1 error

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method getTSIG()': '\n   * Returns the TSIG record from the ADDITIONAL section, if one is present.\n   *\n   * @see TSIGRecord\n   * @see TSIG\n   * @see Section\n   ', 'return type': 'org.xbill.DNS.TSIGRecord', 'dependent classes': 'class `org.xbill.DNS.Record`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Record.type;\n\t\nclass `org.xbill.DNS.Message`:\n\tdependent fields:\n\t\torg.xbill.DNS.Header org.xbill.DNS.Message.header;\n\t\tjava.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;\n\trelated functions:\n\t\tmethod `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.getTSIG()`, api document: \n   * Adds a record to a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public Record getQuestion()` returns `org.xbill.DNS.Record`, related with `org.xbill.DNS.Message.getTSIG()`, api document: \n   * Returns the first record in the QUESTION section.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.getTSIG()`;\nclass `org.xbill.DNS.Section`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Section.ADDITIONAL;\n\t\nclass `org.xbill.DNS.Type`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Type.TSIG;\n\t\nclass `org.xbill.DNS.SetResponse`:\n\trelated functions:\n\t\tmethod `public RRset getNS()` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Message.getTSIG()`, api document:  If the query hit a delegation point, return the NS set. ;\nclass `org.xbill.DNS.ExtendedResolver`:\n\trelated functions:\n\t\tmethod `public Resolver getResolver(int)` returns `org.xbill.DNS.Resolver`, related with `org.xbill.DNS.Message.getTSIG()`, api document:  Returns the nth resolver used by this ExtendedResolver ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 



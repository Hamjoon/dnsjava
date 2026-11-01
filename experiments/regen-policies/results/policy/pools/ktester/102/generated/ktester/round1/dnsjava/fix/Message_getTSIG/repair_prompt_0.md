
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
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
        TSIG tsigKey = new TSIG(keyName, "hmac-md5", "secret".getBytes());
        // add the TSIG to the message using the public API
        m.setTSIG(tsigKey);
        // sanity check – the header should now count one additional record
        assertEquals(1, m.getHeader().getCount(Section.ADDITIONAL));
        // the method under test should return the TSIGRecord we just added
        TSIGRecord tsigRecord = m.getTSIG();
        assertNotNull(tsigRecord, "TSIGRecord should be returned when TSIG is present");
        assertEquals(Type.TSIG, tsigRecord.getType(), "Returned record must be of type TSIG");
        // the key stored in the TSIGRecord must be the same key we supplied
        assertEquals(tsigKey.getName(), tsigRecord.getName(), "TSIGRecord name should match the key name");
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

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_getTSIG_Test.java:60: error: no suitable constructor found for TSIG(Name,String,byte[])
        TSIG tsigKey = new TSIG(keyName, "hmac-md5", "secret".getBytes());
                       ^
    constructor TSIG.TSIG(Name,Name,String) is not applicable
      (argument mismatch; String cannot be converted to Name)
    constructor TSIG.TSIG(Name,Name,byte[]) is not applicable
      (argument mismatch; String cannot be converted to Name)
    constructor TSIG.TSIG(Name,Name,SecretKey) is not applicable
      (argument mismatch; String cannot be converted to Name)
    constructor TSIG.TSIG(Name,String,String) is not applicable
      (argument mismatch; byte[] cannot be converted to String)
    constructor TSIG.TSIG(String,String,String) is not applicable
      (argument mismatch; Name cannot be converted to String)
src/test/java/org/xbill/DNS/Message_getTSIG_Test.java:70: error: cannot find symbol
        assertEquals(tsigKey.getName(), tsigRecord.getName(), "TSIGRecord name should match the key name");
                            ^
  symbol:   method getName()
  location: variable tsigKey of type TSIG
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method getTSIG()': '\n   * Returns the TSIG record from the ADDITIONAL section, if one is present.\n   *\n   * @see TSIGRecord\n   * @see TSIG\n   * @see Section\n   ', 'return type': 'org.xbill.DNS.TSIGRecord', 'dependent classes': 'class `org.xbill.DNS.Record`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Record.type;\n\t\nclass `org.xbill.DNS.Message`:\n\tdependent fields:\n\t\tjava.util.List<org.xbill.DNS.Record>[] org.xbill.DNS.Message.sections;\n\t\torg.xbill.DNS.Header org.xbill.DNS.Message.header;\n\trelated functions:\n\t\tmethod `public Record getQuestion()` returns `org.xbill.DNS.Record`, related with `org.xbill.DNS.Message.getTSIG()`, api document: \n   * Returns the first record in the QUESTION section.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `public void addRecord(Record, int)` returns `void`, related with `org.xbill.DNS.Message.getTSIG()`, api document: \n   * Adds a record to a section of the Message, and adjusts the header.\n   *\n   * @see Record\n   * @see Section\n   ;\n\t\tmethod `private int sectionToWire(DNSOutput, int, Compression, int)` returns `int`, related with `org.xbill.DNS.Message.getTSIG()`;\nclass `org.xbill.DNS.Section`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Section.ADDITIONAL;\n\t\nclass `org.xbill.DNS.Type`:\n\tdependent fields:\n\t\tint org.xbill.DNS.Type.TSIG;\n\t\nclass `org.xbill.DNS.SetResponse`:\n\trelated functions:\n\t\tmethod `public RRset getNS()` returns `org.xbill.DNS.RRset`, related with `org.xbill.DNS.Message.getTSIG()`, api document:  If the query hit a delegation point, return the NS set. ;\nclass `org.xbill.DNS.ExtendedResolver`:\n\trelated functions:\n\t\tmethod `public Resolver getResolver(int)` returns `org.xbill.DNS.Resolver`, related with `org.xbill.DNS.Message.getTSIG()`, api document:  Returns the nth resolver used by this ExtendedResolver ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 



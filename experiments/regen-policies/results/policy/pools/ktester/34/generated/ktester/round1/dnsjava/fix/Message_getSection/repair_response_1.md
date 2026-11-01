```java
package org.xbill.DNS;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Message#getSection(int)}.
 *
 * The test class does not implement any of the {@code Message} functionality;
 * it simply exercises the real {@code Message} implementation provided by the
 * dnsjava library.  Helper classes and static imports are added so that the
 * code compiles and the tests can run.
 */
@ExtendWith(MockitoExtension.class)
class Message_getSection_Test {

    /**
     * Helper class to provide a deterministic loopback IPv4 address without
     * triggering checked exceptions.
     */
    private static final class InetAddressLoopback {

        static final java.net.InetAddress IPV4;

        static {
            try {
                IPV4 = java.net.InetAddress.getByName("127.0.0.1");
            } catch (java.net.UnknownHostException e) {
                throw new AssertionError("Unable to resolve loopback address", e);
            }
        }
    }

    /**
     * Verify that a newly constructed Message returns empty, immutable lists for all sections.
     */
    @Test
    void getSection_emptyMessage_returnsEmptyImmutableLists() {
        // uses the no‑arg constructor
        Message msg = new Message();
        for (int sec = 0; sec < 4; sec++) {
            List<Record> section = msg.getSection(sec);
            // The list must be empty
            assertTrue(section.isEmpty(), "Section " + sec + " should be empty");
            // The list must be immutable – any attempt to modify it should throw
            assertThrows(UnsupportedOperationException.class,
                    () -> section.add(null),
                    "Section list should be unmodifiable");
        }
        // Invalid section numbers must cause IllegalArgumentException (Section.check)
        assertThrows(IllegalArgumentException.class, () -> msg.getSection(-1));
        assertThrows(IllegalArgumentException.class, () -> msg.getSection(4));
    }

    /**
     * Verify that a populated section returns an immutable view containing the added records.
     */
    @Test
    void getSection_populatedSection_returnsImmutableView() {
        Message msg = new Message();
        Name name = Name.fromConstantString("example.com.");
        Record aRecord = new ARecord(name, DClass.IN, 3600, InetAddressLoopback.IPV4);
        Record txtRecord = new TXTRecord(name, DClass.IN, 3600, "sample text");
        // Populate the ANSWER section (Section.ANSWER == 1)
        msg.addRecord(aRecord, Section.ANSWER);
        msg.addRecord(txtRecord, Section.ANSWER);
        List<Record> answerSection = msg.getSection(Section.ANSWER);
        // The view must contain exactly the two records in insertion order
        assertEquals(2, answerSection.size());
        assertEquals(aRecord, answerSection.get(0));
        assertEquals(txtRecord, answerSection.get(1));
        // The view must be immutable
        assertThrows(UnsupportedOperationException.class,
                () -> answerSection.remove(0));
    }

    /**
     * Verify that the list returned by {@code getSection} reflects later modifications
     * to the underlying Message (the view is live).
     */
    @Test
    void getSection_liveViewReflectsSubsequentAdds() {
        Message msg = new Message();
        Name name = Name.fromConstantString("example.org.");
        Record mxRecord = new MXRecord(name, DClass.IN, 300, 10,
                Name.fromConstantString("mail.example.org."));
        Record nsRecord = new NSRecord(name, DClass.IN, 300,
                Name.fromConstantString("ns1.example.org."));
        // Initially empty
        List<Record> additional = msg.getSection(Section.ADDITIONAL);
        assertTrue(additional.isEmpty());
        // Add a record after obtaining the view
        msg.addRecord(mxRecord, Section.ADDITIONAL);
        assertEquals(1, additional.size());
        assertEquals(mxRecord, additional.get(0));
        // Add another record
        msg.addRecord(nsRecord, Section.ADDITIONAL);
        assertEquals(2, additional.size());
        assertEquals(nsRecord, additional.get(1));
    }

    /**
     * Verify that {@code getSection} returns an empty list (not null) when the internal
     * array slot is {@code null}. This is exercised via the constructor that parses a
     * DNS wire format where a particular section count is zero.
     */
    @Test
    void getSection_sectionCountZero_returnsEmptyList() throws IOException {
        // Build a minimal DNS query (header only, no records)
        Message query = new Message();
        // produces a valid DNS message with zero counts
        byte[] wire = query.toWire();
        // uses the byte[] constructor
        Message parsed = new Message(wire);
        // All sections should be empty but not null
        for (int sec = 0; sec < 4; sec++) {
            List<Record> secList = parsed.getSection(sec);
            assertNotNull(secList);
            assertTrue(secList.isEmpty());
            assertEquals(Collections.emptyList(), secList);
        }
    }

    /**
     * After {@link Message#removeAllRecords(int)} the internal list for the
     * section becomes an empty mutable list (not {@code null}).  The
     * {@code getSection} method must therefore return an immutable empty list
     * that is *different* from the shared {@link Collections#emptyList()}
     * instance.
     */
    @Test
    void getSection_afterRemoveAllRecords_returnsDistinctEmptyImmutableList() {
        Message msg = new Message();
        Name name = Name.fromConstantString("example.net.");
        Record a = new ARecord(name, DClass.IN, 300, InetAddressLoopback.IPV4);
        Record txt = new TXTRecord(name, DClass.IN, 300, "test");
        // Populate SECTION 2 (Authority) and then clear it
        msg.addRecord(a, Section.AUTHORITY);
        msg.addRecord(txt, Section.AUTHORITY);
        assertFalse(msg.getSection(Section.AUTHORITY).isEmpty());
        // Remove all records from that section
        msg.removeAllRecords(Section.AUTHORITY);
        List<Record> authority = msg.getSection(Section.AUTHORITY);
        assertTrue(authority.isEmpty(), "Section should be empty after removal");
        // Must be immutable
        assertThrows(UnsupportedOperationException.class,
                () -> authority.add(a));
        // Must not be the shared empty list instance
        assertNotSame(Collections.emptyList(), authority,
                "Empty list after removal should be a distinct immutable view");
    }

    /**
     * Adding multiple records to a section must preserve the insertion order.
     */
    @Test
    void getSection_multipleRecords_preservesInsertionOrder() {
        Message msg = new Message();
        Name name = Name.fromConstantString("order.test.");
        Record r1 = new ARecord(name, DClass.IN, 300, InetAddressLoopback.IPV4);
        Record r2 = new TXTRecord(name, DClass.IN, 300, "first");
        Record r3 = new TXTRecord(name, DClass.IN, 300, "second");
        // Add records in a specific order
        msg.addRecord(r1, Section.ANSWER);
        msg.addRecord(r2, Section.ANSWER);
        msg.addRecord(r3, Section.ANSWER);
        List<Record> answer = msg.getSection(Section.ANSWER);
        assertEquals(3, answer.size(), "Exactly three records expected");
        assertEquals(r1, answer.get(0), "First record should be r1");
        assertEquals(r2, answer.get(1), "Second record should be r2");
        assertEquals(r3, answer.get(2), "Third record should be r3");
    }

    /**
     * Simulate a corrupted {@link Message} where the internal {@code sections}
     * array is {@code null}.  Accessing any section should result in a
     * {@link NullPointerException}.
     */
    @Test
    void getSection_nullInternalSectionsArray_throwsNPE() throws Exception {
        Message msg = new Message();
        // Use reflection to set the private 'sections' field to null
        Field sectionsField = Message.class.getDeclaredField("sections");
        sectionsField.setAccessible(true);
        sectionsField.set(msg, null);
        // Any call to getSection should now throw NPE
        assertThrows(NullPointerException.class, () -> msg.getSection(0));
    }
}
```
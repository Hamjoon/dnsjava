```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Message#getSection(int)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>returning an empty, immutable list for sections that have never been populated;
 *   <li>returning an immutable view of a populated section;
 *   <li>the view reflecting further modifications to the underlying {@link Message};
 *   <li>validation of the {@code section} argument (out‑of‑range values).
 * </ul>
 *
 * <p>No private members of {@link Message} are accessed; the tests interact only through the
 * public API.
 */
@ExtendWith(MockitoExtension.class)
class Message_getSection_Test {

    /** No global resources are required for the whole test suite. */
    @BeforeAll
    static void setupBeforeAll() {
        // nothing to initialise globally
    }

    /** Create a fresh {@link Message} before each test. */
    @BeforeEach
    void setupBeforeEach() {
        // nothing to initialise per‑test; each test creates its own Message instance
    }

    /** No per‑test cleanup needed. */
    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up
    }

    /** No global cleanup needed. */
    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up globally
    }

    /** Verify that a newly constructed Message returns empty, immutable lists for all sections. */
    @Test
    void getSection_emptyMessage_returnsEmptyImmutableLists() {
        Message msg = new Message(); // uses the no‑arg constructor

        for (int sec = 0; sec < 4; sec++) {
            List<Record> section = msg.getSection(sec);
            // The list must be empty
            assertTrue(section.isEmpty(), "Section " + sec + " should be empty");
            // The list must be immutable – any attempt to modify it should throw
            assertThrows(UnsupportedOperationException.class, () -> section.add(null),
                    "Section list should be unmodifiable");
        }

        // Invalid section numbers must cause IllegalArgumentException (Section.check)
        assertThrows(IllegalArgumentException.class, () -> msg.getSection(-1));
        assertThrows(IllegalArgumentException.class, () -> msg.getSection(4));
    }

    /** Verify that a populated section returns an immutable view containing the added records. */
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
        assertThrows(UnsupportedOperationException.class, () -> answerSection.remove(0));
    }

    /** Verify that the list returned by {@code getSection} reflects later modifications
     *  to the underlying Message (the view is live). */
    @Test
    void getSection_liveViewReflectsSubsequentAdds() {
        Message msg = new Message();

        Name name = Name.fromConstantString("example.org.");
        Record mxRecord = new MXRecord(name, DClass.IN, 300, 10, Name.fromConstantString("mail.example.org."));
        Record nsRecord = new NSRecord(name, DClass.IN, 300, Name.fromConstantString("ns1.example.org."));

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

    /** Verify that {@code getSection} returns an empty list (not null) when the internal
     *  array slot is {@code null}. This is exercised via the constructor that parses a
     *  DNS wire format where a particular section count is zero. */
    @Test
    void getSection_sectionCountZero_returnsEmptyList() throws IOException {
        // Build a minimal DNS query (header only, no records)
        Message query = new Message();
        byte[] wire = query.toWire(); // produces a valid DNS message with zero counts
        Message parsed = new Message(wire); // uses the byte[] constructor

        // All sections should be empty but not null
        for (int sec = 0; sec < 4; sec++) {
            List<Record> secList = parsed.getSection(sec);
            assertNotNull(secList);
            assertTrue(secList.isEmpty());
            assertEquals(Collections.emptyList(), secList);
        }
    }

    /** Helper class to provide a deterministic loopback IPv4 address without triggering
     *  checked exceptions. */
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
}
```
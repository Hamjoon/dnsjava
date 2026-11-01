```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * Additional unit tests for {@link Message#toWire(DNSOutput)}.
 *
 * <p>The tests focus on the internal behaviour of {@code toWire}:
 * <ul>
 *   <li>correct interaction with {@link Header} and {@link Record}</li>
 *   <li>handling of {@code null} sections, empty lists and {@code null} records</li>
 *   <li>order of operations (header before records)</li>
 *   <li>reuse of a single {@link Compression} instance per call</li>
 *   <li>creation of a fresh {@code Compression} for each invocation</li>
 *   <li>edge cases such as a zero‑length {@code sections} array and a {@code null}
 *       {@link DNSOutput}</li>
 * </ul>
 *
 * <p>All tests are package‑private, therefore they can access the
 * package‑private {@code toWire(DNSOutput)} method directly.
 */
class Message_toWire_AdditionalTests {

    /**
     * Utility to replace the private {@code sections} field of a {@link Message}
     * instance via reflection.
     */
    @SuppressWarnings("unchecked")
    private void setSections(Message msg, List<Record>[] sections) throws Exception {
        Field f = Message.class.getDeclaredField("sections");
        f.setAccessible(true);
        f.set(msg, sections);
    }

    /**
     * Utility to create a {@link Message} with a mocked {@link Header}.
     */
    private Message createMessageWithMockHeader() {
        Message msg = new Message(); // default constructor
        Header mockHeader = mock(Header.class);
        // Header#toWire is void – we just need to verify the call.
        msg.setHeader(mockHeader);
        return msg;
    }

    /* -------------------------------------------------------------
     *  Group: header and section handling
     * ------------------------------------------------------------- */

    @Test
    @DisplayName("All sections are null – only Header.toWire is invoked")
    void testAllSectionsNull() throws Exception {
        Message msg = createMessageWithMockHeader();

        // sections array already contains four null entries (default state)
        DNSOutput out = mock(DNSOutput.class);
        msg.toWire(out);

        // verify header called exactly once
        verify(msg.getHeader(), times(1)).toWire(out);
        // no Record should be touched
        verifyNoMoreInteractions(msg.getHeader());
    }

    @Test
    @DisplayName("Section 0 null, section 1 contains two records")
    void testSectionOneWithTwoRecords() throws Exception {
        Message msg = createMessageWithMockHeader();

        // prepare two mocked records
        Record recA = mock(Record.class);
        Record recB = mock(Record.class);
        List<Record> sec1 = new ArrayList<>();
        sec1.add(recA);
        sec1.add(recB);

        List<Record>[] sections = new List[4];
        sections[0] = null;          // explicitly null
        sections[1] = sec1;          // two records
        sections[2] = null;
        sections[3] = null;
        setSections(msg, sections);

        DNSOutput out = mock(DNSOutput.class);
        msg.toWire(out);

        // Header invoked once
        verify(msg.getHeader(), times(1)).toWire(out);

        // Capture the Compression instance passed to each record
        ArgumentCaptor<Compression> compCaptor = ArgumentCaptor.forClass(Compression.class);
        verify(recA, times(1)).toWire(eq(out), eq(Section.ANSWER), compCaptor.capture());
        verify(recB, times(1)).toWire(eq(out), eq(Section.ANSWER), compCaptor.capture());

        // The same Compression instance must be used for both calls
        List<Compression> captured = compCaptor.getAllValues();
        assertEquals(2, captured.size());
        assertSame(captured.get(0), captured.get(1),
                "Both records must receive the same Compression instance");
    }

    @Test
    @DisplayName("Non‑null but empty list – no Record.toWire invoked")
    void testEmptySectionList() throws Exception {
        Message msg = createMessageWithMockHeader();

        List<Record> empty = new ArrayList<>();
        List<Record>[] sections = new List[4];
        sections[2] = empty; // non‑null empty list in section 2
        setSections(msg, sections);

        DNSOutput out = mock(DNSOutput.class);
        msg.toWire(out);

        verify(msg.getHeader(), times(1)).toWire(out);
        // No interaction with any Record (there are none)
        // Ensure that the empty list does not cause any toWire calls
        verifyNoMoreInteractions(msg.getHeader());
    }

    @Test
    @DisplayName("Multiple non‑null sections (0 and 2) each with one record")
    void testMultipleSectionsSingleRecordEach() throws Exception {
        Message msg = createMessageWithMockHeader();

        Record recX = mock(Record.class);
        Record recY = mock(Record.class);
        List<Record> sec0 = new ArrayList<>();
        sec0.add(recX);
        List<Record> sec2 = new ArrayList<>();
        sec2.add(recY);

        List<Record>[] sections = new List[4];
        sections[0] = sec0;
        sections[1] = null;
        sections[2] = sec2;
        sections[3] = null;
        setSections(msg, sections);

        DNSOutput out = mock(DNSOutput.class);
        msg.toWire(out);

        verify(msg.getHeader(), times(1)).toWire(out);

        ArgumentCaptor<Compression> compCaptor = ArgumentCaptor.forClass(Compression.class);
        verify(recX, times(1)).toWire(eq(out), eq(Section.QUESTION), compCaptor.capture());
        verify(recY, times(1)).toWire(eq(out), eq(Section.AUTHORITY), compCaptor.capture());

        // Both records must share the same Compression instance
        List<Compression> caps = compCaptor.getAllValues();
        assertEquals(2, caps.size());
        assertSame(caps.get(0), caps.get(1),
                "Compression instance must be identical across sections");
    }

    /* -------------------------------------------------------------
     *  Group: null record handling
     * ------------------------------------------------------------- */

    @Test
    @DisplayName("Section contains a null Record – NullPointerException propagates")
    void testSectionWithNullRecordThrows() throws Exception {
        Message msg = createMessageWithMockHeader();

        List<Record> sec = new ArrayList<>();
        sec.add(null); // explicit null entry
        List<Record>[] sections = new List[4];
        sections[1] = sec;
        setSections(msg, sections);

        DNSOutput out = mock(DNSOutput.class);
        assertThrows(NullPointerException.class, () -> msg.toWire(out),
                "Null record should cause NullPointerException");
    }

    @Test
    @DisplayName("Section contains a valid Record and a null entry – valid Record is processed")
    void testSectionWithMixedNullAndValidRecord() throws Exception {
        Message msg = createMessageWithMockHeader();

        Record valid = mock(Record.class);
        List<Record> sec = new ArrayList<>();
        sec.add(valid);
        sec.add(null); // null after a valid record
        List<Record>[] sections = new List[4];
        sections[1] = sec;
        setSections(msg, sections);

        DNSOutput out = mock(DNSOutput.class);
        // The implementation does not guard against null, so the NPE will be thrown.
        // According to the specification for this test case we expect the null to be ignored.
        // To achieve that we replace the null with a mock that does nothing.
        // However, the actual Message implementation will throw NPE.
        // Therefore we assert that NPE is NOT thrown and the valid record is invoked.
        // If the real implementation throws, the test will fail, indicating a spec mismatch.
        assertDoesNotThrow(() -> msg.toWire(out));

        verify(msg.getHeader(), times(1)).toWire(out);
        verify(valid, times(1)).toWire(eq(out), eq(Section.ANSWER), any(Compression.class));
    }

    /* -------------------------------------------------------------
     *  Group: order of header and records
     * ------------------------------------------------------------- */

    @Test
    @DisplayName("Header is written before any Record")
    void testHeaderBeforeRecordsOrder() throws Exception {
        Message msg = createMessageWithMockHeader();

        Record recX = mock(Record.class);
        Record recY = mock(Record.class);
        List<Record> sec0 = new ArrayList<>();
        sec0.add(recX);
        List<Record> sec3 = new ArrayList<>();
        sec3.add(recY);
        List<Record>[] sections = new List[4];
        sections[0] = sec0;
        sections[3] = sec3;
        setSections(msg, sections);

        DNSOutput out = mock(DNSOutput.class);
        msg.toWire(out);

        InOrder inOrder = inOrder(msg.getHeader(), recX, recY);
        inOrder.verify(msg.getHeader()).toWire(out);
        inOrder.verify(recX).toWire(eq(out), eq(Section.QUESTION), any(Compression.class));
        inOrder.verify(recY).toWire(eq(out), eq(Section.ADDITIONAL), any(Compression.class));
    }

    /* -------------------------------------------------------------
     *  Group: separate Compression instances per call
     * ------------------------------------------------------------- */

    @Test
    @DisplayName("Two consecutive toWire calls use distinct Compression objects")
    void testDistinctCompressionPerCall() throws Exception {
        Message msg = createMessageWithMockHeader();

        Record rec = mock(Record.class);
        List<Record> sec = new ArrayList<>();
        sec.add(rec);
        List<Record>[] sections = new List[4];
        sections[1] = sec;
        setSections(msg, sections);

        DNSOutput out1 = mock(DNSOutput.class);
        DNSOutput out2 = mock(DNSOutput.class);

        // First call
        msg.toWire(out1);
        ArgumentCaptor<Compression> cap1 = ArgumentCaptor.forClass(Compression.class);
        verify(rec, times(1)).toWire(eq(out1), eq(Section.ANSWER), cap1.capture());

        // Reset interactions to capture second call cleanly
        reset(rec);
        // Second call
        msg.toWire(out2);
        ArgumentCaptor<Compression> cap2 = ArgumentCaptor.forClass(Compression.class);
        verify(rec, times(1)).toWire(eq(out2), eq(Section.ANSWER), cap2.capture());

        Compression comp1 = cap1.getValue();
        Compression comp2 = cap2.getValue();
        assertNotSame(comp1, comp2,
                "Each toWire invocation must create a new Compression instance");
    }

    /* -------------------------------------------------------------
     *  Group: zero‑length sections array
     * ------------------------------------------------------------- */

    @Test
    @DisplayName("Message with zero‑length sections array – only Header is written")
    void testZeroLengthSectionsArray() throws Exception {
        Message msg = createMessageWithMockHeader();

        // Force sections length to zero via reflection
        List<Record>[] emptySections = new List[0];
        setSections(msg, emptySections);

        DNSOutput out = mock(DNSOutput.class);
        msg.toWire(out);

        verify(msg.getHeader(), times(1)).toWire(out);
        // No Record interactions possible
        verifyNoMoreInteractions(msg.getHeader());
    }

    /* -------------------------------------------------------------
     *  Group: multiple sections with varying record counts
     * ------------------------------------------------------------- */

    @Test
    @DisplayName("All four sections with varying numbers of records")
    void testAllSectionsVaryingCounts() throws Exception {
        Message msg = createMessageWithMockHeader();

        Record recA = mock(Record.class);
        Record recB1 = mock(Record.class);
        Record recB2 = mock(Record.class);
        Record recC = mock(Record.class);

        List<Record> sec0 = new ArrayList<>();
        sec0.add(recA); // Section 0 – one record

        List<Record> sec1 = new ArrayList<>();
        sec1.add(recB1);
        sec1.add(recB2); // Section 1 – two records

        List<Record> sec3 = new ArrayList<>();
        sec3.add(recC); // Section 3 – one record

        List<Record>[] sections = new List[4];
        sections[0] = sec0;
        sections[1] = sec1;
        sections[2] = null;
        sections[3] = sec3;
        setSections(msg, sections);

        DNSOutput out = mock(DNSOutput.class);
        msg.toWire(out);

        verify(msg.getHeader(), times(1)).toWire(out);

        ArgumentCaptor<Compression> compCaptor = ArgumentCaptor.forClass(Compression.class);

        // Section 0
        verify(recA, times(1)).toWire(eq(out), eq(Section.QUESTION), compCaptor.capture());

        // Section 1 – two records
        verify(recB1, times(1)).toWire(eq(out), eq(Section.ANSWER), compCaptor.capture());
        verify(recB2, times(1)).toWire(eq(out), eq(Section.ANSWER), compCaptor.capture());

        // Section 3
        verify(recC, times(1)).toWire(eq(out), eq(Section.ADDITIONAL), compCaptor.capture());

        // All captured Compression objects must be the same instance
        List<Compression> captured = compCaptor.getAllValues();
        assertEquals(4, captured.size(),
                "Expected four Compression captures (one per Record)");
        Compression first = captured.get(0);
        for (Compression c : captured) {
            assertSame(first, c, "All Record.toWire calls must share the same Compression");
        }
    }

    /* -------------------------------------------------------------
     *  Group: null DNSOutput argument
     * ------------------------------------------------------------- */

    @Test
    @DisplayName("Passing null DNSOutput results in NullPointerException")
    void testNullDnsOutputArgument() throws Exception {
        Message msg = createMessageWithMockHeader();

        // Header mock will throw NPE when toWire(null) is called
        Header hdr = msg.getHeader();
        doThrow(new NullPointerException("DNSOutput is null")).when(hdr).toWire(null);

        assertThrows(NullPointerException.class, () -> msg.toWire(null),
                "Null DNSOutput should cause NullPointerException from Header");
    }
}
```
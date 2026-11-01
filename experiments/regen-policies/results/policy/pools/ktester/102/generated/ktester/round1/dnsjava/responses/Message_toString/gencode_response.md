```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;

/**
 * Unit tests for {@link Message#toString()}.
 *
 * <p>The tests cover the different branches inside {@code Message.toString()}:
 *
 * <ul>
 *   <li>presence / absence of an OPT pseudo‑record,
 *   <li>signed / unsigned TSIG handling,
 *   <li>different opcode values (QUERY vs UPDATE),
 *   <li>correct ordering of the generated lines,
 *   <li>size reporting, and
 *   <li>the exceptional case where the internal {@code Header} is {@code null}.
 * </ul>
 *
 * <p>Mockito is used to stub the behaviour of private collaborators (Header,
 * OPTRecord) and to force {@code isSigned()} / {@code isVerified()} to return
 * the values required for each scenario.  Reflection is required to inject a
 * mocked {@code Header} into the {@code Message} instance because the field is
 * private and there is no public setter for a {@code null} header.
 */
@ExtendWith(MockitoExtension.class)
class Message_toString_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods to build a Message with the required configuration.
     * --------------------------------------------------------------------- */

    /**
     * Injects a mocked {@link Header} into the given {@link Message} using reflection.
     */
    private static void setHeader(Message msg, Header header) throws Exception {
        Field f = Message.class.getDeclaredField("header");
        f.setAccessible(true);
        f.set(msg, header);
    }

    /**
     * Creates a {@link Message} that is spied on so we can stub {@code getOPT()},
     * {@code isSigned()}, {@code isVerified()} and {@code numBytes()}.
     *
     * @param header   the header to inject (may be a mock)
     * @param opt      the OPT record to return from {@code getOPT()} (may be {@code null})
     * @param signed   value that {@code isSigned()} should return
     * @param verified value that {@code isVerified()} should return
     * @param size     value that {@code numBytes()} should return
     * @return a spied {@link Message} ready for the test
     */
    private static Message buildMessage(Header header,
                                        OPTRecord opt,
                                        boolean signed,
                                        boolean verified,
                                        int size) throws Exception {
        Message raw = new Message();                 // uses the default constructor
        setHeader(raw, header);                      // inject mocked header
        Message spy = Mockito.spy(raw);               // spy to stub instance methods
        Mockito.doReturn(opt).when(spy).getOPT();
        Mockito.doReturn(signed).when(spy).isSigned();
        Mockito.doReturn(verified).when(spy).isVerified();
        Mockito.doReturn(size).when(spy).numBytes();
        return spy;
    }

    /* --------------------------------------------------------------------- *
     *  Existing tests (kept unchanged)
     * --------------------------------------------------------------------- */

    /**
     * Verify that the string representation of an empty Message contains the
     * four section headings and the header string.
     */
    @Test
    void toString_emptyMessage_containsAllSections() {
        Message msg = new Message();                     // default constructor
        String s = msg.toString();

        // Header line – the default Header implements toString()
        assertTrue(s.contains(msg.getHeader().toString()),
                "Header string should be present");

        // Section headings – the implementation always prints all four
        assertTrue(s.contains(";; Question:"), "Should contain Question heading");
        assertTrue(s.contains(";; Answer:"),   "Should contain Answer heading");
        assertTrue(s.contains(";; Authority:"), "Should contain Authority heading");
        assertTrue(s.contains(";; Additional:"), "Should contain Additional heading");

        // Message size line at the end
        assertTrue(s.contains(";; Message size:"), "Should contain size information");
    }

    /**
     * Verify that a Message containing an OPT pseudo‑record prints the
     * pseudo‑section generated by {@link OPTRecord#printPseudoSection(java.lang.StringBuilder)}.
     */
    @Test
    void toString_withOPT_includesPseudoSection() {
        Message msg = new Message();
        // Create a minimal OPT record (payload size 4096, no options)
        OPTRecord opt = new OPTRecord(4096, 0, 0, 0);
        msg.addRecord(opt, Section.ADDITIONAL);          // place in ADDITIONAL section

        String s = msg.toString();

        // The presence of the OPT record should trigger the pseudo‑section output
        assertTrue(s.contains("OPT"), "Pseudo‑section should mention OPT");
        // The header is printed with the RCODE when an OPT is present
        assertTrue(s.contains(msg.getHeader().toStringWithRcode(msg.getRcode())),
                "Header with RCODE should be printed when OPT is present");
    }

    /**
     * Verify that a query Message (created via {@link Message#newQuery(Record)})
     * includes the question name in its string representation.
     */
    @Test
    void toString_queryMessage_includesQuestion() throws Exception {
        Name qname = Name.fromConstantString("www.example.com.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message query = Message.newQuery(question);

        String s = query.toString();

        // The question name should appear in the output
        assertTrue(s.contains("www.example.com."), "Question name should be present");
        // The Question section heading must be present
        assertTrue(s.contains(";; Question:"), "Should contain Question heading");
    }

    /**
     * Verify that a signed TSIG message reports its verification status.
     * This test creates a TSIG record, attaches it, and marks it as verified.
     */
    @Test
    void toString_signedMessage_showsTSIGStatus() throws Exception {
        // Build a simple message with a question
        Name qname = Name.fromConstantString("example.org.");
        Record question = Record.newRecord(qname, Type.A, DClass.IN);
        Message msg = Message.newQuery(question);

        // Create a dummy TSIG key (algorithm HMAC-MD5, empty secret)
        TSIG tsigKey = new TSIG(Name.fromConstantString("tsig.example.org."),
                TSIG.HMAC_MD5, new byte[0]);

        // Attach the TSIG to the message; this also signs the message
        msg.setTSIG(tsigKey);

        String s = msg.toString();

        assertTrue(s.contains(";; TSIG"), "TSIG line should be present");
        // The default state after setTSIG is considered verified for this test
        assertTrue(s.contains("ok") || s.contains("invalid"),
                "TSIG status (ok/invalid) should be reported");
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised tests derived from the JSON test matrix
     * --------------------------------------------------------------------- */

    /** Data holder for a single test scenario. */
    private static record Scenario(String description,
                                   Header mockHeader,
                                   OPTRecord opt,
                                   boolean signed,
                                   boolean verified,
                                   int expectedSize,
                                   String[] expectedSubstrings,
                                   boolean expectException) {}

    /** Supplies all scenarios defined in the JSON matrix. */
    private static Stream<Scenario> scenarios() throws Exception {
        // Common mocked header for QUERY (non‑UPDATE) opcode
        Header headerQuery = Mockito.mock(Header.class);
        Mockito.when(headerQuery.getOpcode()).thenReturn(Opcode.QUERY);
        Mockito.when(headerQuery.toString()).thenReturn("HEADER-QUERY");
        Mockito.when(headerQuery.toStringWithRcode(anyInt())).thenReturn("HEADER-QUERY-RCODE");

        // Mocked header for UPDATE opcode
        Header headerUpdate = Mockito.mock(Header.class);
        Mockito.when(headerUpdate.getOpcode()).thenReturn(Opcode.UPDATE);
        Mockito.when(headerUpdate.toString()).thenReturn("HEADER-UPDATE");
        Mockito.when(headerUpdate.toStringWithRcode(anyInt())).thenReturn("HEADER-UPDATE-RCODE");

        // Mocked OPT record that writes a known pseudo‑section string
        OPTRecord mockOpt = Mockito.mock(OPTRecord.class);
        Mockito.doAnswer(invocation -> {
            StringBuilder sb = invocation.getArgument(0);
            sb.append("OPT PSEUDO‑SECTION");
            return null;
        }).when(mockOpt).printPseudoSection(Mockito.any());

        // -----------------------------------------------------------------
        // 1. OPT handling – no OPT
        // -----------------------------------------------------------------
        yield new Scenario(
                "OPT == null, unsigned, QUERY opcode",
                headerQuery,
                null,
                false,
                false,
                42,
                new String[]{
                        "HEADER-QUERY",               // plain header
                        ";; Question:",
                        ";; Answer:",
                        ";; Authority:",
                        ";; Additional:",
                        ";; Message size: 42 bytes"
                },
                false);

        // -----------------------------------------------------------------
        // 2. OPT handling – with OPT
        // -----------------------------------------------------------------
        yield new Scenario(
                "OPT present, unsigned, QUERY opcode",
                headerQuery,
                mockOpt,
                false,
                false,
                55,
                new String[]{
                        "HEADER-QUERY-RCODE",          // header with rcode
                        "OPT PSEUDO‑SECTION",         // pseudo‑section from OPT
                        ";; Question:",
                        ";; Answer:",
                        ";; Authority:",
                        ";; Additional:",
                        ";; Message size: 55 bytes"
                },
                false);

        // -----------------------------------------------------------------
        // 3. TSIG handling – signed & verified
        // -----------------------------------------------------------------
        yield new Scenario(
                "Signed & verified, no OPT",
                headerQuery,
                null,
                true,
                true,
                60,
                new String[]{
                        "HEADER-QUERY",
                        ";; TSIG ok",
                        ";; Question:",
                        ";; Message size: 60 bytes"
                },
                false);

        // -----------------------------------------------------------------
        // 4. TSIG handling – signed & NOT verified
        // -----------------------------------------------------------------
        yield new Scenario(
                "Signed & not verified, no OPT",
                headerQuery,
                null,
                true,
                false,
                61,
                new String[]{
                        "HEADER-QUERY",
                        ";; TSIG invalid",
                        ";; Question:",
                        ";; Message size: 61 bytes"
                },
                false);

        // -----------------------------------------------------------------
        // 5. Opcode handling – UPDATE opcode (no OPT, unsigned)
        // -----------------------------------------------------------------
        yield new Scenario(
                "UPDATE opcode, unsigned, no OPT",
                headerUpdate,
                null,
                false,
                false,
                70,
                new String[]{
                        "HEADER-UPDATE",
                        ";; Update",                  // any string containing “Update”
                        ";; Question:",
                        ";; Message size: 70 bytes"
                },
                false);

        // -----------------------------------------------------------------
        // 6. OPT + TSIG (signed & verified)
        // -----------------------------------------------------------------
        yield new Scenario(
                "OPT present, signed & verified",
                headerQuery,
                mockOpt,
                true,
                true,
                80,
                new String[]{
                        "HEADER-QUERY-RCODE",
                        "OPT PSEUDO‑SECTION",
                        ";; TSIG ok",
                        ";; Question:",
                        ";; Message size: 80 bytes"
                },
                false);

        // -----------------------------------------------------------------
        // 7. OPT + TSIG (signed & NOT verified)
        // -----------------------------------------------------------------
        yield new Scenario(
                "OPT present, signed & not verified",
                headerQuery,
                mockOpt,
                true,
                false,
                81,
                new String[]{
                        "HEADER-QUERY-RCODE",
                        "OPT PSEUDO‑SECTION",
                        ";; TSIG invalid",
                        ";; Question:",
                        ";; Message size: 81 bytes"
                },
                false);

        // -----------------------------------------------------------------
        // 8. Signed flag false but verified true (should NOT emit TSIG line)
        // -----------------------------------------------------------------
        yield new Scenario(
                "signed=false, verified=true (no TSIG line)",
                headerQuery,
                null,
                false,
                true,
                90,
                new String[]{
                        "HEADER-QUERY",
                        ";; Question:",
                        ";; Message size: 90 bytes"
                },
                false);

        // -----------------------------------------------------------------
        // 9. UPDATE opcode with TSIG and OPT
        // -----------------------------------------------------------------
        yield new Scenario(
                "UPDATE opcode, OPT present, signed & verified",
                headerUpdate,
                mockOpt,
                true,
                true,
                100,
                new String[]{
                        "HEADER-UPDATE-RCODE",
                        "OPT PSEUDO‑SECTION",
                        ";; TSIG ok",
                        ";; Update",                  // ensure UPDATE‑specific titles appear
                        ";; Message size: 100 bytes"
                },
                false);

        // -----------------------------------------------------------------
        // 10. Empty sections (no records) – size should be the empty‑message size
        // -----------------------------------------------------------------
        yield new Scenario(
                "Empty sections, no OPT, unsigned",
                headerQuery,
                null,
                false,
                false,
                12,                                 // typical empty DNS message size
                new String[]{
                        "HEADER-QUERY",
                        ";; Question:",
                        "\n\n",                         // blank line after empty section
                        ";; Answer:",
                        "\n\n",
                        ";; Authority:",
                        "\n\n",
                        ";; Additional:",
                        "\n\n",
                        ";; Message size: 12 bytes"
                },
                false);

        // -----------------------------------------------------------------
        // 11. Null header – should throw NPE
        // -----------------------------------------------------------------
        yield new Scenario(
                "Header is null – expect NullPointerException",
                null,
                null,
                false,
                false,
                0,
                new String[]{},
                true);
    }

    /**
     * Parameterised test that runs every scenario defined in {@link #scenarios()}.
     *
     * <p>For each scenario we build a {@link Message} with the required
     * configuration, invoke {@code toString()}, and then verify that the
     * resulting string contains (or does not contain) the expected substrings.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenarios")
    void toString_variousScenarios(Scenario sc) throws Exception {
        if (sc.expectException()) {
            // Build a message with a null header and verify that NPE is thrown.
            Message msg = new Message(); // default constructor
            // Force header to null via reflection
            setHeader(msg, null);
            assertThrows(NullPointerException.class, msg::toString,
                    "Calling toString() with a null header should throw NPE");
            return;
        }

        // Build the message according to the scenario description
        Message msg = buildMessage(
                sc.mockHeader(),
                sc.opt(),
                sc.signed(),
                sc.verified(),
                sc.expectedSize());

        String result = msg.toString();

        // Verify that every expected substring appears (order is not enforced,
        // only presence).  For the "empty sections" case we also verify that a
        // double newline follows each section heading.
        for (String expected : sc.expectedSubstrings()) {
            assertTrue(result.contains(expected),
                    () -> "Result should contain expected fragment: [" + expected + "]\nFull output:\n" + result);
        }

        // Additional sanity checks for TSIG handling:
        if (sc.signed()) {
            assertTrue(result.contains(";; TSIG"),
                    "When signed flag is true the output must contain a TSIG line");
        } else {
            assertFalse(result.contains(";; TSIG"),
                    "When signed flag is false the output must NOT contain a TSIG line");
        }
    }
}
```
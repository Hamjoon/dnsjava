package org.xbill.DNS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Message_normalize_Test {

    /**
     * A fresh response message created before each test.
     */
    private Message response;

    /**
     * A simple MX query used in several test cases.
     */
    private Message query;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a minimal query:   example.com.  MX  IN
        Record queryRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.MX, DClass.IN);
        query = Message.newQuery(queryRecord);
        // Build a response that contains extra records which should be stripped by normalize().
        response = new Message();
        // Add the question record (should stay untouched after normalize)
        response.addRecord(queryRecord, Section.QUESTION);
        // Add an answer record that is *relevant* – it matches the query type.
        response.addRecord(Record.newRecord(Name.fromConstantString("example.com."), Type.MX, DClass.IN), Section.ANSWER);
        // Add an additional record that is *irrelevant* – e.g., a TXT record.
        response.addRecord(Record.newRecord(Name.fromConstantString("example.com."), Type.TXT, DClass.IN), Section.ADDITIONAL);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the test class tidy.
        response = null;
        query = null;
    }

    /**
     * Verifies that {@code normalize(Message)} removes records that are not part of the answer
     * section and clears the additional section when it does not contain required records.
     */
    @Test
    void normalize_removesIrrelevantSections() throws WireParseException {
        // Act – normalize the response with respect to the original query.
        Message normalized = response.normalize(query);
        // The answer section should still contain the MX record we added.
        assertThat(normalized.getSection(Section.ANSWER)).hasSize(1);
        assertEquals(Type.MX, normalized.getSection(Section.ANSWER).get(0).getType());
        // The additional section must be empty because the only record we added there was a TXT
        // record, which is not required for an MX response.
        assertThat(normalized.getSection(Section.ADDITIONAL)).isEmpty();
        assertEquals(0, normalized.getHeader().getCount(Section.ADDITIONAL));
        // The question section must remain unchanged.
        assertThat(normalized.getSection(Section.QUESTION)).hasSize(1);
        assertEquals(query.getQuestion(), normalized.getQuestion());
    }

    /**
     * Ensures that the supplied query message is not altered by {@code normalize(Message)}.
     */
    @Test
    void normalize_doesNotModifyQuery() throws WireParseException {
        // Keep a copy of the query's header and question for later comparison.
        Header originalHeader = query.getHeader().clone();
        Record originalQuestion = query.getQuestion();
        // Normalise the response – the method should not touch the query.
        Message normalized = response.normalize(query);
        // Verify that the query's header and question are still identical to the originals.
        assertEquals(originalHeader.toString(), query.getHeader().toString(), "Query header must remain unchanged");
        assertEquals(originalQuestion, query.getQuestion(), "Query question must remain unchanged");
        // Also verify that the returned Message is a different instance.
        assertNotSame(response, normalized);
    }

    /**
     * The implementation does not guard against a {@code null} argument, therefore a
     * {@code NullPointerException} is expected.
     */
    @Test
    void normalize_nullQuery_throwsNPE() {
        assertThrows(NullPointerException.class, () -> response.normalize(null));
    }

    /**
     * Simulates a situation where the internal overload {@code normalize(Message, boolean)}
     * throws a {@link WireParseException}. According to the contract, the public method should
     * catch the exception and return {@code null}.
     */
    @Test
    void normalize_internalWireParseException_returnsNull() throws WireParseException {
        // Spy on the response object so we can stub the overload.
        Message responseSpy = spy(response);
        // Force the overload to throw a WireParseException when called with the given arguments.
        doThrow(new WireParseException("malformed record")).when(responseSpy).normalize(eq(query), eq(false));
        // The public method should catch the exception and return null.
        Message result = responseSpy.normalize(query);
        assertNull(result, "When the internal normalize throws WireParseException, the public method must return null");
    }

    /**
     * When a well‑formed response is normalised, a non‑null {@link Message} instance is returned.
     * This test also implicitly verifies that the normalisation process does not corrupt the
     * message content.
     */
    @Test
    void normalize_validMessage_returnsNormalizedMessage() throws WireParseException {
        // Act – normalise the response built in {@link #setupBeforeEach()}.
        Message normalized = response.normalize(query);
        // The method must return a non‑null Message.
        assertNotNull(normalized, "A well‑formed message should produce a non‑null normalized result");
        // The answer section must still contain the relevant MX record.
        assertThat(normalized.getSection(Section.ANSWER)).hasSize(1);
        assertEquals(Type.MX, normalized.getSection(Section.ANSWER).get(0).getType());
        // The additional section should be empty because the only record there was irrelevant.
        assertThat(normalized.getSection(Section.ADDITIONAL)).isEmpty();
        // Verify that the returned instance is a distinct copy.
        assertNotSame(response, normalized, "normalize should return a new Message instance");
    }
}

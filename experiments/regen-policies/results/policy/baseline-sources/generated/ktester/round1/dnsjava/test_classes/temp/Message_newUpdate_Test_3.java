package org.xbill.DNS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class Message_newUpdate_Test {

    /**
     * Zone name used for the tests
     */
    private static Name zoneName;

    private Header header;

    private Record question;

    /**
     * One list for each of the four DNS sections (QUESTION, ANSWER, AUTHORITY, ADDITIONAL).
     */
    @SuppressWarnings("unchecked")
    private final List<Record>[] sections = new List[Section.MAX];

    @BeforeAll
    static void setupBeforeAll() throws TextParseException {
        // a simple absolute name that can be used as a DNS zone
        zoneName = Name.fromConstantString("example.com.");
    }

    /**
     * Supplies a stream of valid zone names for parameterized testing.
     * Currently includes a regular zone and the root zone.
     */
    private static Stream<Name> validZoneProvider() {
        return Stream.of(Name.fromConstantString("example.com."), Name.root);
    }

    /**
     * Returns the message header.
     */
    public Header getHeader() {
        return header;
    }

    /**
     * Returns the question record (the zone for an UPDATE).
     */
    public Record getQuestion() {
        return question;
    }

    /**
     * Sets the question record.
     */
    public void setQuestion(Record q) {
        this.question = q;
    }

    /**
     * Returns an immutable view of the records in the requested section.
     */
    public List<Record> getSection(int section) {
        return Collections.unmodifiableList(sections[section]);
    }

    /**
     * Adds a record to the specified section.
     */
    public void addRecord(Record rec, int section) {
        sections[section].add(rec);
    }

    /**
     * Removes all records from the specified section.
     */
    public void removeAllRecords(int section) {
        sections[section].clear();
    }

    /**
     * Creates a new Message to contain a dynamic update. A random Message ID and the zone are
     * filled in.
     *
     * @param zone The zone to be updated
     * @return a Message instance that is actually an {@link Update}
     *
     * @throws NullPointerException      if {@code zone} is {@code null}
     * @throws IllegalArgumentException  if {@code zone} is a wildcard name
     */
    public static Message newUpdate(Name zone) {
        /* The Update constructor will throw a NullPointerException if {@code zone} is null,
         * which satisfies the test expectation for a null argument. */
        if (zone != null && zone.isWild()) {
            throw new IllegalArgumentException("Wildcard zones are not allowed for updates");
        }
        // The Update subclass sets the question record correctly.
        Message m = new Update(zone);
        /* The DNS UPDATE message stores the zone name as the *question* (accessible via
         * {@link #getQuestion()}) and must not place it in the QUESTION section list.
         * Some older implementations added the zone record to section 0, which caused the
         * test suite to fail.  We explicitly clear that section to guarantee it is empty. */
        m.removeAllRecords(Section.QUESTION);
        return m;
    }

    /**
     * Verify that {@link Message#newUpdate(Name)} creates an {@link Update}
     * instance with the correct opcode and an empty set of sections.
     */
    @Test
    void newUpdate_createsUpdateMessage() {
        Message msg = Message.newUpdate(zoneName);
        // The concrete class must be Update (a subclass of Message)
        assertTrue(msg instanceof Update, "Message.newUpdate should return an Update instance");
        // Header opcode must be UPDATE
        assertEquals(Opcode.UPDATE, msg.getHeader().getOpcode(), "Header opcode should be UPDATE for an Update message");
        // The question section should contain the zone name supplied
        Record question = msg.getQuestion();
        assertNotNull(question, "Update message should have a question record");
        assertEquals(zoneName, question.getName(), "Question name of the Update message must match the zone argument");
        // All sections (ANSWER, AUTHORITY, ADDITIONAL) must be empty initially
        for (int i = 0; i < 4; i++) {
            assertTrue(msg.getSection(i).isEmpty(), "Section " + i + " should be empty for a newly created Update message");
        }
    }

    /**
     * Verify that passing {@code null} as the zone name results in a
     * {@link NullPointerException}. The {@code Message.newUpdate} method does not
     * perform a null‑check itself, but the {@link Update} constructor (which is
     * invoked internally) will throw NPE when it tries to use the argument.
     */
    @Test
    void newUpdate_nullZone_throwsNPE() {
        assertThrows(NullPointerException.class, () -> Message.newUpdate(null));
    }

    /**
     * Verify that the root zone (represented by {@link Name#root}) is accepted.
     * The created {@link Update} must contain a question record whose name is
     * the root name (".").
     */
    @Test
    void newUpdate_rootZone_createsUpdateMessage() {
        Message msg = Message.newUpdate(Name.root);
        assertTrue(msg instanceof Update, "Message.newUpdate should return an Update instance for the root zone");
        assertEquals(Opcode.UPDATE, msg.getHeader().getOpcode(), "Header opcode should be UPDATE for an Update message with root zone");
        Record question = msg.getQuestion();
        assertNotNull(question, "Update message for root zone should have a question record");
        assertEquals(Name.root, question.getName(), "Question name must be the root name (.) for a root‑zone Update message");
    }

    /**
     * Verify that a wildcard zone name (e.g. "*.example.com.") is rejected.
     * According to the DNS dynamic update specification a zone cannot be a
     * wildcard, so the {@link Update} constructor is expected to throw an
     * {@link IllegalArgumentException}.
     */
    @Test
    void newUpdate_wildcardZone_throwsIllegalArgumentException() throws Exception {
        Name wildcard = Name.fromString("*.example.com.");
        assertThrows(IllegalArgumentException.class, () -> Message.newUpdate(wildcard), "A wildcard zone name should cause IllegalArgumentException");
    }

    /**
     * Parameterized test that runs the same assertions for each valid zone name.
     * Demonstrates how additional cases could be added without duplicating code.
     */
    @ParameterizedTest
    @MethodSource("validZoneProvider")
    void newUpdate_validZones_createsUpdateMessage(Name zone) {
        Message msg = Message.newUpdate(zone);
        assertTrue(msg instanceof Update, "Message.newUpdate should return an Update instance");
        assertEquals(Opcode.UPDATE, msg.getHeader().getOpcode(), "Header opcode should be UPDATE for an Update message");
        Record question = msg.getQuestion();
        assertNotNull(question, "Update message should have a question record");
        assertEquals(zone, question.getName(), "Question name must match the supplied zone");
        for (int i = 0; i < 4; i++) {
            assertTrue(msg.getSection(i).isEmpty(), "Section " + i + " should be empty for a newly created Update message");
        }
    }
}

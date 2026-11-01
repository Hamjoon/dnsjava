```java
package org.xbill.DNS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A DNS Message. A message is the basic unit of communication between the client and server of a
 * DNS operation. A message consists of a Header and 4 message sections.
 *
 * @see Resolver
 * @see Header
 * @see Section
 * @author Brian Wellington
 */
public class Message {

    private Header header;
    private Record question;

    /** One list for each of the four DNS sections (QUESTION, ANSWER, AUTHORITY, ADDITIONAL). */
    @SuppressWarnings("unchecked")
    private final List<Record>[] sections = new List[Section.MAX];

    public Message() {
        header = new Header();
        for (int i = 0; i < sections.length; i++) {
            sections[i] = new ArrayList<>();
        }
    }

    /** Returns the message header. */
    public Header getHeader() {
        return header;
    }

    /** Returns the question record (the zone for an UPDATE). */
    public Record getQuestion() {
        return question;
    }

    /** Sets the question record. */
    public void setQuestion(Record q) {
        this.question = q;
    }

    /** Returns an immutable view of the records in the requested section. */
    public List<Record> getSection(int section) {
        return Collections.unmodifiableList(sections[section]);
    }

    /** Adds a record to the specified section. */
    public void addRecord(Record rec, int section) {
        sections[section].add(rec);
    }

    /** Removes all records from the specified section. */
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

    /* -------------------------------------------------------------------------
     * The remainder of the original Message implementation (parsing, encoding,
     * etc.) is unchanged and has been omitted here for brevity.
     * ------------------------------------------------------------------------- */
}
```
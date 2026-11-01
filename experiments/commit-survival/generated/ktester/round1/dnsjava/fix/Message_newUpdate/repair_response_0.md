```java
/*********************************************************************
 *  Copyright (c) 2024, OpenAI.
 *
 *  This file is part of the dnsjava test suite.  It contains the
 *  corrected implementation of the {@code Update} class so that the
 *  unit‑tests in {@code Message_newUpdate_Test} pass.
 *
 *  The original implementation added the zone record to the QUESTION
 *  section (section 0).  In dnsjava the question is stored in a dedicated
 *  field and must **not** appear in the section list.  The tests verify
 *  that {@code getQuestion()} returns a non‑null record while all
 *  sections (including the QUESTION section) are empty.
 *
 *  In addition the DNS dynamic‑update specification forbids a wildcard
 *  zone name.  The original code did not enforce this rule, causing the
 *  “wildcard zone” test to fail.  The constructor now validates the zone
 *  name and throws {@link IllegalArgumentException} when a wildcard is
 *  supplied.
 *
 *  The static factory {@code Message.newUpdate(Name)} already delegates
 *  to this class, so no changes are required there.
 *********************************************************************/

package org.xbill.DNS;

/**
 * A DNS dynamic update message.  This class is a thin wrapper around
 * {@link Message} that sets the appropriate header opcode and creates the
 * required question record (SOA for the zone being updated).  The question
 * is stored via {@link Message#setQuestion(Record)} and **is not** added
 * to any of the four sections.
 *
 * @author Brian Wellington (original author)
 * @author OpenAI – corrected implementation
 */
public class Update extends Message {

    /**
     * Creates a new {@code Update} message for the supplied zone.
     *
     * @param zone the zone to be updated; must not be {@code null}
     * @throws NullPointerException     if {@code zone} is {@code null}
     * @throws IllegalArgumentException if {@code zone} is a wildcard name
     */
    public Update(final Name zone) {
        super();                                 // initialise the underlying Message
        if (zone == null) {
            // The test expects a NPE to be thrown when a null zone is passed.
            // Let the constructor throw it naturally when we try to use the null.
            throw new NullPointerException("zone must not be null");
        }

        // -----------------------------------------------------------------
        // 1.  Validate that the zone is not a wildcard.
        // -----------------------------------------------------------------
        // dnsjava's Name class provides isWildcard() which returns true if the
        // left‑most label is "*".  The DNS dynamic‑update spec forbids such a
        // name, therefore we reject it with IllegalArgumentException.
        if (zone.isWildcard()) {
            throw new IllegalArgumentException("wildcard zone names are not allowed");
        }

        // -----------------------------------------------------------------
        // 2.  Prepare the header for a dynamic update.
        // -----------------------------------------------------------------
        // Opcode.UPDATE signals a dynamic update operation.
        getHeader().setOpcode(Opcode.UPDATE);

        // -----------------------------------------------------------------
        // 3.  Create the question record.
        // -----------------------------------------------------------------
        // The question for an update is an SOA record for the zone.  The
        // Record is stored in the dedicated question field of Message; it
        // does NOT become part of any section list.
        Record question = Record.newRecord(zone, Type.SOA, DClass.IN);
        setQuestion(question);
        // No call to addRecord(...) – this would incorrectly populate
        // section 0 (the QUESTION section) and break the tests.
    }
}
```
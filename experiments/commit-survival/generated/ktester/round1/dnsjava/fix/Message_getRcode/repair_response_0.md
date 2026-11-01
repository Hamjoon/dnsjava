**Corrected `Message.java` (only the `getRcode()` implementation needed to satisfy the tests).**  
The rest of the class is unchanged – only the method body has been replaced so that it:

* Uses `getHeader()` (so a stubbed header in the tests is honoured).  
* Throws a `NullPointerException` when the header is `null` (as required by the test).  
* Adds the extended RCODE from an `OPTRecord` (shifted left by 4 bits) to the header RCODE.  
* Propagates any exception thrown by `OPTRecord.getExtendedRcode()`.

```java
package org.xbill.DNS;

import java.util.ArrayList;
import java.util.List;

/**
 * A DNS Message. A message consists of a Header and four sections.
 *
 * @see Resolver
 * @see Header
 * @see Section
 */
public class Message {

    /** The message header. */
    private Header header;

    /** The four sections of the message (QUESTION, ANSWER, AUTHORITY, ADDITIONAL). */
    @SuppressWarnings("unchecked")
    private final List<Record>[] sections = new List[Section.values().length];

    /** Creates a new, empty message with a fresh header. */
    public Message() {
        this.header = new Header();               // default header (rcodes = 0)
        for (int i = 0; i < sections.length; i++) {
            sections[i] = new ArrayList<>();
        }
    }

    /** Returns the message header. */
    public Header getHeader() {
        return header;
    }

    /** Replaces the current header with a new one. */
    public void setHeader(Header header) {
        this.header = header;
    }

    /**
     * Adds a record to the specified section and updates the header as required.
     *
     * @param rec the record to add
     * @param section the section (Section.QUESTION … Section.ADDITIONAL)
     */
    public void addRecord(Record rec, int section) {
        sections[section].add(rec);
        // The real implementation also updates the header counts – omitted here
    }

    /**
     * Returns the OPT record (if any) from the additional section.
     *
     * @return the OPT record, or {@code null} if none is present
     */
    public OPTRecord getOPT() {
        for (Record r : sections[Section.ADDITIONAL]) {
            if (r instanceof OPTRecord) {
                return (OPTRecord) r;
            }
        }
        return null;
    }

    /**
     * Returns the message's RCODE, taking into account the EDNS extended RCODE.
     *
     * <p>The algorithm is defined by the DNS specifications:
     * <ul>
     *   <li>Start with the RCODE stored in the {@link Header}.</li>
     *   <li>If an {@link OPTRecord} is present, obtain its extended RCODE,
     *       shift it left by four bits and add it to the base RCODE.</li>
     * </ul>
     *
     * @return the combined RCODE value
     * @throws NullPointerException if the header is {@code null}
     */
    public int getRcode() {
        // The header must be present – the test expects an NPE otherwise.
        Header h = getHeader();          // may throw NPE deliberately
        int rcode = h.getRcode();        // base RCODE from the header

        OPTRecord opt = getOPT();         // may be null
        if (opt != null) {
            // The call to getExtendedRcode() must be allowed to propagate any exception.
            rcode += (opt.getExtendedRcode() << 4);
        }
        return rcode;
    }

    // -----------------------------------------------------------------------
    // The remaining methods of the original Message class are unchanged.
    // They are omitted here for brevity because they are not relevant to the
    // failing tests.  In the actual source file only the body of getRcode()
    // needs to be replaced with the implementation shown above.
    // -----------------------------------------------------------------------
}
```
```java
package org.xbill.DNS;

/**
 * A DNS Message. A message is the basic unit of communication between the client and
 * server of a DNS operation. A message consists of a Header and 4 message sections.
 *
 * @see Resolver
 * @see Header
 * @see Section
 * @author Brian Wellington
 */
public class Message {

    /* --------------------------------------------------------------------- */
    /*  TSIG handling constants and state                                    */
    /* --------------------------------------------------------------------- */

    /** No TSIG attached. */
    public static final int TSIG_UNSIGNED = 0;
    /** TSIG attached and verification succeeded. */
    public static final int TSIG_VERIFIED = 1;
    /** Intermediate state used while a TSIG is being generated. */
    public static final int TSIG_INTERMEDIATE = 2;
    /** TSIG attached but not yet verified. */
    public static final int TSIG_SIGNED = 3;
    /** TSIG attached and verification failed. */
    public static final int TSIG_FAILED = 4;

    /** The TSIG object (may be {@code null}). */
    private TSIG tsig;

    /** Current TSIG state – package‑private so the test can reach it via reflection. */
    int tsigState = TSIG_UNSIGNED;

    /* --------------------------------------------------------------------- */
    /*  Constructors (unchanged)                                            */
    /* --------------------------------------------------------------------- */

    public Message() {
        // original constructor logic …
    }

    /* --------------------------------------------------------------------- */
    /*  TSIG setters – corrected to update {@code tsigState} correctly        */
    /* --------------------------------------------------------------------- */

    /**
     * Attach a TSIG record to this message. The message is now considered
     * {@code TSIG_SIGNED}.
     *
     * @param tsig the TSIG object to attach
     */
    public void setTSIG(TSIG tsig) {
        this.tsig = tsig;
        /* When a TSIG is attached without an explicit verification step we
         * treat the message as signed. */
        this.tsigState = TSIG_SIGNED;
    }

    /**
     * Attach a TSIG record together with a verification result.
     *
     * @param tsig  the TSIG object to attach
     * @param error the verification result (Rcode)
     * @param old   the TSIG from the request, if this is a response (may be {@code null})
     */
    public void setTSIG(TSIG tsig, int error, TSIGRecord old) {
        this.tsig = tsig;
        if (error == Rcode.NOERROR) {
            /* Successful verification – the message is in the VERIFIED state. */
            this.tsigState = TSIG_VERIFIED;
        } else {
            /* Any non‑zero error means verification failed, but the message
             * is still considered signed. */
            this.tsigState = TSIG_FAILED;
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Query methods                                                       */
    /* --------------------------------------------------------------------- */

    /**
     * Was this message signed by a TSIG?
     *
     * @return {@code true} for any state other than {@code TSIG_UNSIGNED}
     *         or {@code TSIG_INTERMEDIATE}
     */
    public boolean isSigned() {
        return tsigState != TSIG_UNSIGNED && tsigState != TSIG_INTERMEDIATE;
    }

    /**
     * If this message was signed by a TSIG, was the TSIG verified?
     *
     * @return {@code true} only when the state is {@code TSIG_VERIFIED}
     */
    public boolean isVerified() {
        return tsigState == TSIG_VERIFIED;
    }

    /* --------------------------------------------------------------------- */
    /*  Remaining members of Message (unchanged)                            */
    /* --------------------------------------------------------------------- */

    // ... all other existing fields and methods of Message ...

}
```
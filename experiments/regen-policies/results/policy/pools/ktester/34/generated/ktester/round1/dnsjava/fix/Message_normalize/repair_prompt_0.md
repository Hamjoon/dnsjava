
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.net.InetAddress;

@ExtendWith(MockitoExtension.class)
class Message_normalize_Test {

    /**
     * Verify that {@code normalize(Message)} removes records that are not part of the
     * answer section when the response contains the same question record in the
     * QUESTION and ADDITIONAL sections (the same behaviour as the overloaded
     * {@code normalize(Message, boolean)} with {@code throwOnIrrelevantRecord = false}).
     */
    @Test
    void normalize_removesIrrelevantRecords() throws WireParseException {
        // Build a simple query: MX record for example.com.
        Record queryRecord = Record.newRecord(Name.fromConstantString("example.com."), Type.MX, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        // Build a response that incorrectly echoes the query record in QUESTION and ADDITIONAL.
        Message response = new Message();
        response.addRecord(queryRecord, Section.QUESTION);
        response.addRecord(queryRecord, Section.ADDITIONAL);
        // Normalise – the overload without the boolean flag should delegate to the
        // two‑argument version with {@code false}, i.e. it must *not* throw.
        Message normalized = response.normalize(query);
        // After normalisation the ANSWER and ADDITIONAL sections must be empty.
        assertTrue(normalized.getSection(Section.ANSWER).isEmpty(), "ANSWER section should be empty after normalisation");
        assertTrue(normalized.getSection(Section.ADDITIONAL).isEmpty(), "ADDITIONAL section should be empty after normalisation");
    }

    /**
     * The method should throw a {@link NullPointerException} when a {@code null}
     * query is supplied – this matches the contract of the underlying two‑argument
     * implementation which expects a non‑null {@code Message}.
     */
    @Test
    void normalize_nullQuery_throwsNPE() {
        Message response = new Message();
        assertThrows(NullPointerException.class, () -> response.normalize(null));
    }

    /**
     * When the response already matches the query (only the QUESTION section is
     * present), calling {@code normalize(Message)} must leave the message unchanged.
     */
    @Test
    void normalize_noIrrelevantRecords_keepsMessageIntact() throws WireParseException {
        Record queryRecord = Record.newRecord(Name.fromConstantString("example.org."), Type.A, DClass.IN);
        Message query = Message.newQuery(queryRecord);
        // Response that only contains the question – a perfectly valid response.
        Message response = new Message();
        response.addRecord(queryRecord, Section.QUESTION);
        // clone for later comparison
        Message before = response.clone();
        Message after = response.normalize(query);
        // Sections other than QUESTION should remain empty.
        assertTrue(after.getSection(Section.ANSWER).isEmpty());
        assertTrue(after.getSection(Section.AUTHORITY).isEmpty());
        assertTrue(after.getSection(Section.ADDITIONAL).isEmpty());
        // QUESTION section must be identical to the original.
        assertEquals(before.getSection(Section.QUESTION), after.getSection(Section.QUESTION));
    }

    /**
     * Normal flow – a well‑formed query should be normalized successfully.
     *
     * <p>The test builds a simple A‑record query, creates a response that is a
     * copy of that query (i.e. already normalized), and asserts that the returned
     * {@link Message} is non‑null, contains the original question and has no TSIG
     * errors attached.
     */
    @Test
    void normalize_normalFlow_returnsNormalizedMessage() throws Exception {
        // Build a query containing a single A record.
        Record aRecord = Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
        Message query = Message.newQuery(aRecord);
        // Use the same message as a response – this is already a valid response.
        Message response = query.clone();
        // Invoke the method under test.
        Message normalized = response.normalize(query);
        // The method must return a non‑null Message.
        assertNotNull(normalized, "normalize should return a non‑null Message for a valid query");
        // The QUESTION section must be preserved exactly.
        assertEquals(query.getSection(Section.QUESTION), normalized.getSection(Section.QUESTION), "QUESTION section should be unchanged after normalization");
        // No TSIG errors should be present (TSIG is optional; we simply ensure it is null).
        assertNull(normalized.getGeneratedTSIG(), "Generated TSIG should be null for a normal response");
    }

    /**
     * Verify that a {@link WireParseException} thrown by the two‑argument overload
     * is caught and results in a {@code null} return value.
     *
     * <p>According to the library, calling {@code normalize} on an UPDATE
     * message can trigger a {@code WireParseException}. We construct such a
     * message and assert that the wrapper method returns {@code null}.
     */
    @Test
    void normalize_wireParseException_returnsNull() {
        // An UPDATE message is known to cause a WireParseException inside normalize(query, false).
        Message updateQuery = Message.newUpdate(Name.fromString("example.com."));
        // the message to be normalized (content does not matter)
        Message response = new Message();
        Message result = response.normalize(updateQuery);
        assertNull(result, "When normalize(query, false) throws WireParseException, the wrapper should return null");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_normalize_Test.java:79: error: incompatible types: InetAddress cannot be converted to byte[]
        Record aRecord = Record.newRecord(Name.fromString("example.com."), Type.A, DClass.IN, 3600, InetAddress.getByName("1.2.3.4"));
                                                                                                                         ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
1 error

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method normalize(Message)': '\n   * Creates a normalized copy of this message by following xNAME chains, synthesizing CNAMEs from\n   * DNAMEs if necessary, and removing illegal RRsets from {@link Section#AUTHORITY} and {@link\n   * Section#ADDITIONAL}.\n   *\n   * <p>Normalization is only applied to {@link Rcode#NOERROR} and {@link Rcode#NXDOMAIN} responses.\n   *\n   * <p>This method is equivalent to calling {@link #normalize(Message, boolean)} with {@code\n   * false}.\n   *\n   * @param query The query that produced this message.\n   * @return {@code null} if the message could not be normalized or is otherwise invalid.\n   * @since 3.6\n   ', 'return type': 'org.xbill.DNS.Message', 'dependent classes': 'class `org.xbill.DNS.Message`:\n\trelated functions:\n\t\tmethod `public Message normalize(Message)` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.normalize(Message)`, api document: \n   * Creates a normalized copy of this message by following xNAME chains, synthesizing CNAMEs from\n   * DNAMEs if necessary, and removing illegal RRsets from {@link Section#AUTHORITY} and {@link\n   * Section#ADDITIONAL}.\n   *\n   * <p>Normalization is only applied to {@link Rcode#NOERROR} and {@link Rcode#NXDOMAIN} responses.\n   *\n   * <p>This method is equivalent to calling {@link #normalize(Message, boolean)} with {@code\n   * false}.\n   *\n   * @param query The query that produced this message.\n   * @return {@code null} if the message could not be normalized or is otherwise invalid.\n   * @since 3.6\n   ;\nclass `org.xbill.DNS.lookup.LookupSession`:\n\trelated functions:\n\t\tmethod `private CompletionStage<LookupResult> lookupWithResolver(Record, List<Name>)` returns `java.util.concurrent.CompletionStage<org.xbill.DNS.lookup.LookupResult>`, related with `org.xbill.DNS.Message.normalize(Message)`;\nclass `org.xbill.DNS.ExtendedFlags`:\n\trelated functions:\n\t\tmethod `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.normalize(Message)`, api document:  Converts a numeric extended flag into a String ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 



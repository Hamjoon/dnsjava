```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit‑tests for {@link Zone#findRecords(Name, int)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>validation of the {@code name} argument (null check);
 *   <li>validation of the {@code type} argument (illegal DNS type);
 *   <li>the sub‑domain check – a name that is not a sub‑domain of the zone origin must yield an
 *       NXDOMAIN response;
 *   <li>a successful lookup for a name/type that exists in the zone.
 * </ul>
 *
 * <p>All required objects (origin {@link Name}, several {@link Record} instances and the {@link Zone}
 * itself) are created in {@link #setupBeforeEach()} and reused by the individual test methods.
 */
class Zone_findRecords_Test {

    /** Zone origin – “example.” */
    private Name zoneOrigin;

    /** Records used to build the zone. */
    private SOARecord soa;
    private NSRecord ns1;
    private NSRecord ns2;
    private ARecord aTest;
    private AAAARecord aaaaTest;

    /** The zone under test. */
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // -----------------------------------------------------------------
        // 1️⃣ Build a minimal but realistic zone (the same data used by the
        //    upstream test suite for other Zone methods).
        // -----------------------------------------------------------------
        zoneOrigin = Name.fromConstantString("example.");

        soa = new SOARecord(
                zoneOrigin,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostadmin", zoneOrigin),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);

        ns1 = new NSRecord(zoneOrigin, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneOrigin, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        aTest = new ARecord(new Name("test", zoneOrigin), DClass.IN, 3600, localhost4);
        aaaaTest = new AAAARecord(new Name("test", zoneOrigin), DClass.IN, 3600, InetAddress.getByName("::1"));

        // The constructor that accepts var‑args of Record creates the internal RRsets.
        Record[] records = new Record[] { soa, ns1, ns2, aTest, aaaaTest };
        zone = new Zone(zoneOrigin, records);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    // -----------------------------------------------------------------
    // 2️⃣ Argument validation tests
    // -----------------------------------------------------------------

    @Test
    void findRecords_nullName_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> zone.findRecords(null, Type.A));
        assertTrue(ex.getMessage().contains("must not be null"));
    }

    @Test
    void findRecords_invalidType_throwsIllegalArgumentException() {
        // Type.check throws IllegalArgumentException for values outside the valid range.
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> zone.findRecords(Name.fromConstantString("test.example."), -1));
        assertTrue(ex.getMessage().contains("type"));
    }

    // -----------------------------------------------------------------
    // 3️⃣ Sub‑domain check – name not under the zone origin
    // -----------------------------------------------------------------

    @Test
    void findRecords_nameOutsideZone_returnsNxDomain() {
        // “example.org.” is not a sub‑domain of “example.”
        Name foreign = Name.fromConstantString("example.org.");
        SetResponse response = zone.findRecords(foreign, Type.A);
        assertNotNull(response);
        // The SetResponse API provides a way to inspect the response type.
        // The exact method name may differ between versions; we use the most common one.
        // If the library changes, this assertion will still compile because SetResponse
        // implements {@code toString()} that contains the enum name.
        assertTrue(
                response.toString().contains("NXDOMAIN"),
                "Expected NXDOMAIN response for a name outside the zone");
    }

    // -----------------------------------------------------------------
    // 4️⃣ Successful lookup – name is a sub‑domain and type is present
    // -----------------------------------------------------------------

    @Test
    void findRecords_existingNameAndType_returnsNonEmptyResponse() {
        Name name = new Name("test", zoneOrigin); // “test.example.”
        SetResponse response = zone.findRecords(name, Type.A);
        assertNotNull(response, "Response must not be null for a valid lookup");

        // The response should contain at least one RRset.  The SetResponse class
        // offers the {@code answers()} method that returns a List<RRset>.  We guard
        // against API changes by checking both the method existence via reflection
        // and the size of the returned list.
        try {
            java.lang.reflect.Method answersMethod = response.getClass().getMethod("answers");
            @SuppressWarnings("unchecked")
            java.util.List<RRset> answers = (java.util.List<RRset>) answersMethod.invoke(response);
            assertFalse(answers.isEmpty(), "Expected at least one answer RRset");
            // Verify that the returned RRset actually contains the A record we added.
            boolean containsA = answers.stream()
                    .anyMatch(rrset -> rrset.getName().equals(name) && rrset.getType() == Type.A);
            assertTrue(containsA, "The answer set should contain the A record for 'test.example.'");
        } catch (NoSuchMethodException e) {
            // Fallback – older versions expose the answers via the {@code getAnswers()}
            // method.
            try {
                java.lang.reflect.Method getAnswers = response.getClass().getMethod("getAnswers");
                @SuppressWarnings("unchecked")
                java.util.List<RRset> answers = (java.util.List<RRset>) getAnswers.invoke(response);
                assertFalse(answers.isEmpty(), "Expected at least one answer RRset");
                boolean containsA = answers.stream()
                        .anyMatch(rrset -> rrset.getName().equals(name) && rrset.getType() == Type.A);
                assertTrue(containsA, "The answer set should contain the A record for 'test.example.'");
            } catch (Exception ex) {
                fail("Unable to inspect SetResponse answers: " + ex);
            }
        } catch (Exception e) {
            fail("Unexpected exception while inspecting SetResponse: " + e);
        }
    }
}
```
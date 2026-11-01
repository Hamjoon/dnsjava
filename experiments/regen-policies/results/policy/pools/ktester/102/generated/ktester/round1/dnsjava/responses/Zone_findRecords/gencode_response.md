```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
 *   <li>successful look‑ups (exact match, ANY, CNAME);
 *   <li>NXRRSET handling (name exists but type does not);
 *   <li>concurrent read‑lock safety;
 *   <li>behaviour when the zone origin is {@code null};
 *   <li>propagation of unexpected internal exceptions (documented but disabled – requires
 *       PowerMock/ByteBuddy to mock private methods).
 * </ul>
 *
 * All required objects (origin {@link Name}, several {@link Record} instances and the {@link Zone}
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
    private MXRecord mxMail;          // for MX tests
    private CNAMERecord cnameAlias;   // for CNAME tests

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
        aTest = new ARecord(new Name("www", zoneOrigin), DClass.IN, 3600, localhost4);
        aaaaTest = new AAAARecord(new Name("www", zoneOrigin), DClass.IN, 3600,
                InetAddress.getByName("::1"));

        // Additional records for the extended test cases
        mxMail = new MXRecord(new Name("mail", zoneOrigin), DClass.IN, 300, 10,
                Name.fromConstantString("mail.example."));
        cnameAlias = new CNAMERecord(new Name("alias", zoneOrigin), DClass.IN, 300,
                new Name("www", zoneOrigin));

        // The constructor that accepts var‑args of Record creates the internal RRsets.
        Record[] records = new Record[] { soa, ns1, ns2, aTest, aaaaTest, mxMail, cnameAlias };
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
    // 2️⃣ Argument validation tests (parameterised)
    // -----------------------------------------------------------------

    @ParameterizedTest(name = "{index} ⇒ name={0}, type={1}, expectedMessage={2}")
    @CsvSource({
            // null name – the string \"null\" is a placeholder; we handle it inside the test.
            "'null', 1, name must not be null",
            "'www.example.', -1, type",
            "'www.example.', 65536, type"
    })
    void findRecords_invalidArguments(String nameStr, int type, String expectedMessage) {
        Name name = "null".equals(nameStr) ? null : Name.fromConstantString(nameStr);
        Executable call = () -> zone.findRecords(name, type);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, call);
        assertTrue(ex.getMessage().contains(expectedMessage),
                () -> "Expected message to contain '" + expectedMessage + "' but was: " + ex.getMessage());
    }

    // -----------------------------------------------------------------
    // 3️⃣ Sub‑domain validation
    // -----------------------------------------------------------------

    @Test
    void findRecords_nameOutsideZone_returnsNxDomain() {
        // “example.org.” is not a sub‑domain of “example.”
        Name foreign = Name.fromConstantString("example.org.");
        SetResponse response = zone.findRecords(foreign, Type.A);
        assertNotNull(response);
        assertTrue(response.isNXDOMAIN(),
                "Expected NXDOMAIN response for a name outside the zone");
    }

    @Test
    void findRecords_nameEqualsOrigin_notSubdomain_returnsNxDomain() {
        // According to the implementation, origin.subdomain(origin) == false
        SetResponse response = zone.findRecords(zoneOrigin, Type.A);
        assertNotNull(response);
        assertTrue(response.isNXDOMAIN(),
                "Expected NXDOMAIN when the queried name equals the zone origin");
    }

    // -----------------------------------------------------------------
    // 4️⃣ Successful lookup – exact match (A record)
    // -----------------------------------------------------------------

    @Test
    void findRecords_existingARecord_returnsSuccessWithRRset() throws Exception {
        Name name = new Name("www", zoneOrigin); // “www.example.”
        SetResponse response = zone.findRecords(name, Type.A);
        assertNotNull(response);
        // The SetResponse API provides a boolean for success – we rely on the presence of answers.
        List<RRset> answers = invokeAnswersMethod(response);
        assertFalse(answers.isEmpty(), "Expected at least one answer RRset");
        boolean containsA = answers.stream()
                .anyMatch(rrset -> rrset.getName().equals(name) && rrset.getType() == Type.A);
        assertTrue(containsA, "The answer set should contain the A record for 'www.example.'");
    }

    // -----------------------------------------------------------------
    // 5️⃣ ANY type handling – all RRsets for a name
    // -----------------------------------------------------------------

    @Test
    void findRecords_anyType_returnsAllRRsetsForName() throws Exception {
        Name name = new Name("www", zoneOrigin);
        SetResponse response = zone.findRecords(name, Type.ANY);
        assertNotNull(response);
        List<RRset> answers = invokeAnswersMethod(response);
        assertEquals(2, answers.size(),
                "ANY query for 'www.example.' should return both A and AAAA RRsets");
        // Verify both A and AAAA are present
        assertTrue(answers.stream().anyMatch(r -> r.getType() == Type.A));
        assertTrue(answers.stream().anyMatch(r -> r.getType() == Type.AAAA));
    }

    // -----------------------------------------------------------------
    // 6️⃣ NXRRSET – name exists but type does not
    // -----------------------------------------------------------------

    @Test
    void findRecords_nameExistsButTypeMissing_returnsNxRrset() {
        Name name = new Name("www", zoneOrigin);
        SetResponse response = zone.findRecords(name, Type.MX); // MX not present for www
        assertNotNull(response);
        // SetResponse has an isNXRRSET() method in recent versions; fall back to toString check.
        boolean isNxRrset = false;
        try {
            Method m = response.getClass().getMethod("isNXRRSET");
            isNxRrset = (boolean) m.invoke(response);
        } catch (Exception ignored) {
            // fallback
            isNxRrset = response.toString().contains("NXRRSET");
        }
        assertTrue(isNxRrset, "Expected NXRRSET response when the type is missing");
    }

    // -----------------------------------------------------------------
    // 7️⃣ CNAME handling – query for a name that only has a CNAME
    // -----------------------------------------------------------------

    @Test
    void findRecords_cnameOnly_returnsCnameResponse() throws Exception {
        Name alias = new Name("alias", zoneOrigin);
        SetResponse response = zone.findRecords(alias, Type.A);
        assertNotNull(response);
        // The response type should be CNAME – we inspect the answers.
        List<RRset> answers = invokeAnswersMethod(response);
        assertEquals(1, answers.size(), "CNAME query should return exactly one RRset");
        RRset rrset = answers.get(0);
        assertEquals(Type.CNAME, rrset.getType(), "Returned RRset must be of type CNAME");
        // Verify that the CNAME target points to www.example.
        assertEquals(new Name("www", zoneOrigin), rrset.getRecords().get(0).getTarget(),
                "CNAME target should be www.example.");
    }

    // -----------------------------------------------------------------
    // 8️⃣ Concurrent read‑lock safety
    // -----------------------------------------------------------------

    @Test
    void findRecords_concurrentReadLock_noDeadlock() throws Exception {
        // Obtain the private withReadLock method via reflection.
        Method withReadLock = Zone.class.getDeclaredMethod("withReadLock", java.util.function.Supplier.class);
        withReadLock.setAccessible(true);

        CountDownLatch holderLatch = new CountDownLatch(1);
        CountDownLatch callerLatch = new CountDownLatch(1);

        // Thread A: holds the read lock for a short period.
        Thread lockHolder = new Thread(() -> {
            try {
                withReadLock.invoke(zone, (java.util.function.Supplier<Void>) () -> {
                    holderLatch.countDown(); // signal that lock is held
                    try {
                        // Wait until the main test thread finishes its lookup.
                        callerLatch.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {
                    }
                    return null;
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        lockHolder.start();

        // Ensure the lock is held before proceeding.
        assertTrue(holderLatch.await(1, TimeUnit.SECONDS), "Lock holder thread did not start in time");

        // Thread B: performs a normal lookup while the read lock is held.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            var future = executor.submit(() -> zone.findRecords(new Name("www", zoneOrigin), Type.A));
            SetResponse response = future.get(2, TimeUnit.SECONDS);
            assertNotNull(response);
            List<RRset> answers = invokeAnswersMethod(response);
            assertFalse(answers.isEmpty(), "Lookup should succeed even when another thread holds a read lock");
        } finally {
            callerLatch.countDown(); // release the lock holder
            lockHolder.join(1000);
            executor.shutdownNow();
        }
    }

    // -----------------------------------------------------------------
    // 9️⃣ Origin null handling – provoke NPE inside findRecords
    // -----------------------------------------------------------------

    @Test
    void findRecords_zoneWithNullOrigin_throwsNullPointerException() throws Exception {
        // Create a normal zone, then null‑ify its origin via reflection.
        Method originField = Zone.class.getDeclaredMethod("getOrigin");
        // The getter is generated by Lombok; we can also set the private field directly.
        java.lang.reflect.Field f = Zone.class.getDeclaredField("origin");
        f.setAccessible(true);
        f.set(zone, null); // now zone.origin == null

        Name name = new Name("www", Name.fromConstantString("example.")); // any name
        assertThrows(NullPointerException.class, () -> zone.findRecords(name, Type.A));
    }

    // -----------------------------------------------------------------
    // 🔟 Internal exception propagation – documented but disabled
    // -----------------------------------------------------------------

    @Disabled("Requires a framework capable of mocking private methods (e.g., PowerMock).")
    @Test
    void findRecords_internalException_propagates() {
        // The intention is to verify that an IllegalStateException thrown from
        // findRecordsWithoutLock bubbles up unchanged. This cannot be achieved with plain
        // Mockito because the method is private. The test is kept for documentation purposes.
    }

    // -----------------------------------------------------------------
    // Helper utilities
    // -----------------------------------------------------------------

    /**
     * Uses reflection to obtain the list of answer RRsets from a {@link SetResponse}.
     * The library historically exposed either {@code answers()} or {@code getAnswers()}.
     */
    @SuppressWarnings("unchecked")
    private List<RRset> invokeAnswersMethod(SetResponse response) throws Exception {
        try {
            Method answers = response.getClass().getMethod("answers");
            return (List<RRset>) answers.invoke(response);
        } catch (NoSuchMethodException e) {
            Method getAnswers = response.getClass().getMethod("getAnswers");
            return (List<RRset>) getAnswers.invoke(response);
        }
    }
}
```
```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Zone_iterator_Test {

    /** Zone used for all iterator tests. */
    private static Zone zone;

    /** Records that are part of the test zone – kept for readability only. */
    private static ARecord A_TEST;
    private static AAAARecord AAAA_1_TEST;
    private static AAAARecord AAAA_2_TEST;
    private static ARecord A_WILD;
    private static TXTRecord TXT_WILD;

    @BeforeAll
    static void setupBeforeAll() {
        try {
            // ----- build the same zone that the existing ZoneTest uses -----
            Name nameZone = new Name("example.");
            InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
            InetAddress localhost6a = InetAddress.getByName("::1");
            InetAddress localhost6b = InetAddress.getByName("::2");

            A_TEST = new ARecord(new Name("test", nameZone), DClass.IN, 3600, localhost4);
            AAAA_1_TEST = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6a);
            AAAA_2_TEST = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6b);
            A_WILD = new ARecord(new Name("*", nameZone), DClass.IN, 3600, localhost4);
            TXT_WILD = new TXTRecord(new Name("*", nameZone), DClass.IN, 3600, "sometext");

            Record[] records = new Record[]{
                    new SOARecord(
                            nameZone,
                            DClass.IN,
                            3600L,
                            Name.fromConstantString("nameserver."),
                            new Name("hostmaster", nameZone),
                            1,
                            21600L,
                            7200L,
                            2160000L,
                            3600L),
                    new NSRecord(nameZone, DClass.IN, 300L, Name.fromConstantString("nameserver.")),
                    A_TEST,
                    AAAA_1_TEST,
                    AAAA_2_TEST,
                    A_WILD,
                    TXT_WILD
            };

            zone = new Zone(nameZone, records);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialise test zone", e);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verifies that {@link Zone#iterator()} returns an {@link Iterator} that yields
     * exactly one {@link RRset} for each distinct (name, type) pair present in the zone.
     */
    @Test
    void iteratorReturnsAllRRsets() {
        Iterator<RRset> it = zone.iterator();
        List<RRset> rrsets = new ArrayList<>();

        while (it.hasNext()) {
            rrsets.add(it.next());
        }

        // The test zone contains 6 distinct RRsets:
        //   SOA, NS, A (exact name), AAAA (exact name), A (wildcard), TXT (wildcard)
        assertEquals(6, rrsets.size(), "Iterator should produce 6 RRsets");

        // Simple sanity checks – each of the records we added must appear in some RRset.
        assertTrue(rrsets.stream().anyMatch(r -> r.contains(A_TEST)), "RRset with A_TEST missing");
        assertTrue(rrsets.stream().anyMatch(r -> r.contains(AAAA_1_TEST) && r.contains(AAAA_2_TEST)),
                "RRset with both AAAA records missing");
        assertTrue(rrsets.stream().anyMatch(r -> r.contains(A_WILD)), "RRset with wildcard A missing");
        assertTrue(rrsets.stream().anyMatch(r -> r.contains(TXT_WILD)), "RRset with wildcard TXT missing");
    }

    /**
     * Ensures that the iterator behaves according to the {@link Iterator} contract:
     * after {@code hasNext()} returns {@code false}, a subsequent {@code next()}
     * call must throw {@link NoSuchElementException}.
     */
    @Test
    void iteratorExhaustionThrowsNoSuchElementException() {
        Iterator<RRset> it = zone.iterator();

        // Exhaust the iterator
        while (it.hasNext()) {
            it.next();
        }

        assertFalse(it.hasNext(), "Iterator should report no more elements");

        assertThrows(NoSuchElementException.class, it::next,
                "Calling next() on an exhausted iterator must throw NoSuchElementException");
    }
}
```
```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Zone#AXFR()} – the iterator used for full zone transfers.
 *
 * <p>The tests verify that:
 *
 * <ul>
 *   <li>The AXFR iterator returns the same RRsets as the regular {@link Zone#iterator()}
 *       (i.e. a complete snapshot of the zone).</li>
 *   <li>Calling {@code remove()} on the AXFR iterator is not supported and throws
 *       {@link IllegalStateException}.</li>
 *   <li>When the iterator is exhausted, {@code next()} throws {@link NoSuchElementException}.</li>
 *   <li>The iterator is weakly‑consistent – modifications to the zone after the iterator is
 *       obtained do not cause a {@link java.util.ConcurrentModificationException}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Zone_AXFR_Test {

    /* --------------------------------------------------------------------- *
     *  Shared test data – created once per test method (see @BeforeEach).   *
     * --------------------------------------------------------------------- */
    private Name zoneName;
    private SOARecord soa;
    private NSRecord ns1;
    private NSRecord ns2;
    private ARecord aTest;
    private AAAARecord aaaaTest;
    private ARecord aUnique;
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        zoneName = Name.fromConstantString("example.");
        soa = new SOARecord(
                zoneName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostadmin", zoneName),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);
        ns1 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        ns2 = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

        InetAddress localhost = InetAddress.getByName("127.0.0.1");
        aTest = new ARecord(new Name("test", zoneName), DClass.IN, 3600L, localhost);
        aaaaTest = new AAAARecord(new Name("test", zoneName), DClass.IN, 3600L, InetAddress.getByName("::1"));
        aUnique = new ARecord(new Name("unique", zoneName), DClass.IN, 3600L, InetAddress.getByName("127.0.0.3"));

        Record[] records = new Record[] { soa, ns1, ns2, aTest, aaaaTest, aUnique };
        zone = new Zone(zoneName, records);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /** The AXFR iterator must return a snapshot containing every RRset in the zone. */
    @Test
    void axfrIteratesAllRRsets() {
        // Collect RRsets via the normal iterator (reference snapshot)
        List<RRset> expected = new ArrayList<>();
        for (Iterator<RRset> it = zone.iterator(); it.hasNext(); ) {
            expected.add(it.next());
        }

        // Collect RRsets via the AXFR iterator
        List<RRset> actual = new ArrayList<>();
        for (Iterator<RRset> it = zone.AXFR(); it.hasNext(); ) {
            actual.add(it.next());
        }

        assertThat(actual)
                .as("AXFR iterator should return the same number of RRsets as the regular iterator")
                .hasSameSizeAs(expected);

        // The order of RRsets is defined by ZoneIterator – we compare element‑wise.
        assertThat(actual)
                .as("AXFR iterator should return RRsets in the same order as the regular iterator")
                .containsExactlyElementsOf(expected);
    }

    /** {@code remove()} is not supported on the AXFR iterator. */
    @Test
    void axfrIteratorRemoveNotSupported() {
        Iterator<RRset> it = zone.AXFR();
        assertThatThrownBy(it::remove)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("remove");
    }

    /** After the iterator is exhausted, {@code next()} must throw {@link NoSuchElementException}. */
    @Test
    void axfrIteratorNextAfterLastFails() {
        Iterator<RRset> it = zone.AXFR();
        while (it.hasNext()) {
            it.next();
        }
        assertThatThrownBy(it::next)
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("No more elements");
    }

    /**
     * The iterator returned by {@code AXFR()} is weakly‑consistent.
     *
     * <p>We obtain the iterator, then add a new record to the zone and continue iterating.
     * The iterator must not throw {@link java.util.ConcurrentModificationException}.
     * The newly added record may or may not appear – both behaviours are allowed – but the
     * iterator must remain functional.
     */
    @Test
    void axfrIsWeaklyConsistent() {
        Iterator<RRset> it = zone.AXFR();

        // Consume the first RRset (SOA) to ensure the iterator is in use.
        assertThat(it.hasNext()).isTrue();
        RRset first = it.next();
        assertThat(first).isNotNull();

        // Add a new record after the iterator has been created.
        ARecord extra = new ARecord(new Name("extra", zoneName), DClass.IN, 3600L,
                InetAddress.getByName("127.0.0.99"));
        assertDoesNotThrow(() -> zone.addRecord(extra));

        // Continue iterating – we only assert that no CME is thrown.
        assertDoesNotThrow(() -> {
            while (it.hasNext()) {
                it.next();
            }
        });
    }
}
```
```java
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link Zone#iterator()} – the default iterator that walks through the RRsets in a zone.
 *
 * The test data mirrors the data used in {@code ZoneTest} and therefore provides a
 * realistic zone containing an SOA, two NS records and several A/AAAA/TXT records.
 *
 * The iterator is expected to:
 * <ul>
 *   <li>return the SOA first, then the NS set, followed by the remaining RRsets</li>
 *   <li>throw {@link IllegalStateException} when {@code remove()} is called before {@code next()}</li>
 *   <li>prevent removal of the NS RRset (the zone must always contain at least one NS)</li>
 *   <li>allow removal of other RRsets (e.g. an A record)</li>
 *   <li>throw {@link NoSuchElementException} when {@code next()} is called after the iterator is exhausted</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Zone_iterator_Test {

    /* --------------------------------------------------------------------- *
     *  Test fixtures – created once per test method (via @BeforeEach)      *
     * --------------------------------------------------------------------- */
    private Name ZONE_NAME;
    private SOARecord SOA1;
    private NSRecord NS1;
    private NSRecord NS2;
    private ARecord A_UNIQUE;
    private ARecord A_TEST;
    private AAAARecord AAAA_1_TEST;
    private AAAARecord AAAA_2_TEST;
    private ARecord A_WILD;
    private TXTRecord TXT_WILD;
    private Zone ZONE;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // zone name
        ZONE_NAME = Name.fromConstantString("example.");

        // mandatory SOA
        SOA1 = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("nameserver."),
                new Name("hostadmin", ZONE_NAME),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);

        // two NS records – the iterator will combine them into a single RRset
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));

        // a few address records
        InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
        InetAddress localhost6a = InetAddress.getByName("::1");
        InetAddress localhost6b = InetAddress.getByName("::2");

        A_UNIQUE = new ARecord(new Name("unique", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
        A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
        AAAA_1_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
        AAAA_2_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
        A_WILD = new ARecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
        TXT_WILD = new TXTRecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, "sometext");

        // build the zone – the constructor validates the presence of SOA & NS
        Record[] records = new Record[]{
                SOA1, NS1, NS2,
                A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST,
                A_WILD, TXT_WILD
        };
        ZONE = new Zone(ZONE_NAME, records);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the Zone instance is garbage‑collected.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /* --------------------------------------------------------------------- *
     *  Tests                                                               *
     * --------------------------------------------------------------------- */

    @Test
    void iteratorOrderAndContent() {
        Iterator<RRset> it = ZONE.iterator();

        // first RRset must be the SOA
        assertThat(it).isNotNull();
        assertThat(it.hasNext()).isTrue();
        RRset first = it.next();
        assertThat(first).isEqualTo(new RRset(SOA1));

        // second RRset must contain both NS records
        assertThat(it.hasNext()).isTrue();
        RRset second = it.next();
        assertThat(second).isEqualTo(ZONE.getNS());

        // third RRset – we know there is at least one A record (A_UNIQUE)
        assertThat(it.hasNext()).isTrue();
        RRset third = it.next();
        // we cannot guarantee order of the remaining RRsets, but the set must contain the A_UNIQUE record
        assertThat(third).contains(A_UNIQUE);
    }

    @Test
    void iteratorRemoveBeforeNextFails() {
        Iterator<RRset> it = ZONE.iterator();
        assertThatThrownBy(it::remove)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not at");
    }

    @Test
    void iteratorRemoveNsFails() {
        Iterator<RRset> it = ZONE.iterator();

        // advance to the NS RRset (second element)
        it.next(); // SOA
        assertThat(it.hasNext()).isTrue();
        RRset nsSet = it.next();
        assertThat(nsSet).isEqualTo(ZONE.getNS());

        // attempt to remove the NS set – the implementation forbids this
        assertThatThrownBy(it::remove)
                .hasMessageContaining("remove all NS");
    }

    @Test
    void iteratorRemoveARecordSucceeds() {
        Iterator<RRset> it = ZONE.iterator();

        // skip SOA and NS
        it.next(); // SOA
        it.next(); // NS

        // next RRset contains A records (including A_TEST)
        assertThat(it.hasNext()).isTrue();
        RRset aSet = it.next();
        assertThat(aSet).contains(A_TEST);

        // remove this RRset – the underlying zone should no longer have the A record
        it.remove();

        // verify removal via the public API
        assertThat(ZONE.findExactMatch(A_TEST.getName(), Type.A)).isNull();
    }

    @Test
    void iteratorNextAfterExhaustionFails() {
        Iterator<RRset> it = ZONE.iterator();

        // exhaust the iterator
        while (it.hasNext()) {
            it.next();
        }

        // now next() must throw NoSuchElementException
        assertThatThrownBy(it::next)
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("No more elements");
    }
}
```
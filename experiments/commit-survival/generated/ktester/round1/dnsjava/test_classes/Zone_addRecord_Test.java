package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.mockito.Mock;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class Zone_addRecord_Test {

    /**
     * Common zone origin used for all tests.
     */
    private static Name origin;

    /**
     * A sample IPv4 address used for {@link ARecord}s.
     */
    private static InetAddress localhost4;

    /**
     * First A record used in the “existing RRset” test.
     */
    private static ARecord aRecord1;

    /**
     * Second A record (same name/type) used in the “existing RRset” test.
     */
    private static ARecord aRecord2;

    /**
     * Fresh zone instance for each test.
     */
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        origin = new Name("example.");
        localhost4 = InetAddress.getByName("127.0.0.1");
        // Records share the same name and type (A) but have different TTLs to make them distinct.
        aRecord1 = new ARecord(new Name("test", origin), DClass.IN, 3600, localhost4);
        aRecord2 = new ARecord(new Name("test", origin), DClass.IN, 7200, localhost4);
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create an empty zone (no initial records) for each test.
        zone = new Zone(origin, new Record[0]);
    }

    /**
     * Helper that mirrors the one used in the original ZoneTest for convenience.
     */
    private static java.util.List<RRset> oneRRset(Record... records) {
        return java.util.Collections.singletonList(new RRset(records));
    }

    /**
     * Adding a record when no RRset exists should create a new RRset containing that record.
     */
    @Test
    void addRecordCreatesNewRRset() throws IOException {
        // Act
        zone.addRecord(aRecord1);
        // Verify
        SetResponse response = zone.findRecords(aRecord1.getName(), Type.A);
        assertTrue(response.isSuccessful(), "Response should be successful");
        assertEquals(oneRRset(aRecord1), response.answers(), "A new RRset containing the added record should be returned");
    }

    /**
     * Adding a record for a name/type that already exists should add the record to the existing
     * RRset.
     */
    @Test
    void addRecordAppendsToExistingRRset() throws IOException {
        // Arrange: first add aRecord1 so that an RRset exists
        zone.addRecord(aRecord1);
        // Act: add a second record with the same name and type
        zone.addRecord(aRecord2);
        // Verify: the RRset now contains both records
        SetResponse response = zone.findRecords(aRecord1.getName(), Type.A);
        assertTrue(response.isSuccessful(), "Response should be successful");
        assertEquals(oneRRset(aRecord1, aRecord2), response.answers(), "RRset should contain both added records");
    }

    /**
     * Adding a record with the same name but a different type creates a distinct RRset.
     */
    @Test
    void addRecordSameNameDifferentType() throws IOException {
        // existing A record
        zone.addRecord(aRecord1);
        // MX record with the same name
        MXRecord mx = new MXRecord(new Name("example.", origin), DClass.IN, 3600, 10, new Name("mail.example.", origin));
        zone.addRecord(mx);
        // Verify A RRset still contains only the A record
        SetResponse aResp = zone.findRecords(aRecord1.getName(), Type.A);
        assertTrue(aResp.isSuccessful());
        assertEquals(oneRRset(aRecord1), aResp.answers());
        // Verify MX RRset exists and contains the MX record
        SetResponse mxResp = zone.findRecords(mx.getName(), Type.MX);
        assertTrue(mxResp.isSuccessful());
        assertEquals(oneRRset(mx), mxResp.answers());
    }

    /**
     * Adding a {@code null} record must throw {@link NullPointerException}.
     */
    @Test
    void addRecord_NullRecord_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> zone.addRecord(null), "addRecord(null) should throw NullPointerException");
    }

    /**
     * Adding the same record twice must not create a duplicate inside the RRset.
     */
    @Test
    void addRecord_DuplicateRecord_NoDuplicateInRRset() throws IOException {
        zone.addRecord(aRecord1);
        // duplicate
        zone.addRecord(aRecord1);
        SetResponse resp = zone.findRecords(aRecord1.getName(), Type.A);
        assertTrue(resp.isSuccessful());
        // The RRset should contain exactly one record
        List<RRset> rrsets = resp.answers();
        assertEquals(1, rrsets.size());
        assertEquals(1, rrsets.get(0).size(), "Duplicate addition must not increase the number of records in the RRset");
    }

    /**
     * Concurrent invocations must not lose any records thanks to the internal synchronization.
     */
    @Test
    void addRecord_ConcurrentAdditions() throws Exception {
        // Two distinct A records for the same name
        ARecord r1 = new ARecord(new Name("concurrent", origin), DClass.IN, 3600, InetAddress.getByName("9.9.9.9"));
        ARecord r2 = new ARecord(new Name("concurrent", origin), DClass.IN, 3600, InetAddress.getByName("10.10.10.10"));
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        Runnable task1 = () -> {
            try {
                startLatch.await();
                zone.addRecord(r1);
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        };
        Runnable task2 = () -> {
            try {
                startLatch.await();
                zone.addRecord(r2);
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        };
        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        t1.start();
        t2.start();
        // Release both threads simultaneously
        startLatch.countDown();
        // Wait for both to finish (with a safety timeout)
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Threads did not finish in time");
        SetResponse resp = zone.findRecords(r1.getName(), Type.A);
        assertTrue(resp.isSuccessful());
        List<RRset> rrsets = resp.answers();
        assertEquals(1, rrsets.size());
        RRset set = rrsets.get(0);
        assertEquals(2, set.size(), "Both concurrent records must be present in the RRset");
        assertTrue(set.contains(r1));
        assertTrue(set.contains(r2));
    }

    /**
     * Adding a record whose {@code getName()} returns {@code null} must throw NPE.
     */
    @Test
    void addRecord_NullName_ThrowsNPE() {
        Record mock = Mockito.mock(Record.class);
        Mockito.when(mock.getName()).thenReturn(null);
        Mockito.when(mock.getRRsetType()).thenReturn(Type.A);
        // other methods are not used by addRecord, but we stub them to avoid unexpected NPEs
        Mockito.when(mock.getDClass()).thenReturn(DClass.IN);
        Mockito.when(mock.getTTL()).thenReturn(3600L);
        assertThrows(NullPointerException.class, () -> zone.addRecord(mock), "Record with null name should cause NullPointerException");
    }

    /**
     * Adding a record with an illegal (negative) type must result in IllegalArgumentException.
     */
    @Test
    void addRecord_InvalidType_ThrowsIllegalArgumentException() {
        Record mock = Mockito.mock(Record.class);
        Mockito.when(mock.getName()).thenReturn(new Name("badtype", origin));
        Mockito.when(mock.getRRsetType()).thenReturn(-1);
        Mockito.when(mock.getDClass()).thenReturn(DClass.IN);
        Mockito.when(mock.getTTL()).thenReturn(3600L);
        assertThrows(IllegalArgumentException.class, () -> zone.addRecord(mock), "Negative DNS type should trigger IllegalArgumentException");
    }

    /**
     * Adding a record with a type outside the 16‑bit range must trigger IllegalArgumentException.
     */
    @Test
    void addRecord_UnsupportedRRsetType_ThrowsIllegalArgumentException() {
        Record mock = Mockito.mock(Record.class);
        Mockito.when(mock.getName()).thenReturn(new Name("toolarge", origin));
        // outside valid range
        Mockito.when(mock.getRRsetType()).thenReturn(65536);
        Mockito.when(mock.getDClass()).thenReturn(DClass.IN);
        Mockito.when(mock.getTTL()).thenReturn(3600L);
        assertThrows(IllegalArgumentException.class, () -> zone.addRecord(mock), "Type > 65535 should trigger IllegalArgumentException");
    }

    /**
     * Adding a record whose data field is null must cause IllegalArgumentException from RRset.
     */
    @Test
    void addRecord_RecordWithNullData_ThrowsIllegalArgumentException() {
        // ARecord constructor validates its address, so we use a mock Record to simulate null data.
        Record mock = Mockito.mock(Record.class);
        Mockito.when(mock.getName()).thenReturn(new Name("nulldata", origin));
        Mockito.when(mock.getRRsetType()).thenReturn(Type.A);
        Mockito.when(mock.getDClass()).thenReturn(DClass.IN);
        Mockito.when(mock.getTTL()).thenReturn(3600L);
        // Simulate that the record's data is null – the library checks this in RRset.addRR().
        Mockito.when(mock.rdataToString()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> zone.addRecord(mock), "Record with null data should cause IllegalArgumentException");
    }
}

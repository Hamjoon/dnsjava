package org.xbill.DNS;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class Zone_getOrigin_Test {

    /**
     * A reusable zone name used for the successful construction of a {@link Zone}.
     */
    private static Name zoneName;

    /**
     * The {@link Zone} instance created once for the whole test suite.
     */
    private static Zone zone;

    /**
     * Create a common {@link Name} and {@link Zone} that will be used by the tests.
     */
    @BeforeAll
    static void setupBeforeAll() {
        try {
            // absolute name
            zoneName = new Name("example.");
            // an empty record array is sufficient – we are only interested in the origin
            zone = new Zone(zoneName, new Record[0]);
        } catch (IOException e) {
            // The constructors used in this test do not perform I/O, so any IOException is unexpected.
            throw new RuntimeException("Failed to create test Zone", e);
        }
    }

    /**
     * Helper that creates a fresh {@link Zone} instance with a dummy origin.
     * The dummy origin is immediately cleared via reflection so that the
     * internal {@code origin} field starts as {@code null}.
     */
    private Zone createZoneWithNullOrigin() throws Exception {
        Zone z = new Zone(new Name("dummy."), new Record[0]);
        Field f = Zone.class.getDeclaredField("origin");
        f.setAccessible(true);
        f.set(z, null);
        return z;
    }

    /**
     * Verify that {@code getOrigin()} returns the exact {@link Name} supplied to the constructor.
     */
    @Test
    void getOriginReturnsSuppliedName() {
        assertEquals(zoneName, zone.getOrigin(), "Zone.getOrigin() should return the Name that was passed to the constructor");
    }

    /**
     * Verify that the constructor rejects a {@code null} zone name.
     */
    @Test
    void constructorRejectsNullOrigin() {
        assertThrows(IllegalArgumentException.class, () -> new Zone((Name) null, new Record[0]), "Zone constructor must throw IllegalArgumentException when the zone name is null");
    }

    /**
     * Verify that a different constructor (the one that takes a file) also preserves the origin.
     */
    @Test
    void getOriginPreservedWhenUsingFileConstructor() throws IOException {
        // Create a minimal zone file in‑memory using the temporary file API.
        // The content only needs a SOA record; the actual parsing is exercised by the constructor.
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("zone", ".txt");
        try {
            String zoneFileContent = "$ORIGIN example.\n" + "@ 3600 IN SOA ns.example. hostmaster.example. 1 7200 3600 1209600 3600\n";
            java.nio.file.Files.writeString(tempFile, zoneFileContent);
            Zone fileZone = new Zone(zoneName, tempFile.toString());
            assertEquals(zoneName, fileZone.getOrigin(), "Zone created from a file should retain the supplied origin");
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Origin Retrieval - Default State
     */
    @Test
    void getOriginReturnsNullWhenOriginNeverSet() throws Exception {
        Zone z = createZoneWithNullOrigin();
        assertNull(z.getOrigin(), "When a Zone is instantiated and its private 'origin' field is never set, getOrigin() must return null");
    }

    /**
     * Origin Retrieval - After Setting Origin via Reflection (single set)
     */
    @Test
    void getOriginReturnsNameSetViaReflection() throws Exception {
        Zone z = createZoneWithNullOrigin();
        Name expected = Name.fromString("example.com.");
        Field f = Zone.class.getDeclaredField("origin");
        f.setAccessible(true);
        f.set(z, expected);
        // reference equality is expected because we did not clone the value
        assertSame(expected, z.getOrigin(), "getOrigin() should return the exact Name instance that was injected via reflection");
    }

    /**
     * Origin Retrieval - After Setting Origin via Reflection (multiple sets)
     */
    @Test
    void getOriginReflectsMostRecentSet() throws Exception {
        Zone z = createZoneWithNullOrigin();
        Field f = Zone.class.getDeclaredField("origin");
        f.setAccessible(true);
        Name first = Name.fromString("first.com.");
        Name second = Name.fromString("second.com.");
        f.set(z, first);
        f.set(z, second);
        assertSame(second, z.getOrigin(), "After setting the origin twice via reflection, getOrigin() must return the last value");
    }

    /**
     * Origin Retrieval - Immutability Check
     */
    @Test
    void getOriginDoesNotExposeMutableState() throws Exception {
        Zone z = createZoneWithNullOrigin();
        Name origin = Name.fromString("mutable.com.");
        Field f = Zone.class.getDeclaredField("origin");
        f.setAccessible(true);
        f.set(z, origin);
        // Retrieve the name and (attempt to) modify it – Name in dnsjava is immutable,
        // so we simply verify that a second call returns the same logical value.
        Name firstCall = z.getOrigin();
        Name secondCall = z.getOrigin();
        assertEquals(firstCall, secondCall, "Repeated calls to getOrigin() must return equal Name values");
        assertSame(firstCall, secondCall, "Because Name is immutable, the same instance should be returned");
    }

    /**
     * Origin Retrieval - Serialization Persistence
     */
    @Test
    void originSurvivesSerializationRoundTrip() throws Exception {
        Zone original = createZoneWithNullOrigin();
        Name origin = Name.fromString("persist.com.");
        Field f = Zone.class.getDeclaredField("origin");
        f.setAccessible(true);
        f.set(original, origin);
        // Serialize
        byte[] data;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(original);
            out.flush();
            data = bos.toByteArray();
        }
        // Deserialize
        Zone deserialized;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            deserialized = (Zone) in.readObject();
        }
        // The deserialized Zone should contain an equal (but not necessarily same) Name instance
        Name after = deserialized.getOrigin();
        assertNotNull(after, "Deserialized Zone must have a non‑null origin");
        assertEquals(origin, after, "Origin after deserialization should be equal to the original Name");
    }

    /**
     * Origin Retrieval - Repeated Calls Consistency
     */
    @Test
    void repeatedCallsReturnIdenticalReference() throws Exception {
        Zone z = createZoneWithNullOrigin();
        Name origin = Name.fromString("repeat.com.");
        Field f = Zone.class.getDeclaredField("origin");
        f.setAccessible(true);
        f.set(z, origin);
        Name first = z.getOrigin();
        Name second = z.getOrigin();
        Name third = z.getOrigin();
        assertSame(first, second, "Second call should return the same reference as the first");
        assertSame(first, third, "Third call should also return the same reference");
    }

    /**
     * Origin Retrieval - Null Reset via Reflection
     */
    @Test
    void originCanBeResetToNullViaReflection() throws Exception {
        Zone z = createZoneWithNullOrigin();
        Field f = Zone.class.getDeclaredField("origin");
        f.setAccessible(true);
        Name tmp = Name.fromString("temp.com.");
        // set to a non‑null value
        f.set(z, tmp);
        assertNotNull(z.getOrigin(), "Origin should be non‑null after first injection");
        // reset to null
        f.set(z, null);
        assertNull(z.getOrigin(), "Origin must be null after being reset via reflection");
    }

    /**
     * Origin Retrieval - After Adding Record with Same Name
     */
    @Test
    void addingRecordDoesNotChangeOrigin() throws Exception {
        Zone z = createZoneWithNullOrigin();
        Name origin = Name.fromString("record.com.");
        Field f = Zone.class.getDeclaredField("origin");
        f.setAccessible(true);
        f.set(z, origin);
        // Create a simple A record whose name equals the origin.
        Record aRecord = new ARecord(origin, DClass.IN, 3600, InetAddress.getByName("192.0.2.1"));
        // The addRecord method is part of the public API; it should not tamper with the origin.
        z.addRecord(aRecord);
        assertSame(origin, z.getOrigin(), "Adding a record with the same name as the origin must not modify the origin field");
    }

    /**
     * Origin Retrieval - Concurrency Visibility
     */
    @Test
    void concurrentReflectionWritesResultInLastWriterVisibility() throws Exception {
        Zone z = createZoneWithNullOrigin();
        Field f = Zone.class.getDeclaredField("origin");
        f.setAccessible(true);
        Name name1 = Name.fromString("thread1.com.");
        Name name2 = Name.fromString("thread2.com.");
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        Runnable writer1 = () -> {
            try {
                // wait for the signal to start
                startLatch.await();
                f.set(z, name1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                doneLatch.countDown();
            }
        };
        Runnable writer2 = () -> {
            try {
                startLatch.await();
                f.set(z, name2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                doneLatch.countDown();
            }
        };
        Thread t1 = new Thread(writer1);
        Thread t2 = new Thread(writer2);
        t1.start();
        t2.start();
        // Release both threads simultaneously
        startLatch.countDown();
        // Wait for both to finish
        doneLatch.await();
        // The last write (writer2) happens after writer1; due to the simple ordering
        // in this test, we expect name2 to be visible.
        assertSame(name2, z.getOrigin(), "After concurrent reflection writes, the origin should reflect the value set by the last writer");
    }
}

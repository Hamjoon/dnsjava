```java
package org.xbill.DNS;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Zone_getDClass_Test {
    private static final String ZONE_TEXT = "example.";

    private Name zoneName;

    private SOARecord soa;

    private NSRecord ns;

    private Zone zone;

    /**
     * Create a minimal, valid zone before each test.
     */
    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build the zone name (absolute)
        zoneName = Name.fromConstantString(ZONE_TEXT);
        // Minimal required records: one SOA and at least one NS.
        // The values are arbitrary but must be syntactically correct.
        soa = new SOARecord(
                zoneName,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns.example."),
                new Name("hostmaster", zoneName),
                1,
                7200L,
                3600L,
                1209600L,
                3600L);
        ns = new NSRecord(zoneName, DClass.IN, 300L, Name.fromConstantString("ns.example."));
        // Construct the zone using the var‑args constructor.
        zone = new Zone(zoneName, soa, ns);
    }

    /**
     * Ensure that the test fixture itself does not leak resources.
     */
    @AfterEach
    void teardownAfterEach() {
        // No explicit resources to release; method kept for symmetry with the template.
        zone = null;
        soa = null;
        ns = null;
        zoneName = null;
    }

    /**
     * Verify that {@link Zone#getDClass()} always returns {@link DClass#IN}.
     */
    @Test
    void getDClassReturnsIN() {
        int dclass = zone.getDClass();
        assertEquals(DClass.IN, dclass, "Zone#getDClass() should return DClass.IN");
    }

    /**
     * Calling the method repeatedly must keep returning the same constant.
     */
    @Test
    void getDClassIsIdempotent() {
        assertAll(
                () -> assertEquals(DClass.IN, zone.getDClass()),
                () -> assertEquals(DClass.IN, zone.getDClass()),
                () -> assertEquals(DClass.IN, zone.getDClass()));
    }

    /**
     * Ensure that serialization does not affect the returned class.
     */
    @Test
    void getDClassAfterSerialization() throws IOException, ClassNotFoundException {
        // Serialize
        byte[] data;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(zone);
            out.flush();
            data = bos.toByteArray();
        }
        // Deserialize
        Zone deserialized;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            deserialized = (Zone) in.readObject();
        }
        assertEquals(DClass.IN, deserialized.getDClass(),
                "Deserialized Zone should still return DClass.IN");
    }

    /**
     * Calling {@code toString()} must not change the result of {@code getDClass()}.
     */
    @Test
    void getDClassAfterToString() {
        // toString may build an internal representation; we ignore its output.
        String ignored = zone.toString();
        // sanity check
        assertNotNull(ignored);
        assertEquals(DClass.IN, zone.getDClass(),
                "Zone#getDClass() must remain DClass.IN after toString()");
    }

    /**
     * Exhausting the zone iterator must not affect the class constant.
     */
    @Test
    void getDClassAfterIterator() {
        Iterator<RRset> it = zone.iterator();
        while (it.hasNext()) {
            // consume
            it.next();
        }
        assertEquals(DClass.IN, zone.getDClass(),
                "Zone#getDClass() must remain DClass.IN after iterator consumption");
    }

    /**
     * Concurrent invocations of {@code getDClass()} must all return the same constant.
     */
    @Test
    void getDClassConcurrentAccess() throws InterruptedException, ExecutionException {
        int threadCount = 20;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> zone.getDClass());
        }
        List<Future<Integer>> results = exec.invokeAll(tasks);
        exec.shutdown();
        for (Future<Integer> f : results) {
            assertEquals(DClass.IN, f.get(),
                    "Each concurrent call must return DClass.IN");
        }
    }

    /**
     * Subclass that does not override {@code getDClass()} should inherit the constant.
     */
    @Test
    void getDClassSubclassInheritance() {
        class MyZone extends Zone {
            MyZone(Name name, SOARecord soa, NSRecord ns) {
                super(name, soa, ns);
            }
        }
        Zone myZone = new MyZone(zoneName, soa, ns);
        assertEquals(DClass.IN, myZone.getDClass(),
                "Subclassed Zone must inherit getDClass() returning DClass.IN");
    }

    /**
     * Adding a large number of RRsets must not affect the returned class value.
     */
    @Test
    void getDClassAfterAddingMultipleRRsets() throws IOException {
        // Add 100 A records with distinct names.
        for (int i = 0; i < 100; i++) {
            Name name = Name.fromConstantString("host" + i + ".example.");
            ARecord a = new ARecord(name, DClass.IN, 300L,
                    InetAddress.getByName("192.0.2." + i));
            zone.addRecord(a);
        }
        assertEquals(DClass.IN, zone.getDClass(),
                "Zone#getDClass() must stay DClass.IN after many RRsets are added");
    }

    /**
     * Removing all RRsets (leaving the zone empty) must not affect the class constant.
     */
    @Test
    void getDClassAfterRemovingAllRRsets() throws IOException {
        // First add a few records so we have something to remove.
        List<Record> added = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Name name = Name.fromConstantString("host" + i + ".example.");
            ARecord a = new ARecord(name, DClass.IN, 300L,
                    InetAddress.getByName("192.0.2." + i));
            zone.addRecord(a);
            added.add(a);
        }
        // Remove each record.
        for (Record r : added) {
            zone.removeRecord(r);
        }
        // Also remove the initial NS and SOA (allowed by API) to make the zone empty.
        zone.removeRecord(soa);
        zone.removeRecord(ns);
        assertEquals(DClass.IN, zone.getDClass(),
                "Zone#getDClass() must stay DClass.IN after all RRsets are removed");
    }

    /**
     * Invoking AXFR (zone transfer iterator) must not change the class constant.
     */
    @Test
    void getDClassAfterAXFRInvocation() {
        Iterator<RRset> axfrIt = zone.AXFR();
        // Consume a few elements if present; the iterator may be empty for a minimal zone.
        int consumed = 0;
        while (axfrIt.hasNext() && consumed < 5) {
            axfrIt.next();
            consumed++;
        }
        assertEquals(DClass.IN, zone.getDClass(),
                "Zone#getDClass() must stay DClass.IN after AXFR() invocation");
    }
}
```
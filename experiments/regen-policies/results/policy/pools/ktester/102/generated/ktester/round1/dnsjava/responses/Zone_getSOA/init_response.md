```java
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Zone#getSOA()}.
 *
 * <p>The tests verify that the SOA record supplied at construction time is returned,
 * that adding a new SOA record updates the value returned by {@code getSOA()},
 * and that the SOA survives a serialization round‑trip.
 */
@ExtendWith(MockitoExtension.class)
class Zone_getSOA_Test {

    private static Name ZONE_NAME;
    private static SOARecord SOA1;
    private static SOARecord SOA2;
    private static NSRecord NS1;
    private static NSRecord NS2;
    private Zone zone;

    @BeforeAll
    static void setupBeforeAll() throws TextParseException {
        // constant zone name used by all tests
        ZONE_NAME = Name.fromConstantString("example.");
        // first SOA record (will be the initial one in the zone)
        SOA1 = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns1.example."),
                new Name("hostadmin", ZONE_NAME),
                1,
                21600L,
                7200L,
                2160000L,
                3600L);
        // a second SOA record used to test replacement behaviour
        SOA2 = new SOARecord(
                ZONE_NAME,
                DClass.IN,
                3600L,
                Name.fromConstantString("ns2.example."),
                new Name("zoneadmin", ZONE_NAME),
                2,
                21600L,
                7200L,
                2160000L,
                3600L);
        // at least one NS record is required for a valid zone
        NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns1.example."));
        NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("ns2.example."));
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Build a zone that contains SOA1 and two NS records.
        Record[] records = new Record[] { SOA1, NS1, NS2 };
        zone = new Zone(ZONE_NAME, records);
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up for now
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up for now
    }

    /** The SOA record supplied at construction must be returned by {@code getSOA()}. */
    @Test
    void getSOA_returnsInitialSOA() {
        assertEquals(SOA1, zone.getSOA(),
                "Zone#getSOA should return the SOA record that was provided during construction");
    }

    /**
     * Adding a new SOA record should replace the existing one.
     * The {@code addRecord} method is part of the public API, therefore we can use it
     * to inject a second SOA and verify that {@code getSOA()} reflects the change.
     */
    @Test
    void getSOA_afterAddingAnotherSOA_returnsNewSOA() throws IOException {
        // Add a second SOA record – the zone implementation replaces the stored SOA.
        zone.addRecord(SOA2);
        assertEquals(SOA2, zone.getSOA(),
                "After adding a new SOA record, Zone#getSOA should return the newly added record");
    }

    /** The SOA record must survive a serialization / deserialization round‑trip. */
    @Test
    void getSOA_survivesSerialization() throws IOException, ClassNotFoundException {
        // Serialize the zone
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(zone);
        }

        // Deserialize the zone
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Zone deserialized;
        try (ObjectInputStream in = new ObjectInputStream(bis)) {
            deserialized = (Zone) in.readObject();
        }

        // Verify that the SOA record is still the same instance (equals) after deserialization
        assertEquals(SOA1, deserialized.getSOA(),
                "Deserialized Zone should retain the original SOA record");
    }
}
```
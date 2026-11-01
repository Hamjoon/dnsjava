{
    "test_cases": [
        {
            "group": "Multiple NS records – returns full copy",
            "cases": [
                {
                    "input": [],
                    "expected": "RRset instance containing all NS records that were added to the Zone, equal in content to the internal nsRRset, distinct object reference, and size matches the number of added NS records.",
                    "description": "Create a Zone, add three different NS records (e.g., ns1.example.com, ns2.example.com, ns3.example.com) via addRecord or addRRset. Call getNS() and verify that the returned RRset holds exactly three records, that it equals the internal nsRRset in terms of records, and that it is a different object (original != returned)."
                }
            ]
        },
        {
            "group": "NS RRset removed – returns empty RRset",
            "cases": [
                {
                    "input": [],
                    "expected": "RRset instance that is empty (size == 0) after the NS RRset has been removed from the Zone.",
                    "description": "Create a Zone, add an NS RRset, then remove it using removeRRset(Name, Type.NS). Invoke getNS() and verify that the returned RRset is empty and no exception is thrown."
                }
            ]
        },
        {
            "group": "Modification of internal nsRRset after getNS does not affect previous copy",
            "cases": [
                {
                    "input": [],
                    "expected": "Two RRset instances: the first returned by the initial getNS() remains unchanged after the internal nsRRset is later modified; the second getNS() reflects the updated internal state.",
                    "description": "Create a Zone, add a single NS record, call getNS() (firstCopy). Then add another NS record directly to the Zone (e.g., via addRecord). Call getNS() again (secondCopy). Verify that firstCopy still contains only the original record while secondCopy contains both records, demonstrating that each call returns a snapshot copy."
                }
            ]
        },
        {
            "group": "Serialization round‑trip preserves getNS behavior",
            "cases": [
                {
                    "input": [],
                    "expected": "After serializing and deserializing the Zone, getNS() returns an RRset equal to the original NS RRset (or empty if none were added) and is a distinct object.",
                    "description": "Create a Zone, add an NS RRset, serialize the Zone to a byte array, deserialize it back to a new Zone instance, then call getNS(). Verify that the returned RRset contains the same NS records as before serialization and that it is a new object."
                }
            ]
        },
        {
            "group": "Concurrent modification while reading – getNS returns a consistent snapshot",
            "cases": [
                {
                    "input": [],
                    "expected": "All concurrent calls to getNS() return RRset instances that are internally consistent (no partially‑added records) and either reflect the state before or after the concurrent add/remove operation, but never a corrupted mix.",
                    "description": "Start a thread that continuously adds NS records to a Zone with a short pause between each add. Simultaneously start multiple threads that repeatedly invoke getNS(). Collect the returned RRset sizes; they should be either the previous size or the new size, never an intermediate size that would indicate a partially constructed RRset."
                }
            ]
        },
        {
            "group": "Re‑entrancy of withReadLock – getNS called inside another read‑locked operation",
            "cases": [
                {
                    "input": [],
                    "expected": "getNS() completes successfully without deadlock and returns the expected RRset.",
                    "description": "Create a Zone with an NS RRset. Invoke withReadLock on the Zone (using reflection or a helper method) and, inside the supplied lambda, call getNS(). Verify that the method returns the correct copy of the NS RRset and no deadlock occurs, confirming that the read lock is re‑entrant for the same thread."
                }
            ]
        },
        {
            "group": "Zone contains other RRsets but no NS – getNS returns empty RRset",
            "cases": [
                {
                    "input": [],
                    "expected": "RRset instance that is empty (size == 0) while other RRsets (e.g., A, MX) exist in the Zone.",
                    "description": "Create a Zone, add an A record and an MX record using addRecord, but do not add any NS records. Call getNS() and verify that the returned RRset is empty and that the presence of other RRsets does not affect the result."
                }
            ]
        },
        {
            "group": "Adding a non‑NS record does not affect getNS output",
            "cases": [
                {
                    "input": [],
                    "expected": "RRset returned by getNS() remains empty (or unchanged) after adding records of other types.",
                    "description": "Create a fresh Zone, add a TXT record via addRecord, then call getNS(). Verify that the returned RRset is empty, demonstrating that getNS() only reflects NS records."
                }
            ]
        }
    ]
}
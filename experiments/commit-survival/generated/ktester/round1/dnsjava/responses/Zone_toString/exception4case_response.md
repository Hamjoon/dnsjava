{
    "test_cases": [
        {
            "group": "Null Origin Exception",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Create a Zone instance and, using reflection, set its internal origin field to null. Invoking toString() should throw a NullPointerException because the method attempts to format the $ORIGIN directive using a null Name."
                }
            ]
        },
        {
            "group": "Null RRset Map Exception",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "After constructing a Zone, force the internal map that stores RRsets (e.g., the TreeMap used by the zone) to null via reflection. Calling toString() will try to iterate over the map and raise a NullPointerException."
                }
            ]
        },
        {
            "group": "Add Null Record Leads to Exception",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Invoke zone.addRecord(null). The zone stores a null entry in its data structures. When toString() later iterates over the stored records it encounters the null and throws a NullPointerException."
                }
            ]
        },
        {
            "group": "Record With Null Name",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalArgumentException",
                    "description": "Create a Record (or subclass) whose name field is null and add it to the zone via addRecord(). The zone validates arguments on insertion, but the validation is deferred until toString() formats the record, causing an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Missing Mandatory SOA",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException",
                    "description": "Create a Zone, add an NS record but do NOT add an SOA record. The toMasterFile() implementation expects at least one SOA and throws IllegalStateException when it cannot find one."
                }
            ]
        },
        {
            "group": "Malformed Record Causes IO Failure",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException (wrapping IOException)",
                    "description": "Add a Record whose toString() implementation throws an IOException (simulated via a custom Record subclass). When toString() builds the master‑file string, the IOException is caught and re‑thrown as a RuntimeException. The test expects that RuntimeException."
                }
            ]
        },
        {
            "group": "Concurrent Modification During toString",
            "cases": [
                {
                    "input": [],
                    "expected": "ConcurrentModificationException",
                    "description": "Start a thread that continuously adds records to a Zone while the main test thread calls zone.toString(). Because toMasterFile() iterates over the internal collection without making a defensive copy, a ConcurrentModificationException may be thrown."
                }
            ]
        }
    ]
}
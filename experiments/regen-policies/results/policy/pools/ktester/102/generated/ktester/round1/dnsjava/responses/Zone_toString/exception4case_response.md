{
    "test_cases": [
        {
            "group": "RuntimeException propagation",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.IllegalStateException",
                    "description": "Create a subclass of Zone that overrides toMasterFile() to throw a new IllegalStateException. Calling toString() must propagate this unchecked exception unchanged."
                }
            ]
        },
        {
            "group": "ConcurrentModificationException during toString",
            "cases": [
                {
                    "input": [],
                    "expected": "java.util.ConcurrentModificationException",
                    "description": "Populate a Zone with several records, then start a background thread that adds/removes records while the main thread invokes zone.toString(). The internal iteration performed by toMasterFile() should detect the concurrent modification and throw ConcurrentModificationException, which must be propagated by toString()."
                }
            ]
        },
        {
            "group": "NullPointerException from internal state",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "Create a subclass of Zone that sets its internal data structures (e.g., the map that stores RRsets) to null before calling toString(). When toMasterFile() attempts to iterate over the null collection, a NullPointerException should be thrown and propagated."
                }
            ]
        }
    ]
}
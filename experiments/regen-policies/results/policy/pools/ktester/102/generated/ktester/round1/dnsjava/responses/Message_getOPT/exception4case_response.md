{
    "groups": [
        {
            "group": "Message.getOPT() - getSection returns null",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "A subclass of Message overrides getSection(int) to return null for the ADDITIONAL section. The for‑each loop in getOPT() attempts to iterate over a null reference, causing a NullPointerException."
                }
            ]
        },
        {
            "group": "Message.getOPT() - getSection throws unchecked exception",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.IllegalStateException",
                    "description": "A subclass of Message overrides getSection(int) to throw an IllegalStateException when called for the ADDITIONAL section. The exception propagates out of getOPT()."
                }
            ]
        },
        {
            "group": "Message.getOPT() - iterator throws ConcurrentModificationException",
            "cases": [
                {
                    "input": [],
                    "expected": "java.util.ConcurrentModificationException",
                    "description": "A custom List implementation is returned by getSection(Section.ADDITIONAL) whose iterator throws ConcurrentModificationException on the first call to hasNext(). This simulates a concurrent modification during iteration, which should be propagated by getOPT()."
                }
            ]
        }
    ]
}
{
    "group": "Message.toWire(DNSOutput) - edge cases and error handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "message",
                    "value": "Message instance whose `sections` field is **null** and whose header is a mock Header"
                },
                {
                    "parameter": "out",
                    "value": "Mock DNSOutput"
                }
            ],
            "expected": "NullPointerException thrown when `header.toWire(out)` tries to access `sections.length` (the method does not guard against a null `sections` array).",
            "description": "Verify that a Message with a null `sections` array causes an NPE, exercising the implicit null‑check before the loop."
        },
        {
            "input": [
                {
                    "parameter": "message",
                    "value": "Message instance with an **empty** `sections` array (length = 0) and a mock Header"
                },
                {
                    "parameter": "out",
                    "value": "Mock DNSOutput"
                }
            ],
            "expected": "Header.toWire(out) invoked exactly once; the for‑loop body is never entered; no Compression object is created; method completes without exception.",
            "description": "Confirm that when `sections.length` is zero the method only serialises the header and skips all looping logic."
        },
        {
            "input": [
                {
                    "parameter": "message",
                    "value": "Message instance with `sections` length = 2 where both entries are **non‑null empty lists** (e.g., `new ArrayList<>()`), and a mock Header"
                },
                {
                    "parameter": "out",
                    "value": "Mock DNSOutput"
                }
            ],
            "expected": "Header.toWire(out) called once; a Compression instance is created once; no Record.toWire calls because the lists are empty; method finishes without exception.",
            "description": "Exercise the path where sections are present but contain no records, ensuring the Compression object is still instantiated but not used."
        },
        {
            "input": [
                {
                    "parameter": "message",
                    "value": "Message instance with `sections` length = 3:\n  - sections[0] = null\n  - sections[1] = null\n  - sections[2] contains a single mock Record **R2**\n  Header is a mock Header"
                },
                {
                    "parameter": "out",
                    "value": "Mock DNSOutput"
                }
            ],
            "expected": "Header.toWire(out) called once; Compression created once; exactly one Record.toWire call: R2.toWire(out, 2, CompressionInstance); method completes normally.",
            "description": "Validate that the correct section index (2) is passed to Record.toWire when the record resides in the third section."
        },
        {
            "input": [
                {
                    "parameter": "message",
                    "value": "Message instance with `sections` length = 2 where sections[0] contains a mock Record **BadRec** whose `toWire` method throws a RuntimeException (e.g., new IllegalStateException(\"boom\")), and a mock Header"
                },
                {
                    "parameter": "out",
                    "value": "Mock DNSOutput"
                }
            ],
            "expected": "Header.toWire(out) invoked once; Compression created; BadRec.toWire(out, 0, CompressionInstance) invoked and throws IllegalStateException; the exception propagates out of `Message.toWire` unchanged.",
            "description": "Ensure that unchecked exceptions thrown by a Record during serialisation are not caught by `Message.toWire` and are allowed to bubble up."
        }
    ]
}
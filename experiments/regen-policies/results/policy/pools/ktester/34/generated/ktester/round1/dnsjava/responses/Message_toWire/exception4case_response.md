{
    "test_cases": [
        {
            "group": "Message.toWire(DNSOutput) - header null handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance with header == null and a non‑null empty sections array (e.g., new List[1] { new ArrayList<>() })"
                        },
                        {
                            "parameter": "out",
                            "value": "Mock DNSOutput (no behavior needed)"
                        }
                    ],
                    "expected": "NullPointerException thrown when invoking header.toWire(out)",
                    "description": "The Message has no Header; the method should attempt to call header.toWire and consequently throw an NPE."
                },
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance with a mock Header whose toWire(DNSOutput) method throws IllegalStateException(\"header fail\") and a non‑null sections array (any size)"
                        },
                        {
                            "parameter": "out",
                            "value": "Mock DNSOutput"
                        }
                    ],
                    "expected": "IllegalStateException with message \"header fail\" propagates out of Message.toWire",
                    "description": "Verifies that unchecked exceptions thrown by Header.toWire are not caught by Message.toWire and are re‑thrown to the caller."
                }
            ]
        },
        {
            "group": "Message.toWire(DNSOutput) - null Record element in section list",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance with a mock Header, sections array of length 1 where sections[0] = new ArrayList<>(Arrays.asList((Record) null)) (i.e., list containing a single null Record)"
                        },
                        {
                            "parameter": "out",
                            "value": "Mock DNSOutput"
                        }
                    ],
                    "expected": "NullPointerException thrown when the loop attempts to invoke rec.toWire on a null Record",
                    "description": "Ensures that a null element inside a non‑null section list triggers an NPE, confirming that the method does not guard against null Records."
                }
            ]
        }
    ]
}
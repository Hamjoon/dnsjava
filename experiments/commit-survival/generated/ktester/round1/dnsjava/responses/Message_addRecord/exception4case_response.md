{
    "tests": [
        {
            "group": "addRecord – sections array is null (NullPointerException)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"nullsections.example.\"), Type.A, DClass.IN, 300, InetAddress.getByName(\"12.12.12.12\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "0"
                        }
                    ],
                    "expected": "NullPointerException is thrown",
                    "description": "If the Message object's internal `sections` array is explicitly set to null (e.g., via reflection or a constructor that allows it), the first array access `sections[section]` triggers a NullPointerException before any other logic runs."
                }
            ]
        },
        {
            "group": "addRecord – immutable section list (UnsupportedOperationException)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"immutable.example.\"), Type.A, DClass.IN, 300, InetAddress.getByName(\"13.13.13.13\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "1"
                        }
                    ],
                    "expected": "UnsupportedOperationException is thrown",
                    "description": "When the target `sections[1]` entry is pre‑populated with an unmodifiable list such as `Collections.emptyList()` (or `Collections.unmodifiableList(new LinkedList<>())`), the call to `sections[section].add(r)` fails with an UnsupportedOperationException. The header count is still incremented before the exception is raised."
                }
            ]
        },
        {
            "group": "addRecord – header.incCount throws IllegalArgumentException for out‑of‑range section",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"header.illegal.arg.example.\"), Type.A, DClass.IN, 300, InetAddress.getByName(\"14.14.14.14\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "2"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown",
                    "description": "If the Message's `header` instance is replaced with a custom Header implementation that validates the section index and throws IllegalArgumentException for values it does not recognize (e.g., section 2 is considered invalid), `header.incCount(section)` will raise the exception before the record is added to the list."
                }
            ]
        }
    ]
}
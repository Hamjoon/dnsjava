{
    "test_cases": [
        {
            "group": "addRecord – sections array is null (NullPointerException)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"nullsections.example.\"), Type.A, DClass.IN, 3600, InetAddress.getByName(\"192.0.2.55\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "Message.SECTION_ANSWER"
                        }
                    ],
                    "expected": "NullPointerException is thrown because the internal `sections` array is null and the method attempts to read `sections[section]`.",
                    "description": "Force the `Message.sections` field to `null` (e.g., via reflection) before invoking `addRecord`. The method does not check for a null array, so accessing `sections[section]` should raise a NullPointerException."
                }
            ]
        },
        {
            "group": "addRecord – unmodifiable list in target section (UnsupportedOperationException)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"unmod.example.\"), Type.TXT, DClass.IN, 0, new TXTRecord(Name.fromString(\"unmod.example.\"), DClass.IN, 0, \"test\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "Message.SECTION_ANSWER"
                        }
                    ],
                    "expected": "UnsupportedOperationException is thrown when `sections[Message.SECTION_ANSWER].add(r)` is executed because the list is an unmodifiable view.",
                    "description": "Pre‑populate `Message.sections[Message.SECTION_ANSWER]` with `Collections.unmodifiableList(new ArrayList<>())`. When `addRecord` tries to add the record, the underlying list rejects the modification, resulting in an UnsupportedOperationException."
                }
            ]
        },
        {
            "group": "addRecord – header.incCount throws IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"badheader.example.\"), Type.SOA, DClass.IN, 0, new SOARecord(Name.fromString(\"badheader.example.\"), DClass.IN, 3600, Name.fromString(\"ns.example.\"), Name.fromString(\"hostmaster.example.\"), 2021010101L, 7200, 3600, 1209600, 3600))"
                        },
                        {
                            "parameter": "section",
                            "value": "Message.SECTION_AUTHORITY"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown by the overridden `Header.incCount` implementation for the supplied section.",
                    "description": "Replace the `Message.header` with a custom subclass of `Header` whose `incCount(int section)` method deliberately throws `IllegalArgumentException` for `Message.SECTION_AUTHORITY`. The method calls `header.incCount(section)` before adding the record, so the exception propagates."
                }
            ]
        }
    ]
}
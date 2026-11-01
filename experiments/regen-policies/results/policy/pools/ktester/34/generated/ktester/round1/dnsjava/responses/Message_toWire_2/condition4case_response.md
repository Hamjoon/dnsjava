{
    "testCases": [
        {
            "group": "Empty Message",
            "cases": [
                {
                    "input": [],
                    "expected": "A non‑null byte array of length 12 (the DNS header size) and the Message.size field set to 12.",
                    "description": "Calling toWire() on a freshly constructed Message that contains no sections, questions, or additional records should produce only the DNS header."
                }
            ]
        },
        {
            "group": "Message with a single Question record",
            "cases": [
                {
                    "input": [],
                    "expected": "A non‑null byte array whose length equals 12 (header) + the wire length of the Question record. The Message.size field must match that length.",
                    "description": "Add a simple A‑type question (e.g. \"example.com.\") to the QUESTION section and verify that toWire() serialises the header plus exactly one question."
                }
            ]
        },
        {
            "group": "Message with Question and Answer records",
            "cases": [
                {
                    "input": [],
                    "expected": "A non‑null byte array whose length equals 12 (header) + question length + answer length. The Message.size field must equal the total length.",
                    "description": "Add a question to the QUESTION section and a matching answer (A record) to the ANSWER section. Verify that toWire() includes both sections in the correct order."
                }
            ]
        },
        {
            "group": "Message containing an OPT (EDNS0) record",
            "cases": [
                {
                    "input": [],
                    "expected": "A non‑null byte array whose length equals 12 (header) + any existing sections + the wire length of the OPT record. The Message.size field must equal the total length.",
                    "description": "Set an OPT record via setOPT() (or addRecord with Section.ADDITIONAL) and ensure that toWire() appends the OPT pseudo‑record after all normal sections."
                }
            ]
        },
        {
            "group": "Message with a signed TSIG record",
            "cases": [
                {
                    "input": [],
                    "expected": "A non‑null byte array whose length equals 12 (header) + all normal sections + the wire length of the TSIG record. The Message.size field must equal the total length and the last RR must be a TSIG.",
                    "description": "Create a Message, add some records, then call setTSIG(key) to sign the message. Verify that toWire() adds a TSIG RR at the end and that size reflects the full message length."
                }
            ]
        },
        {
            "group": "Message exceeding the maximum DNS wire length",
            "cases": [
                {
                    "input": [],
                    "expected": "An IOException (or a RuntimeException wrapped by SneakyThrows) thrown when toWire() attempts to serialise a message whose total length would be > Message.MAXLENGTH (65535).",
                    "description": "Construct a Message with enough records (e.g., many large TXT records) so that the resulting wire format would exceed 65535 bytes. toWire() must detect this overflow and propagate an exception."
                }
            ]
        }
    ]
}
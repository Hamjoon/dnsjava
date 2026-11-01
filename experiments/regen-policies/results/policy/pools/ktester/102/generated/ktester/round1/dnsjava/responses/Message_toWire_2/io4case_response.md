{
    "testCases": [
        {
            "group": "MessageWithHeaderFlags",
            "cases": [
                {
                    "input": [],
                    "expected": "byte array whose first 12 bytes (DNS header) have QR=1, AA=1, TC=0, RD=1, RA=1, AD=1, CD=0 bits set correctly; rest of the message contains a simple question only.",
                    "description": "Create a Message, set the header flags (setQR(true), setAA(true), setRD(true), setRA(true), setAD(true)). Call toWire(). The test verifies that the encoded header reflects all flags and that the payload (question) is still correctly encoded."
                }
            ]
        },
        {
            "group": "MessageWithMultipleAdditionalRecordsIncludingOPT",
            "cases": [
                {
                    "input": [],
                    "expected": "byte array where the ADDITIONAL section starts with the OPT pseudo‑record (type 41) followed by a regular A record; the OPT record is encoded before any other additional records, confirming the special‑case ordering logic.",
                    "description": "Add an OPTRecord to the message via Message.getHeader().setFlag(…) or Message.setOPT(...). Then add a normal A record to the ADDITIONAL section. Call toWire() and verify that the OPT record appears first in the additional section."
                }
            ]
        },
        {
            "group": "MessageWithEDNSAndTSIGSigned",
            "cases": [
                {
                    "input": [],
                    "expected": "byte array that contains, in order: DNS header, question, OPT record (EDNS0) in the additional section, and finally a signed TSIG RR (type 250) after the OPT record; the TSIG MAC is non‑empty and the header's ARCOUNT reflects both the OPT and TSIG records.",
                    "description": "Create a query Message, attach an OPTRecord, set a TSIG key and invoke the signing API (Message.sign()). The resulting wire format must contain both the EDNS0 pseudo‑record and the signed TSIG, testing the interaction of both features."
                }
            ]
        },
        {
            "group": "MessageWithTruncationAndTSIGUnsigned",
            "cases": [
                {
                    "input": [],
                    "expected": "byte array of exactly the requested maxLength (e.g., 512) with the TC flag set in the header; the TSIG placeholder record is omitted because the message was truncated before TSIG could be added.",
                    "description": "Construct a Message that would exceed the supplied maxLength when fully encoded (e.g., many answer records). Call the overloaded toWire(maxLength, true). Verify that truncation occurs, the TC flag is set, and no TSIG record is present when a TSIG key is attached but not signed."
                }
            ]
        },
        {
            "group": "MessageWithCompressionOffsetLimit",
            "cases": [
                {
                    "input": [],
                    "expected": "byte array where the first occurrence of a long domain name is encoded in full, and a later occurrence that would require a compression pointer with offset > 0x3FFF is encoded without compression (full name repeated).",
                    "description": "Create a Message with a very long chain of labels (e.g., 20 labels) to push the name's position beyond 16383 bytes. Add a second record that repeats the same name. Call toWire() and verify that the second name is not compressed because the offset exceeds the 14‑bit limit."
                }
            ]
        },
        {
            "group": "MessageExactMaxLengthBoundary",
            "cases": [
                {
                    "input": [],
                    "expected": "byte array of length exactly Message.MAXLENGTH (65535) with no exception thrown; the header's TC flag is clear, indicating the message fit within the limit.",
                    "description": "Build a Message whose encoded size is precisely 65535 bytes (e.g., header + question + a calculated number of answer records). Invoke toWire(int maxLength) with maxLength = Message.MAXLENGTH and verify that the method returns the full byte array without throwing MessageSizeExceededException."
                }
            ]
        },
        {
            "group": "MessageWithMultipleRRsetsInAnswer",
            "cases": [
                {
                    "input": [],
                    "expected": "byte array where the ANSWER section contains two distinct RRsets (e.g., an A RRset for www.example.com and an AAAA RRset for www.example.com) each preceded by its own count and correctly ordered; the overall size field reflects both RRsets.",
                    "description": "Add two RRsets with the same name but different types to the ANSWER section. Call toWire() and verify that both RRsets are emitted, preserving the order they were added."
                }
            ]
        },
        {
            "group": "MessageWithDuplicateRecords",
            "cases": [
                {
                    "input": [],
                    "expected": "byte array where the ANSWER section contains two identical A records for the same name; both records are encoded separately (no deduplication), confirming that toWire() writes every Record instance present in the section.",
                    "description": "Add the same Record instance twice (or two equal Record objects) to the ANSWER section. Call toWire() and check that the wire format contains two A RRs with identical data."
                }
            ]
        }
    ]
}
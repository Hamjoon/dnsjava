{
    "tests": [
        {
            "group": "Message with Multiple Question Records",
            "cases": [
                {
                    "input": [],
                    "expected": "byte array length > 12 containing two question records serialized in order; Message.size equals the returned array length",
                    "description": "Create a Message via new Message(), add two distinct Question records (e.g., A query for example.com and AAAA query for example.org) using addRecord(record, Message.QUESTION_SECTION). Call toWire() and verify that both questions appear in the wire format and that the internal size field matches the byte array length."
                }
            ]
        },
        {
            "group": "Message with OPT and Signed TSIG Order",
            "cases": [
                {
                    "input": [],
                    "expected": "byte array where the OPT pseudo‑RR appears before the signed TSIG record in the additional section; Message.size matches array length; tsigState is TSIG_SIGNED",
                    "description": "Create a Message, add an OPTRecord via setOPT(OPTRecord), then sign the message with a TSIG key using setTSIG(key, 0, null). Call toWire() and verify that the serialized additional section contains the OPT record first and the TSIG record last, confirming correct ordering per RFC 6891."
                }
            ]
        },
        {
            "group": "Message with Various Header Flags (RD, AA, RCODE)",
            "cases": [
                {
                    "input": [],
                    "expected": "byte array whose DNS header has the Recursion Desired (RD) flag set, Authoritative Answer (AA) flag set, and RCODE set to NXDOMAIN (3); Message.size equals array length",
                    "description": "Instantiate a Message, obtain its Header via getHeader(), setRD(true), setAA(true), setRcode(Rcode.NXDOMAIN). Add a simple question record. After toWire(), decode the first two bytes of the header to ensure the RD and AA bits are 1 and the lower 4 bits of the third byte equal 3 (NXDOMAIN)."
                }
            ]
        },
        {
            "group": "Message Size Field Overwrite Verification",
            "cases": [
                {
                    "input": [],
                    "expected": "Message.size reflects the length of the newly generated wire array, not any previous value; byte array length matches Message.size",
                    "description": "Create a Message, manually set its private field `size` to an arbitrary value (e.g., 999) using reflection, then call toWire(). Verify that after serialization the `size` field equals the actual length of the returned byte array, confirming that toWire() overwrites any stale size."
                }
            ]
        },
        {
            "group": "Message at Maximum Allowed Length (65535 bytes)",
            "cases": [
                {
                    "input": [],
                    "expected": "byte array length exactly 65535; no exception thrown; Message.size equals 65535",
                    "description": "Construct a Message that fills the wire format to exactly 65535 bytes (e.g., a query with a large number of TXT records whose total serialized size is 65523 bytes plus the 12‑byte header). Call toWire() and assert that the returned array length is 65535 and that no IOException or IllegalArgumentException is raised."
                }
            ]
        }
    ]
}
{
    "group": "Message.setTSIG - basic field assignment",
    "cases": [
        {
            "input": [
                { "parameter": "key", "value": "new TSIG(\"hmac-md5\", \"secret\".getBytes())" },
                { "parameter": "error", "value": "0" },
                { "parameter": "querytsig", "value": "new TSIGRecord(Name.fromString(\"example.com.\"), DClass.IN, 300, new TSIG(\"hmac-md5\", \"secret\".getBytes()))" }
            ],
            "expected": "Message.getTSIG() returns the same TSIG instance, Message.getGeneratedTSIG() returns the same TSIGRecord instance, and Message.tsigerror equals 0",
            "description": "Standard usage with non‑null key and query TSIG and a successful error code (0). All three fields should be stored unchanged."
        },
        {
            "input": [
                { "parameter": "key", "value": "null" },
                { "parameter": "error", "value": "0" },
                { "parameter": "querytsig", "value": "null" }
            ],
            "expected": "Message.getTSIG() returns null, Message.getGeneratedTSIG() returns null, and Message.tsigerror equals 0",
            "description": "Both TSIG key and query TSIG are null – the method must still assign the null values without throwing."
        },
        {
            "input": [
                { "parameter": "key", "value": "new TSIG(\"hmac-sha256\", \"key\".getBytes())" },
                { "parameter": "error", "value": "-1" },
                { "parameter": "querytsig", "value": "null" }
            ],
            "expected": "Message.getTSIG() returns the provided TSIG, Message.getGeneratedTSIG() returns null, and Message.tsigerror equals -1",
            "description": "Negative error code is allowed – the method should store the exact integer value."
        },
        {
            "input": [
                { "parameter": "key", "value": "null" },
                { "parameter": "error", "value": "65535" },
                { "parameter": "querytsig", "value": "new TSIGRecord(Name.fromString(\"example.org.\"), DClass.IN, 600, new TSIG(\"hmac-sha1\", \"another\".getBytes()))" }
            ],
            "expected": "Message.getTSIG() returns null, Message.getGeneratedTSIG() returns the provided TSIGRecord, and Message.tsigerror equals 65535",
            "description": "Large positive error code with a non‑null query TSIG but a null key – verifies that each field is set independently."
        },
        {
            "input": [
                { "parameter": "key", "value": "new TSIG(\"hmac-md5\", \"first\".getBytes())" },
                { "parameter": "error", "value": "0" },
                { "parameter": "querytsig", "value": "new TSIGRecord(Name.fromString(\"first.com.\"), DClass.IN, 100, new TSIG(\"hmac-md5\", \"first\".getBytes()))" }
            ],
            "expected": "After first setTSIG call, fields contain the first values; after a second setTSIG call with different arguments, fields contain the second values, confirming overwriting works.",
            "description": "Ensures that repeated invocations replace previously stored TSIG, error and query TSIG rather than appending or leaving stale data."
        }
    ]
}
{
    "group": "setTSIG basic functionality",
    "cases": [
        {
            "input": [
                { "parameter": "key", "value": "TSIG instance A" },
                { "parameter": "error", "value": 0 },
                { "parameter": "querytsig", "value": "TSIGRecord instance X" }
            ],
            "expected": "Message.tsigkey == TSIG instance A && Message.tsigerror == 0 && Message.querytsig == TSIGRecord instance X",
            "description": "Verify that setTSIG correctly stores a non‑null TSIG key, a zero error code and a non‑null query TSIG record."
        },
        {
            "input": [
                { "parameter": "key", "value": "TSIG instance B" },
                { "parameter": "error", "value": 9 },
                { "parameter": "querytsig", "value": "TSIGRecord instance Y" }
            ],
            "expected": "Message.tsigkey == TSIG instance B && Message.tsigerror == 9 && Message.querytsig == TSIGRecord instance Y",
            "description": "Verify that setTSIG stores a positive error code (e.g., BADTIME) together with a different TSIG key and query record."
        }
    ]
}
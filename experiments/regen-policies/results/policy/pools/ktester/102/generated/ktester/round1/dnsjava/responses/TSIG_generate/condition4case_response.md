{
    "group": "Normal operation – successful TSIG generation",
    "cases": [
        {
            "input": [
                { "parameter": "m", "value": "Message with one question and no TSIG" },
                { "parameter": "b", "value": "byte[]{0x01,0x02,0x03}" },
                { "parameter": "error", "value": "0" },
                { "parameter": "old", "value": "null" }
            ],
            "expected": "TSIGRecord instance whose name equals TSIG.GSS_TSIG, algorithm equals HMAC_SHA256, error code 0 and a non‑null MAC value. The returned record length matches recordLength() of the TSIG object.",
            "description": "Standard path: no previous TSIG, NOERROR, fullSignature default (true). The method should compute the MAC over the supplied message bytes and return a fresh TSIGRecord."
        },
        {
            "input": [
                { "parameter": "m", "value": "Message with existing TSIG record (old != null)" },
                { "parameter": "b", "value": "byte[]{0x0A,0x0B}" },
                { "parameter": "error", "value": "0" },
                { "parameter": "old", "value": "TSIGRecord instance from a previous request" }
            ],
            "expected": "TSIGRecord instance whose MAC is computed using the old TSIG’s key and includes the previous MAC in the HMAC input (per RFC 2845). The returned record must contain the same key name and algorithm as the old record.",
            "description": "Branch where an old TSIGRecord is supplied – the method must incorporate the previous MAC into the new MAC calculation."
        }
    ]
  ,
  "group": "Error handling – non‑zero error codes",
  "cases": [
    {
      "input": [
        { "parameter": "m", "value": "Message with a query section only" },
        { "parameter": "b", "value": "byte[]{0x55,0x66}" },
        { "parameter": "error", "value": "9" },              

        { "parameter": "old", "value": "null" }
      ],
      "expected": "TSIGRecord instance whose error field equals 9 (FORMERR, SERVFAIL, etc.) and whose MAC is still calculated over the message bytes. No exception is thrown.",
      "description": "When the error argument is non‑zero the method must still generate a TSIGRecord, but the error field of the record must reflect the supplied value."
    },
    {
      "input": [
        { "parameter": "m", "value": "Message with a TSIG RR already attached" },
        { "parameter": "b", "value": "byte[]{0x01}" },
        { "parameter": "error", "value": "16" },              
        { "parameter": "old", "value": "TSIGRecord with algorithm HMAC_SHA1" }
      ],
      "expected": "TSIGRecord instance whose error field equals 16 and whose MAC is calculated using the HMAC_SHA1 algorithm from the old record.",
      "description": "Branch where both a non‑zero error and an old TSIGRecord are present – the method must honour the error code while still using the old record’s algorithm for MAC generation."
    }
  ]
  ,
  "group": "Null or invalid inputs",
  "cases": [
    {
      "input": [
        { "parameter": "m", "value": "null" },
        { "parameter": "b", "value": "byte[]{0x00}" },
        { "parameter": "error", "value": "0" },
        { "parameter": "old", "value": "null" }
      ],
      "expected": "NullPointerException",
      "description": "The method should throw NPE when the Message argument is null (first defensive check)."
    },
    {
      "input": [
        { "parameter": "m", "value": "Message with a question section" },
        { "parameter": "b", "value": "null" },
        { "parameter": "error", "value": "0" },
        { "parameter": "old", "value": "null" }
      ],
      "expected": "NullPointerException",
      "description": "The method should throw NPE when the byte[] argument is null."
    },
    {
      "input": [
        { "parameter": "m", "value": "Message with a question section" },
        { "parameter": "b", "value": "byte[]{} (empty array)" },
        { "parameter": "error", "value": "0" },
        { "parameter": "old", "value": "null" }
      ],
      "expected": "TSIGRecord instance with a valid MAC (MAC of empty data) and error 0.",
      "description": "Empty byte array is a valid input – the method must still produce a TSIGRecord (MAC of zero‑length data)."
    }
  ]
  ,
  "group": "Full signature flag – internal delegation",
  "cases": [
    {
      "input": [
        { "parameter": "m", "value": "Message with a question section" },
        { "parameter": "b", "value": "byte[]{0x11,0x22}" },
        { "parameter": "error", "value": "0" },
        { "parameter": "old", "value": "null" }
      ],
      "expected": "Same result as calling generate(m,b,0,null,true) – a TSIGRecord with a full MAC (not truncated).",
      "description": "The public generate method must delegate to the overloaded version with fullSignature=true. Verify that the MAC length matches the algorithm’s full output size."
    }
  ]
  ,
  "group": "Exception during MAC initialization",
  "cases": [
    {
      "input": [
        { "parameter": "m", "value": "Message with a question section" },
        { "parameter": "b", "value": "byte[]{0x33,0x44}" },
        { "parameter": "error", "value": "0" },
        { "parameter": "old", "value": "TSIGRecord with an unsupported algorithm name (e.g., Name.fromString(\"unsupported-alg.\"))" }
      ],
      "expected": "GeneralSecurityException wrapped in a RuntimeException (or a specific TSIGException if the library throws it).",
      "description": "When the algorithm name cannot be mapped to a known HMAC algorithm, the internal generate method will attempt to create a Mac instance and fail, propagating an exception. This test validates that the exception path is exercised."
    }
  ]
}
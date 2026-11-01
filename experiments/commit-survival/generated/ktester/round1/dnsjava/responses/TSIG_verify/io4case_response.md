Invalid control character at: line 12 column 27 (char 486)
{
  "testCases": [
    {
      "group": "verify_success",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "Message with valid TSIG RR and matching MAC" },
            { "parameter": "messageBytes", "value": "byte[] of the wire format of the DNS message (including TSIG RR)" },
            { "parameter": "requestTSIG", "value": "TSIGRecord from the original request (same key name, algorithm, and time)" }
          ],
          "expected": "0   // Rcode.NOERROR – verification succeeds",
          "description": "A normal case where the response message contains a TSIG record that matches the request TSIG, the MAC is correct, and the timestamp is within the default FUDGE window."
        }
      ]
    },
    {
      "group": "verify_failure_bad_mac",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "Message with TSIG RR whose MAC was tampered (different bytes)" },
            { "parameter": "messageBytes", "value": "byte[] of the wire format of the DNS message (including the altered TSIG RR)" },
            { "parameter": "requestTSIG", "value": "Original request TSIGRecord (unchanged)" }
          ],
          "expected": "16   // Rcode.BADSIG – MAC does not match",
          "description": "The MAC in the TSIG RR does not match the MAC calculated from the message and the shared secret, causing verification to fail with BADSIG."
        }
      ]
    },
    {
      "group": "verify_failure_bad_key",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "Message with TSIG RR that references a different key name than the request" },
            { "parameter": "messageBytes", "value": "byte[] of the wire format of the DNS message" },
            { "parameter": "requestTSIG", "value": "TSIGRecord from request (key name A)" }
          ],
          "expected": "17   // Rcode.BADKEY – key name mismatch",
          "description": "The key name in the response TSIG does not match the key name used in the request, leading to a BADKEY error."
        }
      ]
    },
    {
      "group": "verify_failure_bad_time",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "Message with TSIG RR whose time is older than now - FUDGE" },
            { "parameter": "messageBytes", "value": "byte[] of the wire format of the DNS message" },
            { "parameter": "requestTSIG", "value": "TSIGRecord from request (timestamp = now)" }
          ],
          "expected": "18   // Rcode.BADTIME – timestamp outside allowed window",
          "description": "The TSIG timestamp is outside the allowed ±FUDGE (300 s) window, causing verification to return BADTIME."
        },
        {
          "input": [
            { "parameter": "m", "value": "Message with TSIG RR whose time is newer than now + FUDGE" },
            { "parameter": "messageBytes", "value": "byte[] of the wire format of the DNS message" },
            { "parameter": "requestTSIG", "value": "TSIGRecord from request (timestamp = now)" }
          ],
          "expected": "18   // Rcode.BADTIME – timestamp outside allowed window",
          "description": "The TSIG timestamp is too far in the future, also resulting in a BADTIME error."
        }
      ]
    },
    {
      "group": "verify_failure_missing_request_tsig",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "Message that contains a TSIG RR" },
            { "parameter": "messageBytes", "value": "byte[] of the wire format of the DNS message" },
            { "parameter": "requestTSIG", "value": "null" }
          ],
          "expected": "1   // Rcode.FORMERR – request TSIG is required for verification",
          "description": "When the request TSIG is null, the method cannot compute the expected MAC and returns FORMERR."
        }
      ]
    },
    {
      "group": "verify_failure_null_message",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "null" },
            { "parameter": "messageBytes", "value": "byte[] of a valid DNS message (ignored because message is null)" },
            { "parameter": "requestTSIG", "value": "valid TSIGRecord from request" }
          ],
          "expected": "1   // Rcode.FORMERR – message must not be null",
          "description": "Passing a null Message object is illegal; the method should return FORMERR."
        }
      ]
    },
    {
      "group": "verify_failure_empty_message_bytes",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "Message with no TSIG RR" },
            { "parameter": "messageBytes", "value": "new byte[0]" },
            { "parameter": "requestTSIG", "value": "valid TSIGRecord from request" }
          ],
          "expected": "1   // Rcode.FORMERR – empty wire data cannot be processed",
          "description": "An empty byte array cannot be parsed into a DNS message; verification should fail with FORMERR."
        }
      ]
    },
    {
      "group": "verify_failure_unknown_algorithm",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "Message with TSIG RR that specifies an unsupported algorithm name" },
            { "parameter": "messageBytes", "value": "byte[] of the wire format of the DNS message" },
            { "parameter": "requestTSIG", "value": "TSIGRecord from request (algorithm = \"unknown-alg\")" }
          ],
          "expected": "1   // Rcode.FORMERR – algorithm not recognized",
          "description": "If the algorithm name cannot be mapped to a known HMAC algorithm, the method should return FORMERR."
        }
      ]
    },
    {
      "group": "verify_success_without_fullSignature",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "Message with TSIG RR where the MAC covers only the DNS message (fullSignature = false default)" },
            { "parameter": "messageBytes", "value": "byte[] of the DNS message without the TSIG RR (as used by the overloaded method)" },
            { "parameter": "requestTSIG", "value": "TSIGRecord from request matching the key and algorithm" }
          ],
          "expected": "0   // Rcode.NOERROR – verification succeeds using the default (fullSignature = true) path",
          "description": "Even though the overload calls verify(..., true), this case confirms that a correctly signed message passes verification."
        }
      ]
    }
  ]
}
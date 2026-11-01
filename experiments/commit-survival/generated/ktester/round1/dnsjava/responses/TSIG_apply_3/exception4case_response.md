{
  "testCases": [
    {
      "group": "Null Secret Key",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "new Message()"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord(keyName='example.com.', algorithm=HMAC_SHA256, secretKey=null, time=Instant.now())"
            }
          ],
          "expected": "IllegalArgumentException thrown because secret key is null",
          "description": "When the supplied old TSIGRecord has a null secretKey the apply method must reject the call."
        }
      ]
    },
    {
      "group": "Null Timestamp",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "new Message()"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord(keyName='example.com.', algorithm=HMAC_SHA256, secretKey=byte[16]{0x01}, time=null)"
            }
          ],
          "expected": "IllegalArgumentException thrown because timestamp is null",
          "description": "apply must validate that the old record's time field is non‑null; a null timestamp should cause an exception."
        }
      ]
    },
    {
      "group": "Null Key Name",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "new Message()"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord(keyName=null, algorithm=HMAC_SHA256, secretKey=byte[16]{0x01}, time=Instant.now())"
            }
          ],
          "expected": "IllegalArgumentException thrown because key name is null",
          "description": "A null keyName in the old TSIGRecord is invalid and must be rejected."
        }
      ]
    },
    {
      "group": "Invalid Key Name Format",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "new Message()"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord(keyName='invalid_name', algorithm=HMAC_SHA256, secretKey=byte[16]{0x01}, time=Instant.now())"
            }
          ],
          "expected": "IllegalArgumentException thrown because key name is not a valid DNS name",
          "description": "TSIG requires the key name to be a fully qualified domain name; a plain string without a trailing dot should cause an exception."
        }
      ]
    },
    {
      "group": "Unsupported Algorithm Name",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "new Message()"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord(keyName='example.com.', algorithm=Name.fromConstantString('HMAC-UNSUPPORTED.'), secretKey=byte[16]{0x01}, time=Instant.now())"
            }
          ],
          "expected": "IllegalArgumentException thrown because the algorithm cannot be instantiated by JCE",
          "description": "When the algorithm name maps to a Name that is not supported by the underlying JCE provider, apply must fail with an exception."
        }
      ]
    },
    {
      "group": "Error Code Upper Bound Violation",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "new Message()"
            },
            {
              "parameter": "int error",
              "value": "65535"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException thrown because error code exceeds the maximum allowed value",
          "description": "apply should validate that the DNS error code is within the range defined by the protocol; an excessively large value must be rejected."
        }
      ]
    }
  ]
}
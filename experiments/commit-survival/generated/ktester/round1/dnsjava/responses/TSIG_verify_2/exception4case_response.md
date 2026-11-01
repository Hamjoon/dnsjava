{
  "test_cases": [
    {
      "group": "Invalid Algorithm Format",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock Message (valid)"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "validMessageBytes"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with algorithm = \"HMAC MD5\" (contains space, illegal DNS name)"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "false"
            }
          ],
          "expected": "Rcode.FORMERR",
          "description": "Algorithm string contains illegal characters that cause Name parsing to throw IllegalArgumentException; method should treat this as a malformed TSIG record and return FORMERR."
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
              "value": "mock Message (valid)"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "validMessageBytes"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with keyName = null"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "false"
            }
          ],
          "expected": "Rcode.NOTAUTH",
          "description": "TSIGRecord does not contain a key name; key lookup fails and MAC initialization cannot proceed, resulting in NOTAUTH."
        }
      ]
    },
    {
      "group": "Empty Secret Key",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock Message (valid)"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "validMessageBytes"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with valid key name but secret key bytes = new byte[0]"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "false"
            }
          ],
          "expected": "Rcode.BADSIG",
          "description": "A zero‑length secret key can be used to create a Mac instance, but the resulting MAC will never match the signature; verification must fail with BADSIG."
        }
      ]
    },
    {
      "group": "MAC Length Mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock Message (valid)"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "validMessageBytes"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with algorithm = \"HMAC-MD5.SIG-ALG.REG.INT.\", MAC = new byte[4] (shorter than expected 16 bytes)"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "false"
            }
          ],
          "expected": "Rcode.BADSIG",
          "description": "The MAC length does not match the length required by the algorithm; verification should detect the mismatch and return BADSIG."
        }
      ]
    },
    {
      "group": "Empty Message Payload With Non‑Empty MAC",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message with no additional records (empty payload)"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "new byte[0]"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with valid algorithm/key/time but MAC = non‑empty byte array"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "false"
            }
          ],
          "expected": "Rcode.BADSIG",
          "description": "When the message payload is empty but the TSIG record contains a non‑empty MAC, the computed MAC (over empty data) cannot match; verification must fail with BADSIG."
        }
      ]
    }
  ]
}
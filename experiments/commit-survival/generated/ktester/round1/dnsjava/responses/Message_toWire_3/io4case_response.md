{
  "testCases": [
    {
      "group": "Boundary condition – maxLength exactly required size",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": "wireSize"
            }
          ],
          "expected": "byte[] whose length equals wireSize and matches the output of toWire() without a maxLength argument",
          "description": "Create a simple query Message (Message.newQuery(questionRecord)). Compute its wire size by calling toWire() with the default overload, store that length as `wireSize`, then call toWire(wireSize). The method should succeed and return a byte array of exactly that length, confirming that the boundary check is inclusive."
        }
      ]
    },
    {
      "group": "Zero maxLength handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 0
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Passing a maxLength of 0 is illegal because a DNS message must contain at least the 12‑byte header. The method should throw IllegalArgumentException before any serialization occurs."
        }
      ]
    },
    {
      "group": "Empty message (no question, no records)",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 512
            }
          ],
          "expected": "byte[] of length 12 (only the DNS header) with all header fields set to zero except ID",
          "description": "Instantiate a Message with the default constructor, do not add a question or any records. toWire(512) should produce a 12‑byte array representing an empty DNS message (header only)."
        }
      ]
    },
    {
      "group": "Message containing only an OPT record (EDNS) and no question",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 256
            }
          ],
          "expected": "byte[] whose length is >12 and contains an OPT RR in the additional section and no question section",
          "description": "Create a Message, add an OPTRecord via setOPT(new OPTRecord(...)) without adding a question Record. toWire(256) must serialize the header followed directly by the OPT RR in the additional section. The resulting byte array should have the OPT RR at the end and the question count field should be zero."
        }
      ]
    },
    {
      "group": "Signed message exceeding supplied maxLength",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 200
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Create a Message with enough records (e.g., 5 large TXT records) such that the unsigned wire size is 180 bytes. Sign the message with setTSIG(key). The TSIG adds ~20 bytes, pushing the total to ~200+ bytes. Calling toWire(200) should detect the overflow and throw IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Internal state – size field reflects wire length",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 1024
            }
          ],
          "expected": "Message.size equals the length of the returned byte[]",
          "description": "After calling toWire(1024) on a Message with a mixture of sections, verify that the private `size` field (exposed via the public method numBytes() if available, or via reflection) matches the length of the byte array returned by toWire."
        }
      ]
    },
    {
      "group": "Large message just under MAXLENGTH",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 65535
            }
          ],
          "expected": "byte[] whose length is < 65535 (e.g., 65000) and serialization succeeds",
          "description": "Populate a Message with many records so that its wire size is, for example, 65000 bytes (well under the limit). Call toWire(Message.MAXLENGTH). The method should succeed and return a byte array of length 65000, confirming that the implementation does not require the message to exactly fill the limit."
        }
      ]
    }
  ]
}
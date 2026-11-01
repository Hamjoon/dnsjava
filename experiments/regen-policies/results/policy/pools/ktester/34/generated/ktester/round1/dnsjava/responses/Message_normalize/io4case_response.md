Invalid control character at: line 25 column 201 (char 919)
{
  "tests": [
    {
      "group": "Message.normalize(Message) - null argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null reference to normalize(Message) should result in a NullPointerException because the method immediately forwards the argument to normalize(query, false) without a null‑check."
        }
      ]
    },
    {
      "group": "Message.normalize(Message) - malformed header causing WireParseException",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message.newQuery(new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\")))\n                .setHeader(new Header() {{ setID(-1); }}) // invalid ID triggers parsing error"
            }
          ],
          "expected": "null",
          "description": "A Message whose header is deliberately corrupted (e.g., negative ID) causes normalize(query, false) to throw a WireParseException. The catch block swallows the exception and the method returns null."
        }
      ]
    },
    {
      "group": "Message.normalize(Message) - message containing irrelevant records",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message m = Message.newQuery(new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\")));\n                m.addRecord(new TXTRecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, \"irrelevant\"), Message.ANSWER);\n                m"
            }
          ],
          "expected": "non‑null Message instance whose ANSWER section is empty",
          "description": "When the supplied query contains records that are not allowed in the QUESTION section, normalize(query, false) should drop those irrelevant records. The returned Message must still be non‑null and must not contain the added TXT record."
        }
      ]
    },
    {
      "group": "Message.normalize(Message) - message with TSIG error flag set",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message m = Message.newQuery(new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\")));\n                TSIG tsig = new TSIG(\"key\", \"hmac-md5\", \"base64==\");\n                m.setTSIG(tsig);\n                // Simulate a TSIG verification failure by manually setting the internal state (for testing only)\n                java.lang.reflect.Field f = Message.class.getDeclaredField(\"tsigState\");\n                f.setAccessible(true);\n                f.setInt(m, Message.TSIG_FAILED);\n                m"
            }
          ],
          "expected": "non‑null Message instance whose TSIG error flag is cleared (i.e., returned Message is normalized and has no TSIG failure)",
          "description": "normalize(Message) should produce a normalized copy that resets TSIG error state. Even if the original message has tsigState == TSIG_FAILED, the normalized result must not propagate the failure."
        }
      ]
    },
    {
      "group": "Message.normalize(Message) - maximum size message (edge of MAXLENGTH)",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message m = new Message();\n                // Fill the message with dummy records until it reaches MAXLENGTH‑1 bytes\n                while (m.numBytes() < Message.MAXLENGTH - 10) {\n                    m.addRecord(new ARecord(Name.fromString(\"a.com.\"), DClass.IN, 3600, InetAddress.getByName(\"127.0.0.1\")), Message.ANSWER);\n                }\n                m"
            }
          ],
          "expected": "non‑null Message instance (normalization succeeds without truncation)",
          "description": "A message whose wire size is just below the protocol limit should be normalized successfully. The test verifies that the method does not mistakenly treat a near‑limit size as an error."
        }
      ]
    }
  ]
}
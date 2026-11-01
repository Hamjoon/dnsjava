Invalid control character at: line 10 column 68 (char 247)
{
  "testCases": [
    {
      "group": "Message.setHeader reference semantics",
      "cases": [
        {
          "input": [
            {
              "parameter": "header",
              "value": "Header hdr = new Header(); hdr.setID(1234); // mutable Header instance"
            }
          ],
          "expected": "After calling setHeader(hdr), Message.getHeader() returns the same hdr instance; modifying hdr.setID(5678) later is reflected in Message.getHeader().getID() == 5678",
          "description": "Verify that setHeader stores the exact Header reference (no defensive copy). Changing the Header after it has been set should be observable through the Message."
        },
        {
          "input": [
            {
              "parameter": "header",
              "value": "Header first = new Header(); first.setID(1111); Header second = new Header(); second.setID(2222);"
            }
          ],
          "expected": "After setHeader(first) followed by setHeader(second), Message.getHeader() returns the second Header instance and its ID equals 2222",
          "description": "Ensure that a subsequent call to setHeader overwrites the previous Header reference with the new one."
        }
      ]
    },
    {
      "group": "Message.setHeader with extreme Header field values",
      "cases": [
        {
          "input": [
            {
              "parameter": "header",
              "value": "Header maxIdHeader = new Header(); maxIdHeader.setID(0xFFFF); // maximum 16‑bit ID"
            }
          ],
          "expected": "Message.getHeader().getID() == 65535",
          "description": "Validate that a Header containing the maximum allowed ID value can be stored without error."
        },
        {
          "input": [
            {
              "parameter": "header",
              "value": "Header flagHeader = new Header(); flagHeader.setFlag(Header.FLAG_QR, true); flagHeader.setFlag(Header.FLAG_AA, true);"
            }
          ],
          "expected": "Message.getHeader().getFlag(Header.FLAG_QR) == true && Message.getHeader().getFlag(Header.FLAG_AA) == true",
          "description": "Check that a Header with unusual flag combinations is retained exactly as provided."
        }
      ]
    },
    {
      "group": "Message.setHeader with subclass of Header",
      "cases": [
        {
          "input": [
            {
              "parameter": "header",
              "value": "class MyHeader extends Header { private String note = \"test\"; public String getNote(){return note;} } MyHeader sub = new MyHeader();"
            }
          ],
          "expected": "Message.getHeader() instanceof MyHeader && ((MyHeader)Message.getHeader()).getNote().equals(\"test\")",
          "description": "Confirm that setHeader accepts any subclass of Header and preserves its runtime type and additional data."
        }
      ]
    },
    {
      "group": "Message.setHeader does not affect unrelated fields",
      "cases": [
        {
          "input": [
            {
              "parameter": "header",
              "value": "new Header()"
            }
          ],
          "expected": "Message.getHeader() is not null && Message.getTSIG() remains unchanged (null if not previously set)",
          "description": "Ensure that setting the Header does not unintentionally modify other Message state such as TSIG fields."
        }
      ]
    }
  ]
}
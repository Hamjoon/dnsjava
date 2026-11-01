{
  "new_test_cases": [
    {
      "group": "Irrelevant records in ANSWER with throwOnIrrelevantRecord true",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with question target.example.com, type A, answer section contains RRset named other.example.com (irrelevant)"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": true
            }
          ],
          "expected": "WireParseException is thrown because an irrelevant RRset is encountered and throwOnIrrelevantRecord is true",
          "description": "When an RRset in the ANSWER section does not match the current sname and throwOnIrrelevantRecord is true, the method should abort with a WireParseException."
        }
      ]
    },
    {
      "group": "CNAME handling when query type is CNAME (multiple records, no trimming)",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with question alias.example.com, type CNAME, answer section contains a CNAME RRset with two CNAME records"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": false
            }
          ],
          "expected": "cleanedAnswerSection contains the original CNAME RRset with both CNAME records (no removal, no exception)",
          "description": "When the query asks for CNAME, the multiple‑CNAME‑record guard is not triggered; the method should keep the RRset unchanged."
        }
      ]
    },
    {
      "group": "ANY query with DNAME synthesis and subsequent records for original name",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with question foo.example.com, type ANY, answer section contains: DNAME RRset (single) for example.com → d.example.com, followed by an A RRset for foo.example.com"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": false
            }
          ],
          "expected": "cleanedAnswerSection contains the DNAME RRset, a synthesized CNAME RRset (TTL 0, target = foo.example.com → d.example.com), and then the original A RRset (because query type is ANY the loop copies records whose name equals the original name)",
          "description": "For an ANY query, after synthesizing a CNAME from a DNAME the method must also copy any records that share the original name (oldSname)."
        }
      ]
    },
    {
      "group": "Authority NS handling when answer section is not empty (non‑NODATA)",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with question www.example.com, type A, answer section contains an A RRset for www.example.com, authority section contains an NS RRset for www.example.com"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": false
            }
          ],
          "expected": "cleanedAuthoritySection includes the NS RRset (NS is kept because the response is not NODATA and not NXDOMAIN)",
          "description": "When the response contains answer records, NS RRsets in the authority section are allowed and should be retained."
        }
      ]
    },
    {
      "group": "Authority allowed type retained (MX) when isTypeAllowedInSection returns true",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with question mail.example.com, type MX, authority section contains an MX RRset for example.com"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": false
            }
          ],
          "expected": "cleanedAuthoritySection contains the MX RRset (MX is allowed in AUTHORITY section)",
          "description": "If isTypeAllowedInSection reports true for a type (e.g., MX) the RRset should be kept in the normalized message."
        }
      ]
    },
    {
      "group": "Additional section records not referenced are omitted",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with question host.example.com, type A, answer section contains A RRset for host.example.com, additional section contains two A records: one for host.example.com (referenced) and one for unrelated.example.com (unreferenced)"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": false
            }
          ],
          "expected": "cleanedAdditionalSection contains only the A record for host.example.com; the unreferenced record for unrelated.example.com is omitted",
          "description": "addAdditionalRRset should only copy additional records that are referenced by cleaned answer/authority RRsets; unrelated records must be dropped."
        }
      ]
    },
    {
      "group": "Irrelevant CNAME record in ANSWER (name mismatch) is ignored",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with question target.example.com, type A, answer section contains a CNAME RRset whose name is other.example.com (does not match current sname)"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": false
            }
          ],
          "expected": "cleanedAnswerSection does not contain the irrelevant CNAME RRset; it is skipped",
          "description": "When a CNAME RRset's name does not equal the current sname, it is considered irrelevant and must be ignored."
        }
      ]
    }
  ]
}
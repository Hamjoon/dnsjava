{
  "testCases": [
    {
      "group": "findRecords - null and invalid arguments",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "null"
            },
            {
              "parameter": "type",
              "value": "1"
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling findRecords with a null Name should throw a NullPointerException because the method ultimately forwards the argument to lookup which expects a non‑null name."
        },
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"example.com.\")"
            },
            {
              "parameter": "type",
              "value": "-1"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Passing an invalid DNS type (negative value) should cause lookup to reject the request and throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "findRecords - exact match scenarios",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"www.example.com.\")"
            },
            {
              "parameter": "type",
              "value": "Type.A"
            }
          ],
          "expected": "SetResponse with status SUCCESS and an RRset containing the A record for www.example.com.",
          "description": "The zone contains an A record for www.example.com. The query should return a successful SetResponse with the exact matching RRset."
        },
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"mail.example.com.\")"
            },
            {
              "parameter": "type",
              "value": "Type.MX"
            }
          ],
          "expected": "SetResponse with status NXDOMAIN",
          "description": "The queried name does not exist in the zone at all, so lookup should return an NXDOMAIN response."
        },
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"host.example.com.\")"
            },
            {
              "parameter": "type",
              "value": "Type.SRV"
            }
          ],
          "expected": "SetResponse with status NODATA (name exists but no SRV RRset)",
          "description": "The name host.example.com exists (e.g., has an A record) but there is no SRV record. The method should return a NODATA response."
        }
      ]
    },
    {
      "group": "findRecords - delegation and CNAME handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"sub.example.com.\")"
            },
            {
              "parameter": "type",
              "value": "Type.A"
            }
          ],
          "expected": "SetResponse with status DELEGATION and the NS RRset for sub.example.com.",
          "description": "sub.example.com is a delegated sub‑zone (has NS records). The lookup should identify the delegation and return a DELEGATION response."
        },
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"alias.example.com.\")"
            },
            {
              "parameter": "type",
              "value": "Type.A"
            }
          ],
          "expected": "SetResponse with status CNAME and the CNAME RRset pointing to the canonical name.",
          "description": "alias.example.com is defined as a CNAME record. The query for an A record should result in a CNAME response containing the CNAME RRset."
        }
      ]
    },
    {
      "group": "findRecords - wildcard matching",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"anyhost.example.com.\")"
            },
            {
              "parameter": "type",
              "value": "Type.A"
            }
          ],
          "expected": "SetResponse with status SUCCESS and an RRset derived from the wildcard *.example.com A record.",
          "description": "The zone contains a wildcard A record (*.example.com). A query for a name that does not have an explicit record should be satisfied by the wildcard, returning a SUCCESS response."
        }
      ]
    },
    {
      "group": "findRecords - empty zone handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"any.example.com.\")"
            },
            {
              "parameter": "type",
              "value": "Type.A"
            }
          ],
          "expected": "SetResponse with status NXDOMAIN",
          "description": "When the Zone instance contains no records, any lookup should result in an NXDOMAIN response."
        }
      ]
    }
  ]
}
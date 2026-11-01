{
  "testCases": [
    {
      "group": "SOA retrieval - replacement behavior",
      "cases": [
        {
          "input": [],
          "expected": "instance of the second SOARecord added",
          "description": "Add an initial SOARecord to the Zone, then add a different SOARecord (e.g., with a different serial). The second addition should replace the internal SOA reference, so getSOA() must return the second SOARecord instance."
        }
      ]
    },
    {
      "group": "SOA retrieval - add via RRset",
      "cases": [
        {
          "input": [],
          "expected": "instance of the SOARecord contained in the RRset",
          "description": "Create an RRset that contains a single SOARecord, add it to the Zone using addRRset(RRset). After the call, getSOA() should return that SOARecord, confirming that addRRset correctly updates the internal SOA field."
        }
      ]
    },
    {
      "group": "SOA retrieval - non‑SOA records do not affect SOA",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Add a non‑SOA record (e.g., an ARecord) to a freshly created Zone. Since no SOARecord has been added, getSOA() must still return null, proving that unrelated record types do not modify the SOA reference."
        }
      ]
    },
    {
      "group": "SOA retrieval - removal of unrelated records",
      "cases": [
        {
          "input": [],
          "expected": "instance of the original SOARecord",
          "description": "Add a valid SOARecord and also add an unrelated ARecord. Remove the ARecord using removeRecord(Record). The SOARecord should remain untouched, so getSOA() must still return the original SOARecord."
        }
      ]
    },
    {
      "group": "SOA retrieval - zone cleared via iterator removal",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Add an SOARecord to the Zone, obtain an iterator over the Zone's RRsets, and remove every RRset via the iterator's remove() method (or by calling removeRecord on each Record). After all records are removed, getSOA() should return null, confirming that the internal SOA reference is cleared when the zone becomes empty."
        }
      ]
    }
  ]
}
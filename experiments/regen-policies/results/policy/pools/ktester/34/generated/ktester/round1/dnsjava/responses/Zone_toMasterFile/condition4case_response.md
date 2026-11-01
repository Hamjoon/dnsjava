{
  "groups": [
    {
      "group": "Origin node always rendered",
      "cases": [
        {
          "input": [],
          "expected": "ORIGIN:originNode; ",
          "description": "A Zone with a non‑null originNode and no other records. The lambda should call nodeToString once for the originNode, producing the marker \"ORIGIN:originNode; \"."
        },
        {
          "input": [],
          "expected": "ORIGIN:originNode; ",
          "description": "A Zone where the data map contains a single entry whose key equals the zone origin. The `if (!origin.equals(entry.getKey()))` branch must be false, therefore nodeToString is **not** invoked for that entry and the output contains only the originNode marker."
        }
      ]
    },
    {
      "group": "Non‑origin entries are rendered",
      "cases": [
        {
          "input": [],
          "expected": "ORIGIN:originNode; NODE:nodeA; ",
          "description": "A Zone with one extra node whose key differs from the origin. The loop iterates over the entry, the condition `!origin.equals(entry.getKey())` evaluates to true, so nodeToString is called for the entry value, adding the marker \"NODE:nodeA; \"."
        },
        {
          "input": [],
          "expected": "ORIGIN:originNode; NODE:nodeA; NODE:nodeB; ",
          "description": "A Zone with multiple entries, none of which match the origin. All entries satisfy the condition and nodeToString is invoked for each, resulting in concatenated markers for each node."
        }
      ]
    },
    {
      "group": "Empty zone (no data entries)",
      "cases": [
        {
          "input": [],
          "expected": "ORIGIN:originNode; ",
          "description": "When the `data` map is empty, the for‑each loop does not execute. The only output comes from the initial call to nodeToString for the originNode."
        }
      ]
    },
    {
      "group": "Null originNode handling",
      "cases": [
        {
          "input": [],
          "expected": "",
          "description": "If `originNode` is null, the overridden `nodeToString` implementation ignores null values (does not append anything). With an empty data map the final string is empty."
        },
        {
          "input": [],
          "expected": "NODE:nodeA; ",
          "description": "When `originNode` is null but the map contains a non‑origin entry, the first call to nodeToString does nothing, while the second call (for the entry) appends its marker, yielding only \"NODE:nodeA; \"."
        }
      ]
    }
  ]
}
{
    "testCases": [
        {
            "group": "Origin node only",
            "cases": [
                {
                    "input": [],
                    "expected": "ORIGIN_NODE\n",
                    "description": "Zone with a non‑null originNode and an empty data map. The method should invoke nodeToString once for the originNode and return its string representation only."
                }
            ]
        },
        {
            "group": "Origin node plus one additional node",
            "cases": [
                {
                    "input": [],
                    "expected": "ORIGIN_NODE\nADDITIONAL_NODE\n",
                    "description": "Zone where originNode is set and data contains a single entry whose key is different from the origin. The method should serialize the originNode first, then the additional node, preserving order."
                }
            ]
        },
        {
            "group": "Multiple nodes with origin excluded",
            "cases": [
                {
                    "input": [],
                    "expected": "ORIGIN_NODE\nNODE_A\nNODE_B\nNODE_C\n",
                    "description": "Zone with originNode and three distinct data entries (keys A, B, C) none of which equal the origin. All nodes must be serialized after the originNode, in the natural order of the TreeMap."
                }
            ]
        },
        {
            "group": "Data contains only the origin key",
            "cases": [
                {
                    "input": [],
                    "expected": "ORIGIN_NODE\n",
                    "description": "Zone where data contains an entry whose key equals the origin. The loop must skip this entry, resulting in output that contains only the originNode serialization."
                }
            ]
        },
        {
            "group": "Null originNode handling",
            "cases": [
                {
                    "input": [],
                    "expected": "NODE_X\n",
                    "description": "Zone with a null originNode (nodeToString should safely ignore null) and a single data entry X. The method should produce output only for the data node X."
                }
            ]
        },
        {
            "group": "Null origin and null originNode",
            "cases": [
                {
                    "input": [],
                    "expected": "",
                    "description": "Zone where both origin and originNode are null and data map is empty. The method should return an empty string without throwing NullPointerException."
                }
            ]
        }
    ]
}
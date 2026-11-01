{
  "group": "Input validation",
  "cases": [
    {
      "input": [
        {
          "parameter": "name",
          "value": null
        },
        {
          "parameter": "type",
          "value": 1
        }
      ],
      "expected": "IllegalArgumentException with message \"name must not be null\"",
      "description": "Calling findRecords with a null Name should fail early with an IllegalArgumentException."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "www.example.com."
        },
        {
          "parameter": "type",
          "value": -1
        }
      ],
      "expected": "IllegalArgumentException thrown by Type.check(int)",
      "description": "An invalid DNS type (negative value) should be rejected by Type.check, causing an IllegalArgumentException."
    }
  ]
}
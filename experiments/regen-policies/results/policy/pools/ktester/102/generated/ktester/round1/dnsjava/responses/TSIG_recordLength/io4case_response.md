{
  "group": "recordLength",
  "cases": [
    {
      "input": [
        { "parameter": "name", "value": "example.com." },
        { "parameter": "algorithm", "value": "HMAC-MD5.SIG-ALG.REG.INT." }
      ],
      "expected": 87,
      "description": "Standard case with MD5 algorithm and a typical domain name. Expected length = name(13) + alg(26) + mac(16) + constant(32) = 87."
    },
    {
      "input": [
        { "parameter": "name", "value": "a." },
        { "parameter": "algorithm", "value": "hmac-sha256." }
      ],
      "expected": 80,
      "description": "Shortest possible domain name (single‑character label) with SHA‑256 algorithm. Expected length = name(3) + alg(13) + mac(32) + 32 = 80."
    },
    {
      "input": [
        { "parameter": "name", "value": "longdomainname.example.org." },
        { "parameter": "algorithm", "value": "hmac-sha256-128." }
      ],
      "expected": 93,
      "description": "Domain name with three labels and an algorithm that truncates the MAC to 128‑bit (16 bytes). Expected length = name(28) + alg(17) + mac(16) + 32 = 93."
    },
    {
      "input": [
        { "parameter": "name", "value": "example.com." },
        { "parameter": "algorithm", "value": "gss-tsig." }
      ],
      "expected": 55,
      "description": "GSS‑TSIG algorithm which is mapped to a MAC length of 0 bytes. Expected length = name(13) + alg(10) + mac(0) + 32 = 55."
    },
    {
      "input": [
        { "parameter": "name", "value": "<255‑byte‑wire‑format‑name>" },
        { "parameter": "algorithm", "value": "hmac-sha1." }
      ],
      "expected": 318,
      "description": "Maximum‑size DNS name (255 bytes in wire format) with SHA‑1 algorithm (20‑byte MAC). Expected length = name(255) + alg(11) + mac(20) + 32 = 318."
    },
    {
      "input": [
        { "parameter": "name", "value": null },
        { "parameter": "algorithm", "value": "hmac-sha256." }
      ],
      "expected": "NullPointerException",
      "description": "Passing a null Name should cause a NullPointerException because recordLength dereferences name.length()."
    },
    {
      "input": [
        { "parameter": "name", "value": "example.com." },
        { "parameter": "algorithm", "value": null }
      ],
      "expected": "NullPointerException",
      "description": "Passing a null algorithm Name should cause a NullPointerException because recordLength dereferences alg.length() and algLengthMap.get(alg)."
    }
  ]
}
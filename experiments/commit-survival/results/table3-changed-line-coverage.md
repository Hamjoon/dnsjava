# Table 3. Changed-line coverage

Coverage uses methods alive entering each index. Changed-line coordinates are the post-image added/modified lines. Build failures and missing coverage remain unavailable.

## All files

| Index | Subject | Executable lines | Dev (scope) covered | KTester covered | Ouédraogo covered | Dev (scope) lost | KTester lost | Ouédraogo lost |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | Pluggable I/O for SimpleResolver | 33 | 21 (63.6%) | 0 (0.0%) | 0 (0.0%) | 22 | 0 | 0 |
| 2 | Remove unreachable code. | 3 | 3 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 5 | Pass the executor from the initial to all subsequent requests | 31 | 23 (74.2%) | 0 (0.0%) | 0 (0.0%) | 204 | 0 | 0 |
| 6 | Include detailed information in exceptions | 23 | 9 (39.1%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 7 | Add ARecord ctor with byte[] | 5 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 8 | Update dependencies | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 9 | Handle selector errors to close UDP sockets | 12 | 6 (50.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 10 | Do not throw if NioClient was never initialized | 17 | 13 (76.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 11 | Remove mix of how SetResponse is constructed | 52 | 37 (71.2%) | 0 (0.0%) | 32 (61.5%) | 0 | 0 | 0 |
| 13 | Extend exceptions for failed lookups | 19 | 13 (68.4%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 15 | CVE-2024-25638: Message normalization | 330 | 183 (55.5%) | 22 (6.7%) | 23 (7.0%) | 5 | 13 | 1 |
| 16 | Extended logging for signature verification | 27 | 9 (33.3%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 17 | Normalize message before DNSSEC validation | 7 | 3 (42.9%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 18 | CVE-2023-50387 / KeyTrap: Limit number of RRsigs validated | 54 | 10 (18.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 19 | Add new DNSSEC algorithms and digest mnemonics | 7 | 7 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 20 | Enforce minimum RSA key size of 1024 by default | 24 | 14 (58.3%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 25 | CVE-2023-50868 / KeyTrap: NSEC3 closest encloser proof can exhaust CPU | 324 | 142 (43.8%) | 0 (0.0%) | 0 (0.0%) | 2 | 0 | 0 |
| 27 | Prevent exceptions during concurrent selector runs | 6 | 2 (33.3%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 28 | DDNS response messages do not include the question | 22 | 5 (22.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 29 | Add AAAARecord ctor with byte[] | 5 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 30 | Replace synchronization in Zone with locks | 202 | 52 (25.7%) | 54 (26.7%) | 133 (65.8%) | 0 | 0 | 0 |
| 32 | Fix links to RFCs and IANA registries | 2 | 1 (50.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 37 | Properly handle cached xNAME responses in LookupSession | 7 | 3 (42.9%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 42 | Zone now constructs successfully even when the input records include a SOA with an RRSIG. | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 47 | Fix compiling on Java 8 | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 50 | Support custom hosts file size (#349) | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 51 | Fix origin handling in zone loaded from file or stream | 4 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 52 | Apply code formatting | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 53 | Close TCP connections, not just transactions, when closing IO | 4 | 1 (25.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 54 | Fixed confusing parameter name in CNAMERecord (#354) | 1 | 1 (100.0%) | 0 (0.0%) | 1 (100.0%) | 0 | 0 | 0 |
| 55 | Optionally disable ShutdownHook in NioClient (#359) | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 56 | Add support for new TSIG algorithms from RFC 8945 | 48 | 27 (56.2%) | 42 (87.5%) | 0 (0.0%) | 0 | 3 | 0 |
| 57 | Fix TSIG record signature length and max length violation | 1 | 0 (0.0%) | 1 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 58 | Apply code formatting, fix check in Actions | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 62 | Properly handle write failures on full TCP output buffer | 34 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 64 | Disable UPDATE message truncation | 21 | 12 (57.1%) | 4 (19.0%) | 4 (19.0%) | 0 | 0 | 0 |
| 65 | Fix DoH initial request using recommended nanoTime calculation | 10 | 6 (60.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 66 | Replace deprecated URL constructor | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 67 | Fix compiler warnings about unsigned conversions | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 68 | Fix or suppress Sonar-reported issues | 321 | 149 (46.4%) | 30 (9.3%) | 27 (8.4%) | 0 | 0 | 0 |
| 71 | Handle null in setSearchPath overloads equally | 21 | 5 (23.8%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 73 | Split DoH Resolver into Java versions, fix initial request and tests | 144 | 92 (63.9%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 74 | Run NIO close in the selector thread to prevent CME | 55 | 44 (80.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 75 | Reduce hosts file parsing warnings | 20 | 6 (30.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 76 | Reduce lock contention and disk access in hosts file parsing | 43 | 32 (74.4%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 77 | Fix section size in header after message normalization | 21 | 19 (90.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 78 | Unwrap completion exception for legacy async handling | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 79 | Fix Java cast warnings | 9 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 80 | Suppress or replace generic array creation | 9 | 9 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 81 | Improve log message | 1 | 1 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 84 | Prevent identical queries for names exceeding ndots | 8 | 8 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 85 | Fix and validate Zone serialization via Java objects | 3 | 3 (100.0%) | 3 (100.0%) | 3 (100.0%) | 0 | 0 | 0 |
| 86 | Prevent NPE during shutdown when the hook is already released | 6 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 93 | Handle interrupts on NIO selector thread | 11 | 5 (45.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 95 | Handle canceled keys in UDP client | 35 | 15 (42.9%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 96 | Handle invalid/uninitialized file times | 8 | 6 (75.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 99 | Add a few new types/flags/options | 3 | 3 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 101 | Add getXxxAsByteArray() getters and overload with escape boolean | 31 | 3 (9.7%) | 3 (9.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 105 | nio: fix selector thread from self-joining | 17 | 15 (88.2%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 108 | RCode: update documentation with current RFCs | 1 | 1 (100.0%) | 0 (0.0%) | 1 (100.0%) | 0 | 0 | 0 |
| 110 | TSIG: use original message ID in signature (RFC 8945, 4.3.2) | 23 | 1 (4.3%) | 6 (26.1%) | 0 (0.0%) | 0 | 0 | 0 |
| 111 | hosts: resolve PTR queries from the local hosts database (#413) | 59 | 26 (44.1%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 112 | Handle canceled keys in TCP client (#411) | 4 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 115 | Validate SSHFP fingerprint lengths (#415) | 26 | 12 (46.2%) | 6 (23.1%) | 0 (0.0%) | 0 | 0 | 0 |
| 116 | Add dnsjava.secure_random_algorithm property to select SecureRandom | 9 | 6 (66.7%) | 5 (55.6%) | 5 (55.6%) | 0 | 0 | 0 |
| 118 | Generating GOST R 34.11-2012 digest | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 119 | Add unit tests (#412) | 28 | 9 (32.1%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 123 | Include Other Data in BADTIME response signatures | 10 | 0 (0.0%) | 5 (50.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 124 | Prevent accepting Unicode digits when ASCII is expected | 15 | 10 (66.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 125 | Prevent unbounded pre-allocation via unvalidated DNS header count | 1 | 1 (100.0%) | 1 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 126 | Fix CNAME processing in Message normalization | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 127 | DoH Resolver: validate Name/DClass/QType of response against query | 34 | 20 (58.8%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 128 | Clean header counts of truncated messages | 3 | 2 (66.7%) | 2 (66.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 130 | Fix name compression when message is truncated | 14 | 12 (85.7%) | 1 (7.1%) | 1 (7.1%) | 0 | 0 | 0 |
| 131 | Fix Zone record removal when record data is not part of the Zone | 4 | 0 (0.0%) | 0 (0.0%) | 3 (75.0%) | 0 | 0 | 0 |
| 132 | Skip invalid characters when parsing base64 | 10 | 10 (100.0%) | 10 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 133 | IPv4-mapped IPv6 address must roundtrip | 26 | 4 (15.4%) | 2 (7.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 134 | Validate SVCB port range | 4 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 135 | Fix CERT RR Certificate Types mnemonics mapping | 5 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 137 | svcb: new parameters since RFC 9460 / RFC 9848 | 7 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 138 | svcb: fix char-string / comma list parsing | 138 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 139 | Validate empty hex/base32 encoded fields | 24 | 11 (45.8%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 140 | Remove length limit for URI and CAA records | 12 | 2 (16.7%) | 2 (16.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 141 | ttl: reject BIND-style values without a unit at the end | 7 | 4 (57.1%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 142 | generator: fix range in expand() | 3 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 143 | Update: forward DClass to zone (question) record | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 0 | 0 |
| 145 | reverse map: reject invalid IPv4 labels | 4 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 146 | Message: validate section in addRecord | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 3 | 0 |
| 147 | Record: limit TTL to 2^31 - 1 as per RFC 2181, section 8 | 2 | 1 (50.0%) | 1 (50.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 148 | Header: allow 0xffff in randomly generated IDs | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 0 | 0 |
| 149 | LOC: fix rdata formatting, validate size | 25 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 150 | wks: fix sorting of services in ascending order | 3 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 151 | Validate u8/u16/u32 output as 2^n-1 | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 0 | 0 |
| 152 | dnssec: nsec(3): reject out-of-order Type Bitmap window blocks | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 153 | Validate that required fields are present | 40 | 4 (10.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 154 | nsap: ensure invalid input throws only TextParseExceptions | 3 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 155 | hip: ensure relative names are interpreted as absolute | 4 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 156 | sig: reject date/times before Unix Epoch | 8 | 4 (50.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 157 | Parsing using tokenizers must report the error location | 16 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 158 | opt: ensure equals contract is fulfilled | 3 | 2 (66.7%) | 3 (100.0%) | 2 (66.7%) | 0 | 0 | 0 |
| 159 | Message: enforce toWire length | 3 | 2 (66.7%) | 2 (66.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 160 | txt / tokenizer, records: clarify string encoding in doc | 6 | 0 (0.0%) | 2 (33.3%) | 0 (0.0%) | 0 | 0 | 0 |
| 161 | Use constant time comparison for TSIG macs | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 162 | CAA: validate contents of tag, must be lowercase ASCII and digits | 15 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 163 | Always use extended RCode instead of header-only | 7 | 4 (57.1%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 164 | Don't synthesize wildcard below an existing closer encloser | 13 | 12 (92.3%) | 0 (0.0%) | 7 (53.8%) | 0 | 0 | 0 |
| 165 | zone: return NODATA for ENT and wildcards when another type exists | 33 | 19 (57.6%) | 14 (42.4%) | 27 (81.8%) | 1 | 0 | 0 |
| 166 | cache: positive entries should replace negatives | 8 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 168 | generator: refuse expansion resulting in invalid values | 10 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 169 | generator: make modifiers optional | 77 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 170 | Message.toWire must not throw when TC is already set and it fits | 3 | 2 (66.7%) | 2 (66.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 171 | Various new flags, codes for EDNS, DNSKEY | 16 | 16 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 175 | Reuse the declaration of the array type | 5 | 3 (60.0%) | 2 (40.0%) | 2 (40.0%) | 0 | 0 | 0 |
| 177 | Quoting TXT rdata containing semicolons and parentheses | 1 | 0 (0.0%) | 1 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 178 | Generating SM3 digest | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| Total | Common measured commits | 2905 | 1236 (42.5%) | 234 (8.1%) | 279 (9.6%) | 234 | 19 | 1 |

## Scope files

| Index | Subject | Executable lines | Dev (scope) covered | KTester covered | Ouédraogo covered | Dev (scope) lost | KTester lost | Ouédraogo lost |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 11 | Remove mix of how SetResponse is constructed | 10 | 5 (50.0%) | 0 (0.0%) | 7 (70.0%) | 0 | 0 | 0 |
| 15 | CVE-2024-25638: Message normalization | 198 | 110 (55.6%) | 17 (8.6%) | 17 (8.6%) | 5 | 13 | 1 |
| 17 | Normalize message before DNSSEC validation | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 30 | Replace synchronization in Zone with locks | 193 | 52 (26.9%) | 54 (28.0%) | 132 (68.4%) | 0 | 0 | 0 |
| 42 | Zone now constructs successfully even when the input records include a SOA with an RRSIG. | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 47 | Fix compiling on Java 8 | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 51 | Fix origin handling in zone loaded from file or stream | 4 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 56 | Add support for new TSIG algorithms from RFC 8945 | 48 | 27 (56.2%) | 42 (87.5%) | 0 (0.0%) | 0 | 3 | 0 |
| 57 | Fix TSIG record signature length and max length violation | 1 | 0 (0.0%) | 1 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 64 | Disable UPDATE message truncation | 8 | 6 (75.0%) | 1 (12.5%) | 1 (12.5%) | 0 | 0 | 0 |
| 67 | Fix compiler warnings about unsigned conversions | 2 | 2 (100.0%) | 2 (100.0%) | 2 (100.0%) | 0 | 0 | 0 |
| 68 | Fix or suppress Sonar-reported issues | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 77 | Fix section size in header after message normalization | 21 | 19 (90.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 85 | Fix and validate Zone serialization via Java objects | 3 | 3 (100.0%) | 3 (100.0%) | 3 (100.0%) | 0 | 0 | 0 |
| 110 | TSIG: use original message ID in signature (RFC 8945, 4.3.2) | 23 | 1 (4.3%) | 6 (26.1%) | 0 (0.0%) | 0 | 0 | 0 |
| 115 | Validate SSHFP fingerprint lengths (#415) | 6 | 6 (100.0%) | 6 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 123 | Include Other Data in BADTIME response signatures | 10 | 0 (0.0%) | 5 (50.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 125 | Prevent unbounded pre-allocation via unvalidated DNS header count | 1 | 1 (100.0%) | 1 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 126 | Fix CNAME processing in Message normalization | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 128 | Clean header counts of truncated messages | 2 | 2 (100.0%) | 2 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 130 | Fix name compression when message is truncated | 5 | 5 (100.0%) | 1 (20.0%) | 1 (20.0%) | 0 | 0 | 0 |
| 131 | Fix Zone record removal when record data is not part of the Zone | 4 | 0 (0.0%) | 0 (0.0%) | 3 (75.0%) | 0 | 0 | 0 |
| 146 | Message: validate section in addRecord | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 3 | 0 |
| 159 | Message: enforce toWire length | 3 | 2 (66.7%) | 2 (66.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 161 | Use constant time comparison for TSIG macs | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 164 | Don't synthesize wildcard below an existing closer encloser | 13 | 12 (92.3%) | 0 (0.0%) | 7 (53.8%) | 0 | 0 | 0 |
| 165 | zone: return NODATA for ENT and wildcards when another type exists | 33 | 19 (57.6%) | 14 (42.4%) | 27 (81.8%) | 1 | 0 | 0 |
| 170 | Message.toWire must not throw when TC is already set and it fits | 3 | 2 (66.7%) | 2 (66.7%) | 0 (0.0%) | 0 | 0 | 0 |
| Total | Common measured commits | 601 | 277 (46.1%) | 162 (27.0%) | 203 (33.8%) | 6 | 19 | 1 |

## Other files

| Index | Subject | Executable lines | Dev (scope) covered | KTester covered | Ouédraogo covered | Dev (scope) lost | KTester lost | Ouédraogo lost |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | Pluggable I/O for SimpleResolver | 33 | 21 (63.6%) | 0 (0.0%) | 0 (0.0%) | 22 | 0 | 0 |
| 2 | Remove unreachable code. | 3 | 3 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 5 | Pass the executor from the initial to all subsequent requests | 31 | 23 (74.2%) | 0 (0.0%) | 0 (0.0%) | 204 | 0 | 0 |
| 6 | Include detailed information in exceptions | 23 | 9 (39.1%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 7 | Add ARecord ctor with byte[] | 5 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 8 | Update dependencies | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 9 | Handle selector errors to close UDP sockets | 12 | 6 (50.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 10 | Do not throw if NioClient was never initialized | 17 | 13 (76.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 11 | Remove mix of how SetResponse is constructed | 42 | 32 (76.2%) | 0 (0.0%) | 25 (59.5%) | 0 | 0 | 0 |
| 13 | Extend exceptions for failed lookups | 19 | 13 (68.4%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 15 | CVE-2024-25638: Message normalization | 132 | 73 (55.3%) | 5 (3.8%) | 6 (4.5%) | 5 | 13 | 1 |
| 16 | Extended logging for signature verification | 27 | 9 (33.3%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 17 | Normalize message before DNSSEC validation | 5 | 3 (60.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 18 | CVE-2023-50387 / KeyTrap: Limit number of RRsigs validated | 54 | 10 (18.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 19 | Add new DNSSEC algorithms and digest mnemonics | 7 | 7 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 20 | Enforce minimum RSA key size of 1024 by default | 24 | 14 (58.3%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 25 | CVE-2023-50868 / KeyTrap: NSEC3 closest encloser proof can exhaust CPU | 324 | 142 (43.8%) | 0 (0.0%) | 0 (0.0%) | 2 | 0 | 0 |
| 27 | Prevent exceptions during concurrent selector runs | 6 | 2 (33.3%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 28 | DDNS response messages do not include the question | 22 | 5 (22.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 29 | Add AAAARecord ctor with byte[] | 5 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 30 | Replace synchronization in Zone with locks | 9 | 0 (0.0%) | 0 (0.0%) | 1 (11.1%) | 0 | 0 | 0 |
| 32 | Fix links to RFCs and IANA registries | 2 | 1 (50.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 37 | Properly handle cached xNAME responses in LookupSession | 7 | 3 (42.9%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 47 | Fix compiling on Java 8 | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 50 | Support custom hosts file size (#349) | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 52 | Apply code formatting | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 53 | Close TCP connections, not just transactions, when closing IO | 4 | 1 (25.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 54 | Fixed confusing parameter name in CNAMERecord (#354) | 1 | 1 (100.0%) | 0 (0.0%) | 1 (100.0%) | 0 | 0 | 0 |
| 55 | Optionally disable ShutdownHook in NioClient (#359) | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 58 | Apply code formatting, fix check in Actions | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 62 | Properly handle write failures on full TCP output buffer | 34 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 64 | Disable UPDATE message truncation | 13 | 6 (46.2%) | 3 (23.1%) | 3 (23.1%) | 0 | 0 | 0 |
| 65 | Fix DoH initial request using recommended nanoTime calculation | 10 | 6 (60.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 66 | Replace deprecated URL constructor | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 68 | Fix or suppress Sonar-reported issues | 319 | 149 (46.7%) | 30 (9.4%) | 27 (8.5%) | 0 | 0 | 0 |
| 71 | Handle null in setSearchPath overloads equally | 21 | 5 (23.8%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 73 | Split DoH Resolver into Java versions, fix initial request and tests | 144 | 92 (63.9%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 74 | Run NIO close in the selector thread to prevent CME | 55 | 44 (80.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 75 | Reduce hosts file parsing warnings | 20 | 6 (30.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 76 | Reduce lock contention and disk access in hosts file parsing | 43 | 32 (74.4%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 78 | Unwrap completion exception for legacy async handling | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 79 | Fix Java cast warnings | 9 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 80 | Suppress or replace generic array creation | 9 | 9 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 81 | Improve log message | 1 | 1 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 84 | Prevent identical queries for names exceeding ndots | 8 | 8 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 86 | Prevent NPE during shutdown when the hook is already released | 6 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 93 | Handle interrupts on NIO selector thread | 11 | 5 (45.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 95 | Handle canceled keys in UDP client | 35 | 15 (42.9%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 96 | Handle invalid/uninitialized file times | 8 | 6 (75.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 99 | Add a few new types/flags/options | 3 | 3 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 101 | Add getXxxAsByteArray() getters and overload with escape boolean | 31 | 3 (9.7%) | 3 (9.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 105 | nio: fix selector thread from self-joining | 17 | 15 (88.2%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 108 | RCode: update documentation with current RFCs | 1 | 1 (100.0%) | 0 (0.0%) | 1 (100.0%) | 0 | 0 | 0 |
| 111 | hosts: resolve PTR queries from the local hosts database (#413) | 59 | 26 (44.1%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 112 | Handle canceled keys in TCP client (#411) | 4 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 115 | Validate SSHFP fingerprint lengths (#415) | 20 | 6 (30.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 116 | Add dnsjava.secure_random_algorithm property to select SecureRandom | 9 | 6 (66.7%) | 5 (55.6%) | 5 (55.6%) | 0 | 0 | 0 |
| 118 | Generating GOST R 34.11-2012 digest | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 119 | Add unit tests (#412) | 28 | 9 (32.1%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 124 | Prevent accepting Unicode digits when ASCII is expected | 15 | 10 (66.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 127 | DoH Resolver: validate Name/DClass/QType of response against query | 34 | 20 (58.8%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 128 | Clean header counts of truncated messages | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 130 | Fix name compression when message is truncated | 9 | 7 (77.8%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 132 | Skip invalid characters when parsing base64 | 10 | 10 (100.0%) | 10 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 133 | IPv4-mapped IPv6 address must roundtrip | 26 | 4 (15.4%) | 2 (7.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 134 | Validate SVCB port range | 4 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 135 | Fix CERT RR Certificate Types mnemonics mapping | 5 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 137 | svcb: new parameters since RFC 9460 / RFC 9848 | 7 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 138 | svcb: fix char-string / comma list parsing | 138 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 139 | Validate empty hex/base32 encoded fields | 24 | 11 (45.8%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 140 | Remove length limit for URI and CAA records | 12 | 2 (16.7%) | 2 (16.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 141 | ttl: reject BIND-style values without a unit at the end | 7 | 4 (57.1%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 142 | generator: fix range in expand() | 3 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 143 | Update: forward DClass to zone (question) record | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 0 | 0 |
| 145 | reverse map: reject invalid IPv4 labels | 4 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 147 | Record: limit TTL to 2^31 - 1 as per RFC 2181, section 8 | 2 | 1 (50.0%) | 1 (50.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 148 | Header: allow 0xffff in randomly generated IDs | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 0 | 0 |
| 149 | LOC: fix rdata formatting, validate size | 25 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 150 | wks: fix sorting of services in ascending order | 3 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 151 | Validate u8/u16/u32 output as 2^n-1 | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 0 | 0 |
| 152 | dnssec: nsec(3): reject out-of-order Type Bitmap window blocks | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 153 | Validate that required fields are present | 40 | 4 (10.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 154 | nsap: ensure invalid input throws only TextParseExceptions | 3 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 155 | hip: ensure relative names are interpreted as absolute | 4 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 156 | sig: reject date/times before Unix Epoch | 8 | 4 (50.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 157 | Parsing using tokenizers must report the error location | 16 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 158 | opt: ensure equals contract is fulfilled | 3 | 2 (66.7%) | 3 (100.0%) | 2 (66.7%) | 0 | 0 | 0 |
| 160 | txt / tokenizer, records: clarify string encoding in doc | 6 | 0 (0.0%) | 2 (33.3%) | 0 (0.0%) | 0 | 0 | 0 |
| 162 | CAA: validate contents of tag, must be lowercase ASCII and digits | 15 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 163 | Always use extended RCode instead of header-only | 7 | 4 (57.1%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 166 | cache: positive entries should replace negatives | 8 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 168 | generator: refuse expansion resulting in invalid values | 10 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 169 | generator: make modifiers optional | 77 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 171 | Various new flags, codes for EDNS, DNSKEY | 16 | 16 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 175 | Reuse the declaration of the array type | 5 | 3 (60.0%) | 2 (40.0%) | 2 (40.0%) | 0 | 0 | 0 |
| 177 | Quoting TXT rdata containing semicolons and parentheses | 1 | 0 (0.0%) | 1 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 178 | Generating SM3 digest | 2 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| Total | Common measured commits | 2304 | 959 (41.6%) | 72 (3.1%) | 76 (3.3%) | 233 | 13 | 1 |

Production-change commits: 130. Commits with zero executable changed lines: 12. Commits without an executable-line measurement: 3.

You are a professional software tester. Write test cases for a focal method in a target class.

**Instructions:**
1. Analyze method behavior and identify ALL possible exception scenarios
2. Design test cases that trigger each identified exception
3. Follow existing test case structure, Avoid duplication and create new test cases for uncovered exception scenarios

Test Case Format:
1. Group test cases by test function name
2. Each test case MUST include:
   - Input parameters with values (use empty list `[]` if none)
   - Expected exception or observable behavior
   - Clear description of test scenario
3. Return empty array if no exception scenarios found

Output Schema (STRICT JSON format):
```json
[
    {
        "group": "test name",
        "cases": [
            {
                "input": [
                    {
                        "parameter": "param name",
                        "value": "param value"
                    }
                ],
                "expected": "expected exception or behavior",
                "description": "test scenario description"
            }
        ]
    },
    {...}
]
```

@input{focal method and target class}
Here is the code of focal method, `algorithmToName(String)`, with the focal class `TSIG`:
```Java
package org.xbill.DNS;
import java.util.regex.Pattern;
import java.time.Instant;
import java.util.HashMap;
import java.util.regex.Matcher;
import lombok.Getter;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Clock;
import java.util.TreeMap;
import javax.crypto.SecretKey;
import javax.crypto.Mac;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.spec.SecretKeySpec;
import org.xbill.DNS.utils.base64;
import java.util.Objects;
import org.xbill.DNS.utils.hexdump;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
@Slf4j
public class TSIG {
    public static final Name GSS_TSIG = Name.fromConstantString("gss-tsig.");
    public static final Name HMAC_MD5 = Name.fromConstantString("HMAC-MD5.SIG-ALG.REG.INT.");
    public static final Name HMAC_SHA1 = Name.fromConstantString("hmac-sha1.");
    public static final Name HMAC_SHA224 = Name.fromConstantString("hmac-sha224.");
    public static final Name HMAC_SHA256 = Name.fromConstantString("hmac-sha256.");
    public static final Name HMAC_SHA384 = Name.fromConstantString("hmac-sha384.");
    public static final Name HMAC_SHA512 = Name.fromConstantString("hmac-sha512.");
    public static final Name HMAC_SHA256_128 = Name.fromConstantString("hmac-sha256-128.");
    public static final Name HMAC_SHA384_192 = Name.fromConstantString("hmac-sha384-192.");
    public static final Name HMAC_SHA512_256 = Name.fromConstantString("hmac-sha512-256.");
    private static final Map<Name, String> algMap;
    private static final Pattern javaAlgNamePattern = Pattern.compile("^Hmac(?<alg>(SHA(1|\\d{3})|MD5))(/(?<length>\\d{3}))?$", Pattern.CASE_INSENSITIVE);
    public static final Duration FUDGE = Duration.ofSeconds(300);
    public static Name algorithmToName(String alg) {
        if (alg == null) {
            throw new IllegalArgumentException("Null algorithm");
        }
        // Handle Java algorithm names
        if (!alg.contains("-")) {
            Matcher m = javaAlgNamePattern.matcher(alg);
            if (m.matches()) {
                alg = "hmac-" + m.group("alg");
                String truncatedLength = m.group("length");
                if (truncatedLength != null) {
                    alg += "-" + truncatedLength;
                }
            }
        }
        if (!alg.endsWith(".")) {
            alg += ".";
        }
        Name nameAlg;
        try {
            nameAlg = Name.fromString(alg);
        } catch (TextParseException e) {
            throw new IllegalArgumentException(e);
        }
        // Special case, allow "hmac-md5" as an alias for the RFC name.
        if (nameAlg.equals(Name.fromConstantString("hmac-md5."))) {
            return HMAC_MD5;
        }
        // Make sure we understand this name
        if (algMap.get(nameAlg) == null) {
            throw new IllegalArgumentException("Unknown algorithm: " + nameAlg);
        }
        return nameAlg;
    }
public void apply(Message m, int error, TSIGRecord old);
    public void apply(Message m, int error, TSIGRecord old, boolean fullSignature);
    public void apply(Message m, TSIGRecord old);
    public void apply(Message m, TSIGRecord old, boolean fullSignature);
    public TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old);
    public TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old, boolean fullSignature);
    public int recordLength();
    public int verify(Message m, byte[] messageBytes, TSIGRecord requestTSIG);
    public int verify(Message m, byte[] messageBytes, TSIGRecord requestTSIG, boolean fullSignature);
}
```
@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```

@output{test cases}: output the test cases in the JSON format:
You are a professional Java software testing expert. Your task is to generate high-quality unit test code for a focal method, using **JUnit5** and **Mockito** libraries. 

You'll be provided with:
1. The focal method to test.
2. The target class's structure (fields and methods).
3. A pre-defined test class framework with `@Before`/`@After` methods.
4. Test cases in JSON format.
5. Any relevant context.

**Requirements:**

Code Quality:
- Include clear comments explaining each test scenario.
- Ensure code is clean, understandable, and maintainable.
- Do not directly access private methods or fields. **Use reflection if the focal method is private.**
- Include all necessary imports; code must be free of syntax errors.

**Test Structure:**
- Follow the given test class framework; do not modify `@Before`/`@After` methods.
- Create one test function per test case group, ensuring all cases are covered.
- Use parameterized tests where appropriate.
- Reinitialize variables inside test functions if `@Setup` values don't fit.
- Properly mock and set up dependencies.

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

@input{initial test class framework}
```Java
<initial_class>
```

@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```


@input{context information}

api document of class org.xbill.DNS.TSIG: 

 * Transaction signature handling. This class generates and verifies TSIG records on messages, which
 * provide transaction security.
 *
 * @see TSIGRecord
 * @author Brian Wellington
 

api document of method algorithmToName(String): 

   * Convert an algorithm String to its equivalent Name.
   *
   * @param alg String containing name of algorithm.
   * @return Name object for algorithm
   * @throws IllegalArgumentException The algorithm is null or invalid.
   

return type: 
org.xbill.DNS.Name

dependent classes: 
class `org.xbill.DNS.TSIG`:
	dependent fields:
		org.xbill.DNS.Name org.xbill.DNS.TSIG.HMAC_MD5;
		java.util.Map<org.xbill.DNS.Name, java.lang.String> org.xbill.DNS.TSIG.algMap;
		java.util.regex.Pattern org.xbill.DNS.TSIG.javaAlgNamePattern;
	
class `org.xbill.DNS.Zone`:
	related functions:
		method `public void addRecord(T)` returns `void`, related with `org.xbill.DNS.TSIG.algorithmToName(String)`, api document: 
   * Adds a record to the zone. If there is an existing {@link RRset} of the same {@link Name} and
   * {@link Type}, the record is <b>added</b> to this set.
   *
   * @param r The record to add.
   * @throws IllegalArgumentException if {@code name} is {@code null} or if the rrset name does not
   *     match the zone origin.
   ;
		method `private void removeRRsetWithoutLock(Name, int)` returns `void`, related with `org.xbill.DNS.TSIG.algorithmToName(String)`;
class `org.xbill.DNS.hosts.HostsFileParser`:
	related functions:
		method `public Optional<InetAddress> getAddressForHost(Name, int)` returns `java.util.Optional<java.net.InetAddress>`, related with `org.xbill.DNS.TSIG.algorithmToName(String)`, api document: 
   * Performs on-demand parsing and caching of the local hosts database.
   *
   * @param name the hostname to search for.
   * @param type Record type to search for, see {@link org.xbill.DNS.Type}.
   * @return The first address found for the requested hostname.
   * @throws IOException When the parsing fails.
   * @throws IllegalArgumentException when {@code type} is not {@link org.xbill.DNS.Type#A} or{@link
   *     org.xbill.DNS.Type#AAAA}.
   ;
class `org.xbill.DNS.DNSSEC.Digest`:
	related functions:
		method `public static int algLength(int)` returns `int`, related with `org.xbill.DNS.TSIG.algorithmToName(String)`, api document: 
     * Gets the length, in bytes, of the specified digest id.
     *
     * @return The length, in bytes, or -1 for an unknown digest.
     * @since 3.6
     ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.
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
Here is the code of focal method, `verify(Message, byte[], TSIGRecord, boolean)`, with the focal class `TSIG`:
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
    public static final Duration FUDGE = Duration.ofSeconds(300);
    public int verify(Message m, byte[] messageBytes, TSIGRecord requestTSIG, boolean fullSignature) {
        return verify(m, messageBytes, requestTSIG, fullSignature, null);
    }
public static Name algorithmToName(String alg);
    public void apply(Message m, int error, TSIGRecord old);
    public void apply(Message m, int error, TSIGRecord old, boolean fullSignature);
    public void apply(Message m, TSIGRecord old);
    public void apply(Message m, TSIGRecord old, boolean fullSignature);
    public TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old);
    public TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old, boolean fullSignature);
    public int recordLength();
    private static boolean verify(byte[] expected, byte[] signature);
    public int verify(Message m, byte[] messageBytes, TSIGRecord requestTSIG);
    private int verify(Message m, byte[] messageBytes, TSIGRecord requestTSIG, boolean fullSignature, Mac hmac);
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
 

api document of method verify(Message, byte[], TSIGRecord, boolean): 

   * Verifies a TSIG record on an incoming message. Since this is only called in the context where a
   * TSIG is expected to be present, it is an error if one is not present. After calling this
   * routine, Message.isVerified() may be called on this message.
   *
   * <p>Use {@link StreamVerifier} to validate multiple messages in a stream.
   *
   * @param m The message to verify
   * @param messageBytes An array containing the message in unparsed form. This is necessary since
   *     TSIG signs the message in wire format, and we can't recreate the exact wire format (with
   *     the same name compression).
   * @param requestTSIG If this message is a response, the TSIG from the request
   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and
   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for
   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).
   * @return The result of the verification (as an Rcode)
   * @see Rcode
   * @since 3.2
   

return type: 
int

dependent classes: 
class `org.xbill.DNS.Message`:
	
class `org.xbill.DNS.TSIGRecord`:
	
class `org.xbill.DNS.TSIG`:
	related functions:
		method `public int verify(Message, byte[], TSIGRecord, boolean)` returns `int`, related with `org.xbill.DNS.TSIG.verify(Message, byte[], TSIGRecord, boolean)`, api document: 
   * Verifies a TSIG record on an incoming message. Since this is only called in the context where a
   * TSIG is expected to be present, it is an error if one is not present. After calling this
   * routine, Message.isVerified() may be called on this message.
   *
   * <p>Use {@link StreamVerifier} to validate multiple messages in a stream.
   *
   * @param m The message to verify
   * @param messageBytes An array containing the message in unparsed form. This is necessary since
   *     TSIG signs the message in wire format, and we can't recreate the exact wire format (with
   *     the same name compression).
   * @param requestTSIG If this message is a response, the TSIG from the request
   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and
   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for
   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).
   * @return The result of the verification (as an Rcode)
   * @see Rcode
   * @since 3.2
   ;
class `org.xbill.DNS.TSIG.StreamVerifier`:
	related functions:
		method `public int verify(Message, byte[], boolean)` returns `int`, related with `org.xbill.DNS.TSIG.verify(Message, byte[], TSIGRecord, boolean)`, api document: 
     * Verifies a TSIG record on an incoming message that is part of a multiple message response.
     * TSIG records must be present on the first and last messages, and at least every 100 records
     * in between. After calling this routine, {@link Message#isVerified()} may be called on this
     * message.
     *
     * @param message The message
     * @param messageBytes The message in unparsed form
     * @param isLastMessage If true, verifies that the {@link Message} has an {@link TSIGRecord}.
     * @return The result of the verification (as an Rcode)
     * @see Rcode
     * @since 3.5.3
     ;
class `org.xbill.DNS.ExtendedFlags`:
	related functions:
		method `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.TSIG.verify(Message, byte[], TSIGRecord, boolean)`, api document:  Converts a numeric extended flag into a String ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.
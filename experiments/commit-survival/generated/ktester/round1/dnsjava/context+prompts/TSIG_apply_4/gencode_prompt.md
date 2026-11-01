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
Here is the code of focal method, `apply(Message, int, TSIGRecord, boolean)`, with the focal class `TSIG`:
```Java
package org.xbill.DNS;
import java.time.Instant;
import java.util.HashMap;
import lombok.Getter;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Clock;
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
    public static final Duration FUDGE = Duration.ofSeconds(300);
    public void apply(Message m, int error, TSIGRecord old, boolean fullSignature) {
        Record r = generate(m, m.toWire(), error, old, fullSignature);
        m.addRecord(r, Section.ADDITIONAL);
        m.tsigState = Message.TSIG_SIGNED;
    }
public static Name algorithmToName(String alg);
    public void apply(Message m, int error, TSIGRecord old);
    public void apply(Message m, TSIGRecord old);
    public void apply(Message m, TSIGRecord old, boolean fullSignature);
    public TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old);
    public TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old, boolean fullSignature);
    private TSIGRecord generate(Message m, byte[] b, int error, TSIGRecord old, boolean fullSignature, Mac hmac);
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
 

api document of method apply(Message, int, TSIGRecord, boolean): 

   * Generates a TSIG record with a specific error for a message and adds it to the message.
   *
   * @param m The message
   * @param error The error
   * @param old If this message is a response, the TSIG from the request
   * @param fullSignature {@code true} if this message is the first of many in a TCP connection and
   *     all TSIG variables (rfc2845, 3.4.2.) should be included in the signature. {@code false} for
   *     subsequent messages with reduced TSIG variables set (rfc2845, 4.4.).
   * @since 3.2
   

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		int org.xbill.DNS.Message.TSIG_SIGNED;
		int org.xbill.DNS.Message.tsigState;
	related functions:
		method `public boolean isSigned()` returns `boolean`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`, api document: 
   * Was this message signed by a TSIG?
   *
   * @see TSIG
   ;
class `org.xbill.DNS.TSIGRecord`:
	
class `org.xbill.DNS.Section`:
	dependent fields:
		int org.xbill.DNS.Section.ADDITIONAL;
	
class `org.xbill.DNS.TSIG.StreamGenerator`:
	related functions:
		method ` void generate(Message, boolean)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;
class `org.xbill.DNS.SimpleResolver`:
	related functions:
		method `private void applyEDNS(Message)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;
class `org.xbill.DNS.Update`:
	related functions:
		method `private void newPrereq(Record)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;
class `org.xbill.DNS.tools.update`:
	related functions:
		method ` void doGlue(Tokenizer)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;
class `org.xbill.DNS.SIG0`:
	related functions:
		method `public static void signMessage(Message, KEYRecord, PrivateKey, SIGRecord, Instant)` returns `void`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`, api document: 
   * Sign a message with SIG(0). The DNS key and private key must refer to the same underlying
   * cryptographic key.
   *
   * @param message The message to be signed
   * @param key The DNSKEY record to use as part of signing
   * @param privkey The PrivateKey to use when signing
   * @param previous If this message is a response, the SIG(0) from the query
   * @param timeSigned The time instant when the message has been signed.
   ;
class `org.xbill.DNS.tools.jnamed`:
	related functions:
		method ` byte[] buildErrorMessage(Header, int, Record)` returns `byte[]`, related with `org.xbill.DNS.TSIG.apply(Message, int, TSIGRecord, boolean)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.
You are tasked with creating a framework for a test class in Java. Now analyze the following information carefully:

1. The method that needs to be tested and its parameters.
2. The constructor of the target class and its parameters.
3. Any dependencies or external resources used by the target class.

Based on this analysis, complete the test class template in the provided structure:

1. Initialize necessary fields and dependencies within setupBeforeAll() and setupBeforeEach().
2. Handle any required cleanup in teardownAfterEach() and teardownAfterAll().
3. Make sure not to invoke any private methods or fields of the target class and dependent class.
4. Refer to the "given" phase in the existing test class for guidance.
5. Add necessary import statements and ensure there are no syntax errors in the generated code.

@input{focal method and target class}
Here is the code of focal method, `apply(Message, int, TSIGRecord, boolean)`, with the focal class `TSIG_apply_4_Test`:
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


@input{context information}

api document of class org.xbill.DNS.TSIG: 

 * Transaction signature handling. This class generates and verifies TSIG records on messages, which
 * provide transaction security.
 *
 * @see TSIGRecord
 * @author Brian Wellington
 

constructors for class `org.xbill.DNS.TSIG`: 
params: org.xbill.DNS.Name algorithm 
org.xbill.DNS.Name name 
body:
```java
  public TSIG(Name algorithm, Name name, String key) {
    this(algorithm, name, Objects.requireNonNull(base64.fromString(key)));
  }

```
params: org.xbill.DNS.Name algorithm 
org.xbill.DNS.Name name 
body:
```java
  public TSIG(Name algorithm, Name name, byte[] keyBytes) {
    this(algorithm, name, new SecretKeySpec(keyBytes, nameToAlgorithm(algorithm)));
  }

```
params: org.xbill.DNS.Name algorithm 
org.xbill.DNS.Name name 
body:
```java
  public TSIG(Name algorithm, Name name, SecretKey key) {
    this(algorithm, name, key, Clock.systemUTC());
  }

```
params: org.xbill.DNS.Name algorithm 
org.xbill.DNS.Name name 
body:
```java
  public TSIG(Name algorithm, Name name, SecretKey key, Clock clock) {
    this.name = name;
    this.alg = algorithm;
    this.clock = clock;
    this.macAlgorithm = nameToAlgorithm(algorithm);
    this.macKey = key;
    this.sharedHmac = null;
  }

```
params: org.xbill.DNS.Name algorithm 
body:
```java
  public TSIG(Name algorithm, String name, String key) {
    byte[] keyBytes = base64.fromString(key);
    if (keyBytes == null) {
      throw new IllegalArgumentException("Invalid TSIG key string");
    }
    try {
      this.name = Name.fromString(name, Name.root);
    } catch (TextParseException e) {
      throw new IllegalArgumentException("Invalid TSIG key name");
    }
    this.alg = algorithm;
    this.clock = Clock.systemUTC();
    this.macAlgorithm = nameToAlgorithm(algorithm);
    this.sharedHmac = null;
    this.macKey = new SecretKeySpec(keyBytes, macAlgorithm);
  }

```
params: 
body:
```java
  public TSIG(String algorithm, String name, String key) {
    this(algorithmToName(algorithm), name, key);
  }

```

parameters in constructors and focal method: 
class `org.xbill.DNS.Name`:
api document : 
 * A representation of a domain name. It may either be absolute (fully qualified) or relative.
 *
 * @author Brian Wellington
 
constructor:
```java
  private Name() {}

  public Name(String s, Name origin) throws TextParseException {
    switch (s) {
      case "":
        throw new TextParseException("empty name");
      case "@":
        if (origin == null) {
          copy(empty, this);
        } else {
          copy(origin, this);
        }
        return;
      case ".": // full stop
        copy(root, this);
        return;
    }
    int labelstart = -1;
    int pos = 0;
    char[] label = new char[MAXLABEL];
    boolean escaped = false;
    int digits = 0;
    int intval = 0;
    boolean absolute = false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c > 0xff) {
        throw new TextParseException(s, "Illegal character in name");
      }
      if (escaped) {
        if (c >= '0' && c <= '9' && digits < 3) {
          digits++;
          intval *= 10;
          intval += c - '0';
          if (intval > 255) {
            throw new TextParseException(s, "bad escape");
          }
          if (digits < 3) {
            continue;
          }
          c = (char) intval;
        } else if (digits > 0 && digits < 3) {
          throw new TextParseException(s, "bad escape");
        }
        if (pos >= MAXLABEL) {
          throw new TextParseException(s, "label too long");
        }
        labelstart = pos;
        label[pos++] = c;
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
        digits = 0;
        intval = 0;
      } else if (c == '.') {
        if (labelstart == -1) {
          throw new TextParseException(s, "invalid empty label");
        }
        appendFromString(s, label, pos);
        labelstart = -1;
        pos = 0;
      } else {
        if (labelstart == -1) {
          labelstart = i;
        }
        if (pos >= MAXLABEL) {
          throw new TextParseException(s, "label too long");
        }
        label[pos++] = c;
      }
    }
    if (digits > 0 && digits < 3 || escaped) {
      throw new TextParseException(s, "bad escape");
    }
    if (labelstart == -1) {
      appendFromString(s, emptyLabel, 1);
      absolute = true;
    } else {
      appendFromString(s, label, pos);
    }
    if (origin != null && !absolute) {
      absolute = origin.isAbsolute();
      appendFromString(s, origin.name, origin.labels);
    }
    // A relative name that is MAXNAME octets long is a strange and wonderful thing.
    // Not technically in violation, but it can not be used for queries as it needs
    // to be made absolute by appending at the very least the empty label at the
    // end, which there is no room for. To make life easier for everyone, let's only
    // allow Names that are MAXNAME long if they are absolute.
    if (!absolute && length() == MAXNAME) {
      throw new TextParseException(s, "Name too long");
    }
  }

  public Name(String s) throws TextParseException {
    this(s, null);
  }

  public Name(DNSInput in) throws WireParseException {
    int len;
    int pos;
    boolean done = false;
    byte[] label = new byte[MAXLABEL + 1];
    boolean savedState = false;

    while (!done) {
      len = in.readU8();
      switch (len & LABEL_MASK) {
        case LABEL_NORMAL:
          if (len == 0) {
            append(emptyLabel, 0, 1);
            done = true;
          } else {
            label[0] = (byte) len;
            in.readByteArray(label, 1, len);
            append(label, 0, 1);
          }
          break;
        case LABEL_COMPRESSION:
          pos = in.readU8();
          pos += (len & ~LABEL_MASK) << 8;
          log.trace("currently {}, pointer to {}", in.current(), pos);

          if (pos >= in.current() - 2) {
            throw new WireParseException("bad compression");
          }
          if (!savedState) {
            in.save();
            savedState = true;
          }
          in.jump(pos);
          log.trace("current name '{}', seeking to {}", this, pos);
          break;
        default:
          throw new WireParseException("bad label type");
      }
    }
    if (savedState) {
      in.restore();
    }
  }

  public Name(byte[] b) throws IOException {
    this(new DNSInput(b));
  }

  public Name(Name src, int n) {
    if (n > src.labels) {
      throw new IllegalArgumentException("attempted to remove too many labels");
    }
    if (n == src.labels) {
      copy(empty, this);
      return;
    }

    labels = src.labels - n;
    name = Arrays.copyOfRange(src.name, src.offset(n), src.name.length);
    int strippedBytes = src.offset(n);
    for (int i = 1; i < MAXOFFSETS && i < labels; i++) {
      setOffset(i, src.offset(i + n) - strippedBytes);
    }
  }

```
class `org.xbill.DNS.Message`:
api document : 
 * A DNS Message. A message is the basic unit of communication between the client and server of a
 * DNS operation. A message consists of a Header and 4 message sections.
 *
 * @see Resolver
 * @see Header
 * @see Section
 * @author Brian Wellington
 
constructor:
```java
  @SuppressWarnings("unchecked")
  private Message(Header header) {
    sections = new List[4];
    this.header = header;
  }

  public Message(int id) {
    this(new Header(id));
  }

  public Message() {
    this(new Header());
  }

  Message(DNSInput in) throws IOException {
    this(new Header(in));
    boolean isUpdate = header.getOpcode() == Opcode.UPDATE;
    boolean truncated = header.getFlag(Flags.TC);
    try {
      for (int i = 0; i < 4; i++) {
        int count = header.getCount(i);
        if (count > 0) {
          sections[i] = new ArrayList<>(count);
        }
        for (int j = 0; j < count; j++) {
          int pos = in.current();
          Record rec = Record.fromWire(in, i, isUpdate);
          sections[i].add(rec);
          if (i == Section.ADDITIONAL) {
            if (rec.getType() == Type.TSIG) {
              tsigstart = pos;
              if (j != count - 1) {
                throw new WireParseException("TSIG is not the last record in the message");
              }
            }
            if (rec.getType() == Type.SIG) {
              SIGRecord sig = (SIGRecord) rec;
              if (sig.getTypeCovered() == 0) {
                sig0start = pos;
              }
            }
          }
        }
      }
    } catch (WireParseException e) {
      if (!truncated) {
        throw e;
      }
    }
    size = in.current();
  }

  public Message(byte[] b) throws IOException {
    this(new DNSInput(b));
  }

  public Message(ByteBuffer byteBuffer) throws IOException {
    this(new DNSInput(byteBuffer));
  }

```
class `org.xbill.DNS.TSIGRecord`:
api document : 
 * Transaction Signature - this record is automatically generated by the resolver. TSIG records
 * provide transaction security between the sender and receiver of a message, using a shared key.
 *
 * @see Resolver
 * @see TSIG
 * @see <a href="https://tools.ietf.org/html/rfc2845">RFC 2845: Secret Key Transaction
 *     Authentication for DNS (TSIG)</a>
 * @author Brian Wellington
 
constructor:
```java
  TSIGRecord() {}

  public TSIGRecord(
      Name name,
      int dclass,
      long ttl,
      Name alg,
      Instant timeSigned,
      Duration fudge,
      byte[] signature,
      int originalID,
      int error,
      byte[] other) {
    super(name, Type.TSIG, dclass, ttl);
    this.alg = checkName("alg", alg);
    this.timeSigned = timeSigned;
    checkU16("fudge", (int) fudge.getSeconds());
    this.fudge = fudge;
    this.signature = signature;
    this.originalID = checkU16("originalID", originalID);
    this.error = checkU16("error", error);
    this.other = other;
  }

```

existing test class: 
```java
// SPDX-License-Identifier: BSD-3-Clause
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.crypto.spec.SecretKeySpec;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.xbill.DNS.TSIG.StreamGenerator;
import org.xbill.DNS.utils.base64;

class TSIGTest {
  private final TSIG defaultKey = new TSIG(TSIG.HMAC_SHA256, "example.", "12345678");

  @Test
  void signedQuery() throws IOException {
    Record question = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
    Message query = Message.newQuery(question);
    query.setTSIG(defaultKey);
    byte[] qbytes = query.toWire(512);
    assertEquals(1, qbytes[11]);

    Message qparsed = new Message(qbytes);
    int result = defaultKey.verify(qparsed, qbytes, null);
    assertEquals(Rcode.NOERROR, result);
    assertTrue(qparsed.isSigned());
    assertTrue(qparsed.isVerified());
  }

  /**
   * Check all of the string algorithm names defined in the javadoc. Confirm that java names also
   * allowed, even though undocumented. THis is to conserve backwards compatibility.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "hmac-md5",
        "hmac-md5.sig-alg.reg.int.",
        "hmac-sha1",
        "hmac-sha224",
        "hmac-sha256",
        "hmac-sha256.",
        "hmac-sha384",
        "hmac-sha512",
        // Java names
        "HmacMD5",
        "HmacSHA256"
      })
  void queryStringAlg(String alg) throws IOException {
    TSIG key = new TSIG(alg, "example.", "12345678");

    Record rec = Record.newRecord(Name.fromString("www.example."), Type.A, DClass.IN);
    Message msg = Message.newQuery(rec);
    msg.setTSIG(key);
    byte[] bytes = msg.toWire(512);
    assertEquals(1, bytes[11]);

    Message parsed = new Message(bytes);
    int result = key.verify(parsed, bytes, null);
    assertEquals(Rcode.NOERROR, result);
    assertTrue(parsed.isSigned());
    assertTrue(parsed.isVerified());
  }

  /** Confirm error thrown with illegal algorithm name. */
  @Test
  void queryStringAlgError() {
    assertThrows(
......
              3600,
              1800);
      for (int i = 0; i < responseMessageCount; i++) {
        Message response = new Message(parsedQueryMessage.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.addRecord(parsedQueryMessage.getQuestion(), Section.QUESTION);
        if (i == 0) {
          response.addRecord(soa, Section.ANSWER);
        }
        Record answer =
            new ARecord(
                parsedQueryMessage.getQuestion().getName(),
                DClass.IN,
                300,
                InetAddress.getByAddress(ByteBuffer.allocate(4).putInt(i).array()));
        response.addRecord(answer, Section.ANSWER);

        if (i == responseMessageCount - 1) {
          response.addRecord(soa, Section.ANSWER);
        }

        generator.generate(response, !skipLast && i == responseMessageCount - 1);
        messages.add(response);
      }
    }

    @Override
    byte[] recv() {
      return messages.get(recvCalls++).toWire(Message.MAXLENGTH);
    }

    private StreamGenerator getStreamGenerator(int signEvery, Message parsedQueryMessage)
        throws NoSuchFieldException, IllegalAccessException {
      TSIGRecord queryMessageTSIG = parsedQueryMessage.getTSIG();
      StreamGenerator generator;

      // Hack for testing invalid server responses, the constructor would normally prevent such an
      // invalid argument
      if (signEvery > 100) {
        generator = new StreamGenerator(key, queryMessageTSIG, 1);
        Field signEveryNthMessage = StreamGenerator.class.getDeclaredField("signEveryNthMessage");
        signEveryNthMessage.setAccessible(true);
        signEveryNthMessage.set(generator, signEvery);
      } else {
        generator = new StreamGenerator(key, queryMessageTSIG, signEvery);
      }
      return generator;
    }
  }
}
```

invoke examples of focal method: 
example 1:
```java
  public void apply(Message m, TSIGRecord old) {
    apply(m, Rcode.NOERROR, old, true);
  }
```
example 2:
```java
  public void apply(Message m, int error, TSIGRecord old) {
    apply(m, error, old, true);
  }
```
example 3:
```java
  public void apply(Message m, TSIGRecord old, boolean fullSignature) {
    apply(m, Rcode.NOERROR, old, fullSignature);
  }
```



@input{test class template}
```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
// Add other imports if necessary

@ExtendWith(MockitoExtension.class)
class TSIG_apply_4_Test {
    @BeforeAll
    static void setupBeforeAll() {
    }

    @BeforeEach
    void setupBeforeEach() {
    }

    @AfterEach
    void teardownAfterEach() {
    }

    @AfterAll
    static void teardownAfterAll() {
    }
}
```

@output{initial test class}: now consider the invoke examples output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.
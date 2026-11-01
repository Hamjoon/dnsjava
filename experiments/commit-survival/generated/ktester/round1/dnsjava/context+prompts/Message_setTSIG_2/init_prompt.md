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
Here is the code of focal method, `setTSIG(TSIG, int, TSIGRecord)`, with the focal class `Message_setTSIG_2_Test`:
```Java
package org.xbill.DNS;
import java.util.LinkedList;
import java.util.Set;
import java.util.Optional;
import lombok.SneakyThrows;
import java.util.ArrayList;
import java.io.IOException;
import java.util.List;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Collections;
public class Message implements Cloneable {
    public static final int MAXLENGTH = 65535;
    private TSIG tsigkey;
    private TSIGRecord querytsig;
    private int tsigerror;
    int tsigstart;
    int tsigState;
    int sig0start;
    static final int TSIG_UNSIGNED = 0;
    static final int TSIG_VERIFIED = 1;
    static final int TSIG_INTERMEDIATE = 2;
    static final int TSIG_SIGNED = 3;
    static final int TSIG_FAILED = 4;
    public void setTSIG(TSIG key, int error, TSIGRecord querytsig) {
        this.tsigkey = key;
        this.tsigerror = error;
        this.querytsig = querytsig;
    }
public void addRecord(Record r, int section);
    @Override
@SneakyThrows(CloneNotSupportedException.class)
@SuppressWarnings("unchecked")
public Message clone();
    public boolean findRRset(Name name, int type);
    public boolean findRRset(Name name, int type, int section);
    public boolean findRecord(Record r);
    public boolean findRecord(Record r, int section);
    TSIGRecord getGeneratedTSIG();
    public Header getHeader();
    public OPTRecord getOPT();
    public Record getQuestion();
    public int getRcode();
    public Optional<Resolver> getResolver();
    public List<Record> getSection(int section);
    public List<RRset> getSectionRRsets(int section);
    public TSIGRecord getTSIG();
    public boolean isSigned();
    public boolean isVerified();
    public static Message newQuery(Record r);
    public static Message newUpdate(Name zone);
    public int numBytes();
    public void removeAllRecords(int section);
    public boolean removeRecord(Record r, int section);
    public String sectionToString(int i);
    public void setHeader(Header h);
    public void setResolver(Resolver resolver);
    public void setTSIG(TSIG key);
    @Override
public String toString();
    public byte[] toWire();
    public byte[] toWire(int maxLength);
    void toWire(DNSOutput out);
}
```


@input{context information}

api document of class org.xbill.DNS.Message: 

 * A DNS Message. A message is the basic unit of communication between the client and server of a
 * DNS operation. A message consists of a Header and 4 message sections.
 *
 * @see Resolver
 * @see Header
 * @see Section
 * @author Brian Wellington
 

constructors for class `org.xbill.DNS.Message`: 
params: org.xbill.DNS.Header header 
body:
```java
  @SuppressWarnings("unchecked")
  private Message(Header header) {
    sections = new List[4];
    this.header = header;
  }

```
params: 
body:
```java
  public Message(int id) {
    this(new Header(id));
  }

```
params: 
body:
```java
  public Message() {
    this(new Header());
  }

```
params: org.xbill.DNS.DNSInput in 
body:
```java
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

```
params: 
body:
```java
  public Message(byte[] b) throws IOException {
    this(new DNSInput(b));
  }

```
params: 
body:
```java
  public Message(ByteBuffer byteBuffer) throws IOException {
    this(new DNSInput(byteBuffer));
  }

```

parameters in constructors and focal method: 
class `org.xbill.DNS.Header`:
api document : 
 * A DNS message header
 *
 * @see Message
 * @author Brian Wellington
 
constructor:
```java
  public Header(int id) {
    if (id < 0 || id > 0xffff) {
      throw new IllegalArgumentException("DNS message ID " + id + " is out of range");
    }
    counts = new int[4];
    flags = 0;
    this.id = id;
  }

  public Header() {
    this(random.nextInt(0xffff));
  }

  Header(DNSInput in) throws IOException {
    this(in.readU16());
    flags = in.readU16();
    for (int i = 0; i < counts.length; i++) {
      counts[i] = in.readU16();
    }
  }

  public Header(byte[] b) throws IOException {
    this(new DNSInput(b));
  }

```
class `org.xbill.DNS.DNSInput`:
api document : 
 * A class for parsing DNS messages.
 *
 * @author Brian Wellington
 
constructor:
```java
  public DNSInput(byte[] input) {
    this(ByteBuffer.wrap(input));
  }

  public DNSInput(ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
    offset = byteBuffer.position();
    limit = byteBuffer.limit();
    savedPos = -1;
    savedEnd = -1;
  }

```
class `org.xbill.DNS.TSIG`:
api document : 
 * Transaction signature handling. This class generates and verifies TSIG records on messages, which
 * provide transaction security.
 *
 * @see TSIGRecord
 * @author Brian Wellington
 
constructor:
```java
  public TSIG(Name algorithm, Name name, String key) {
    this(algorithm, name, Objects.requireNonNull(base64.fromString(key)));
  }

  public TSIG(Name algorithm, Name name, byte[] keyBytes) {
    this(algorithm, name, new SecretKeySpec(keyBytes, nameToAlgorithm(algorithm)));
  }

  public TSIG(Name algorithm, Name name, SecretKey key) {
    this(algorithm, name, key, Clock.systemUTC());
  }

  public TSIG(Name algorithm, Name name, SecretKey key, Clock clock) {
    this.name = name;
    this.alg = algorithm;
    this.clock = clock;
    this.macAlgorithm = nameToAlgorithm(algorithm);
    this.macKey = key;
    this.sharedHmac = null;
  }

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

  public TSIG(String algorithm, String name, String key) {
    this(algorithmToName(algorithm), name, key);
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
// -*- Java -*-
//
// Copyright (c) 2005, Matthew J. Rutherford <rutherfo@cs.colorado.edu>
// Copyright (c) 2005, University of Colorado at Boulder
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
// * Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
//
// * Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
//
// * Neither the name of the University of Colorado at Boulder nor the
//   names of its contributors may be used to endorse or promote
//   products derived from this software without specific prior written
//   permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
package org.xbill.DNS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.utils.base64;

class MessageTest {
  @Test
  void ctor_0arg() {
    Message m = new Message();
    assertTrue(m.getSection(0).isEmpty());
    assertTrue(m.getSection(1).isEmpty());
    assertTrue(m.getSection(3).isEmpty());
    assertTrue(m.getSection(2).isEmpty());
    assertThrows(IndexOutOfBoundsException.class, () -> m.getSection(4));
    Header h = m.getHeader();
    assertEquals(0, h.getCount(0));
    assertEquals(0, h.getCount(1));
    assertEquals(0, h.getCount(2));
    assertEquals(0, h.getCount(3));
  }

  @Test
  void ctor_1arg() {
    Message m = new Message(10);
    assertEquals(new Header(10).toString(), m.getHeader().toString());
    assertTrue(m.getSection(0).isEmpty());
    assertTrue(m.getSection(1).isEmpty());
    assertTrue(m.getSection(2).isEmpty());
    assertTrue(m.getSection(3).isEmpty());
    assertThrows(IndexOutOfBoundsException.class, () -> m.getSection(4));
    Header h = m.getHeader();
    assertEquals(0, h.getCount(0));
    assertEquals(0, h.getCount(1));
    assertEquals(0, h.getCount(2));
    assertEquals(0, h.getCount(3));
  }

  @Test
  void ctor_byteBuffer() throws IOException {
    byte[] arr =
        base64.fromString(
            "EEuBgAABAAEABAAIA3d3dwZnb29nbGUDY29tAAABAAHADAABAAEAAAAaAASO+rokwBAAAgABAAFHCwAGA25zMcAQwBAAAgABAAFHCwAGA25zNMAQwBAAAgABAAFHCwAGA25zM8AQwBAAAgABAAFHCwAGA25zMsAQwDwAAQABAADObwAE2O8gCsByAAEAAQABrVEABNjvIgrAYAABAAEAAVqZAATY7yQKwE4AAQABAAK9RQAE2O8mCsA8ABwAAQAD4a0AECABSGBIAgAyAAAAAAAAAArAcgAcAAEAAtDgABAgAUhgSAIANAAAAAAAAAAKwGAAHAABAACSagAQIAFIYEgCADYAAAAAAAAACsBOABwAAQAErVoAECABSGBIAgA4AAAAAAAAAAo=");

    ByteBuffer wrap = ByteBuffer.allocate(arr.length + 2);

    // prepend length, like when reading a response from a TCP channel
    wrap.putShort((short) arr.length);
    wrap.put(arr);
    wrap.flip();
    wrap.getShort(); // read the prepended length

    Message m = new Message(wrap);
    assertEquals(Name.fromConstantString("www.google.com."), m.getQuestion().getName());
  }

  @Test
......
  @Test
  void sectionToWire() throws IOException {
    Message m = new Message(4711);
    Name n2 = Name.fromConstantString("test2.example.");
    m.addRecord(new TXTRecord(n2, DClass.IN, 86400, "other record"), Section.ADDITIONAL);
    Name n = Name.fromConstantString("test.example.");
    m.addRecord(new TXTRecord(n, DClass.IN, 86400, "example text -1-"), Section.ADDITIONAL);
    m.addRecord(new TXTRecord(n, DClass.IN, 86400, "example text -2-"), Section.ADDITIONAL);
    m.addRecord(new TXTRecord(n, DClass.IN, 86400, "example text -3-"), Section.ADDITIONAL);
    m.addRecord(new TXTRecord(n, DClass.IN, 86400, "example text -4-"), Section.ADDITIONAL);
    m.addRecord(new OPTRecord(512, 0, 0, 0), Section.ADDITIONAL);

    for (int i = 5; i < 50; i++) {
      m.addRecord(
          new TXTRecord(n, DClass.IN, 86400, "example text -" + i + "-"), Section.ADDITIONAL);
    }

    byte[] binary = m.toWire(512);
    Message m2 = new Message(binary);
    assertEquals(2, m2.getHeader().getCount(Section.ADDITIONAL));
    List<Record> records = m2.getSection(Section.ADDITIONAL);
    assertEquals(2, records.size());
    assertEquals(TXTRecord.class, records.get(0).getClass());
    assertEquals(OPTRecord.class, records.get(1).getClass());
  }

  @Test
  void testQuestionClone() {
    Name qname = Name.fromConstantString("www.example.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message query = Message.newQuery(question);
    Message clone = query.clone();
    assertEquals(query.getHeader().getID(), clone.getHeader().getID());
    assertEquals(query.getQuestion().getName(), clone.getQuestion().getName());
  }

  @Test
  void testResponseClone() throws UnknownHostException {
    Name qname = Name.fromConstantString("www.example.");
    Record question = Record.newRecord(qname, Type.A, DClass.IN);
    Message response = new Message();
    response.getHeader().setFlag(Flags.QR);
    response.addRecord(question, Section.QUESTION);
    response.addRecord(
        new ARecord(qname, DClass.IN, 0, InetAddress.getByName("127.0.0.1")), Section.ANSWER);
    Message clone = response.clone();
    assertEquals(clone.getQuestion(), response.getQuestion());
    assertEquals(clone.getSection(Section.ANSWER), response.getSection(Section.ANSWER));
  }
}
```

invoke examples of focal method: 
example 1:
```java
  public void setTSIG(TSIG key) {
    setTSIG(key, Rcode.NOERROR, null);
  }
```
example 2:
```java
  @Override
  public CompletionStage<Message> sendAsync(Message query, Executor executor) {
    if (query.getHeader().getOpcode() == Opcode.QUERY) {
      Record question = query.getQuestion();
      if (question != null && question.getType() == Type.AXFR) {
        CompletableFuture<Message> f = new CompletableFuture<>();
        CompletableFuture.runAsync(
            () -> {
              try {
                f.complete(sendAXFR(query));
              } catch (IOException e) {
                f.completeExceptionally(e);
              }
            },
            executor);

        return f;
      }
    }

    Message ednsTsigQuery = query.clone();
    applyEDNS(ednsTsigQuery);
    if (tsig != null) {
      ednsTsigQuery.setTSIG(tsig, Rcode.NOERROR, null);
  }
```
example 3:
```java
  byte[] generateReply(Message query, byte[] in, Socket s) {
    Header header;
    int maxLength;
    int flags = 0;

    header = query.getHeader();
    if (header.getFlag(Flags.QR)) {
      return null;
    }
    if (header.getRcode() != Rcode.NOERROR) {
      return errorMessage(query, Rcode.FORMERR);
    }
    if (header.getOpcode() != Opcode.QUERY) {
      return errorMessage(query, Rcode.NOTIMP);
    }

    Record queryRecord = query.getQuestion();

    TSIGRecord queryTSIG = query.getTSIG();
    TSIG tsig = null;
    if (queryTSIG != null) {
      tsig = TSIGs.get(queryTSIG.getName());
      if (tsig == null || tsig.verify(query, in, null) != Rcode.NOERROR) {
        return formerrMessage(in);
      }
    }

    OPTRecord queryOPT = query.getOPT();
    if (s != null) {
      maxLength = 65535;
    } else if (queryOPT != null) {
      maxLength = Math.max(queryOPT.getPayloadSize(), 512);
    } else {
      maxLength = 512;
    }

    if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlags.DO) != 0) {
      flags = FLAG_DNSSECOK;
    }

    Message response = new Message(query.getHeader().getID());
    response.getHeader().setFlag(Flags.QR);
    if (query.getHeader().getFlag(Flags.RD)) {
      response.getHeader().setFlag(Flags.RD);
    }
    response.addRecord(queryRecord, Section.QUESTION);

    Name name = queryRecord.getName();
    int type = queryRecord.getType();
    int dclass = queryRecord.getDClass();
    if (type == Type.AXFR && s != null) {
      return doAXFR(name, query, tsig, queryTSIG, s);
    }
    if (!Type.isRR(type) && type != Type.ANY) {
      return errorMessage(query, Rcode.NOTIMP);
    }

    byte rcode = addAnswer(response, name, type, dclass, 0, flags);
    if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN) {
      return errorMessage(query, rcode);
    }

    addAdditional(response, flags);

    if (queryOPT != null) {
      int optflags = (flags == FLAG_DNSSECOK) ? ExtendedFlags.DO : 0;
      OPTRecord opt = new OPTRecord((short) 4096, rcode, (byte) 0, optflags);
      response.addRecord(opt, Section.ADDITIONAL);
    }

    response.setTSIG(tsig, Rcode.NOERROR, queryTSIG);
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
class Message_setTSIG_2_Test {
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
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
Here is the code of focal method, `newQuery(Record)`, with the focal class `Message_newQuery_Test`:
```Java
package org.xbill.DNS;
import java.util.LinkedList;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;
import lombok.SneakyThrows;
import java.util.ArrayList;
import java.io.IOException;
import java.util.List;
import java.nio.ByteBuffer;
import java.util.Collections;
@Slf4j
public class Message implements Cloneable {
    public static final int MAXLENGTH = 65535;
    private Header header;
    int tsigstart;
    int tsigState;
    int sig0start;
    static final int TSIG_UNSIGNED = 0;
    static final int TSIG_VERIFIED = 1;
    static final int TSIG_INTERMEDIATE = 2;
    static final int TSIG_SIGNED = 3;
    static final int TSIG_FAILED = 4;
    public static Message newQuery(Record r) {
        Message m = new Message();
        m.header.setOpcode(Opcode.QUERY);
        m.header.setFlag(Flags.RD);
        m.addRecord(r, Section.QUESTION);
        return m;
    }
public void addRecord(Record r, int section);
    @Override
@SneakyThrows(CloneNotSupportedException.class)
@SuppressWarnings(;
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
    @SuppressWarnings("java:S1119")
public List<RRset> getSectionRRsets(int section);
    public TSIGRecord getTSIG();
    public boolean isSigned();
    boolean isTypeAllowedInSection(int type, int section);
    public boolean isVerified();
    public static Message newUpdate(Name zone);
    public Message normalize(Message query);
    public Message normalize(Message query, boolean throwOnIrrelevantRecord) throws WireParseException;
    public int numBytes();
    public void removeAllRecords(int section);
    public boolean removeRecord(Record r, int section);
    public String sectionToString(int section);
    public void setHeader(Header h);
    public void setResolver(Resolver resolver);
    public void setTSIG(TSIG key);
    public void setTSIG(TSIG key, int error, TSIGRecord querytsig);
    @Override
public String toString();
    public byte[] toWire();
    public byte[] toWire(int maxLength);
    public byte[] toWire(int maxLength, boolean truncate) throws MessageSizeExceededException;
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
  @SuppressWarnings({"unchecked"})
  private Message(Header header) {
    sections = (List<Record>[]) new List<?>[4];
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
    if (!Utils.isUInt16(id)) {
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
class `org.xbill.DNS.Record`:
api document : 
 * A generic DNS resource record. The specific record types extend this class. A record contains a
 * name, type, class, ttl, and rdata.
 *
 * @author Brian Wellington
 
constructor:
```java
  protected Record() {}

  protected Record(Name name, int type, int dclass, long ttl) {
    if (!name.isAbsolute()) {
      throw new RelativeNameException(name);
    }
    Type.check(type);
    DClass.check(dclass);
    TTL.check(ttl);
    this.name = name;
    this.type = type;
    this.dclass = dclass;
    this.ttl = ttl;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    assertTrue(m.getSection(2).isEmpty());
    assertTrue(m.getSection(3).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> m.getSection(4));
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
    assertThrows(IllegalArgumentException.class, () -> m.getSection(4));
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

......
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

  @Test
  void normalize() throws WireParseException {
    Record queryRecord =
        Record.newRecord(Name.fromConstantString("example.com."), Type.MX, DClass.IN);
    Message query = Message.newQuery(queryRecord);
    Message response = new Message();
    response.addRecord(queryRecord, Section.QUESTION);
    response.addRecord(queryRecord, Section.ADDITIONAL);
    response = response.normalize(query, true);
    assertThat(response.getSection(Section.ANSWER)).isEmpty();
    assertThat(response.getHeader().getCount(Section.ANSWER)).isZero();
    assertThat(response.getSection(Section.ADDITIONAL)).isEmpty();
    assertThat(response.getHeader().getCount(Section.ADDITIONAL)).isZero();
  }
}
```

invoke examples of focal method: 
example 1:
```java
  private CompletionStage<LookupResult> lookupWithResolver(Record queryRecord, List<Name> aliases) {
    Message query = Message.newQuery(queryRecord);
  }
```
example 2:
```java
  private void lookup(Name current) {
    if (lookupFromHostsFile(current)) {
      return;
    }

    SetResponse sr = cache.lookupRecords(current, type, credibility);
    log.debug("Lookup for {}/{}, cache answer: {}", current, Type.string(type), sr);

    processResponse(current, sr);
    if (done || doneCurrent) {
      return;
    }

    Record question = Record.newRecord(current, type, dclass);
    Message query = Message.newQuery(question);
  }
```
example 3:
```java
  void doQuery(Tokenizer st) throws IOException {
    Record rec;
    Tokenizer.Token token;

    Name name;
    int type = Type.A;
    int dclass = defaultClass;

    name = st.getName(zone);
    token = st.get();
    if (token.isString()) {
      type = Type.value(token.value());
      if (type < 0) {
        throw new IOException("Invalid type");
      }
      token = st.get();
      if (token.isString()) {
        dclass = DClass.value(token.value());
        if (dclass < 0) {
          throw new IOException("Invalid class");
        }
      }
    }

    rec = Record.newRecord(name, type, dclass);
    Message newQuery = Message.newQuery(rec);
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
class Message_newQuery_Test {
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
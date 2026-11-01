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
Here is the code of focal method, `isTypeAllowedInSection(int, int)`, with the focal class `Message_isTypeAllowedInSection_Test`:
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
    int tsigstart;
    int tsigState;
    int sig0start;
    static final int TSIG_UNSIGNED = 0;
    static final int TSIG_VERIFIED = 1;
    static final int TSIG_INTERMEDIATE = 2;
    static final int TSIG_SIGNED = 3;
    static final int TSIG_FAILED = 4;
    boolean isTypeAllowedInSection(int type, int section) {
        Type.check(type);
        Section.check(section);
        switch (section) {
            case Section.AUTHORITY :
                if (((((type == Type.SOA) || (type == Type.NS)) || (type == Type.DS)) || (type == Type.NSEC)) || (type == Type.NSEC3)) {
                    return true;
                }
                break;
            case Section.ADDITIONAL :
                if ((type == Type.A) || (type == Type.AAAA)) {
                    return true;
                }
                break;
        }
        return !Boolean.parseBoolean(System.getProperty("dnsjava.harden_unknown_additional", "true"));
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
    public boolean isVerified();
    public static Message newQuery(Record r);
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

  @Test
......
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

  @Test
  void normalize() throws WireParseException {
    Record queryRecord =
        Record.newRecord(Name.fromConstantString("example.com."), Type.MX, DClass.IN);
    Message query = Message.newQuery(queryRecord);
    Message response = new Message();
    response.addRecord(queryRecord, Section.QUESTION);
    response.addRecord(queryRecord, Section.ADDITIONAL);
    response = response.normalize(query, true);
    assertTrue(response.getSection(Section.ANSWER).isEmpty());
    assertTrue(response.getSection(Section.ADDITIONAL).isEmpty());
  }
}
```

invoke examples of focal method: 
example 1:
```java
  private void addAdditionalRRset(
      RRset rrset, List<RRset> additionalSectionSets, List<RRset> cleanedAdditionalSection) {
    if (!doesTypeHaveAdditionalRecords(rrset.getType())) {
      return;
    }

    for (Record r : rrset.rrs(false)) {
      for (RRset set : additionalSectionSets) {
        if (set.getName().equals(r.getAdditionalName())
            && isTypeAllowedInSection(set.getType(), Section.ADDITIONAL)) {
  }
```
example 2:
```java
  public Message normalize(Message query, boolean throwOnIrrelevantRecord)
      throws WireParseException {
    if (getRcode() != Rcode.NOERROR && getRcode() != Rcode.NXDOMAIN) {
      return this;
    }

    Name sname = query.getQuestion().getName();
    List<RRset> answerSectionSets = getSectionRRsets(Section.ANSWER);
    List<RRset> additionalSectionSets = getSectionRRsets(Section.ADDITIONAL);
    List<RRset> authoritySectionSets = getSectionRRsets(Section.AUTHORITY);

    List<RRset> cleanedAnswerSection = new ArrayList<>();
    List<RRset> cleanedAuthoritySection = new ArrayList<>();
    List<RRset> cleanedAdditionalSection = new ArrayList<>();
    boolean hadNsInAuthority = false;

    // For the ANSWER section, remove all "irrelevant" records and add synthesized CNAMEs from
    // DNAMEs. This will strip out-of-order CNAMEs as well.
    for (int i = 0; i < answerSectionSets.size(); i++) {
      RRset rrset = answerSectionSets.get(i);
      Name oldSname = sname;

      if (rrset.getType() == Type.DNAME && sname.subdomain(rrset.getName())) {
        if (rrset.size() > 1) {
          String template =
              "Normalization failed in response to <{}/{}/{}> (id {}), found {} entries (instead of just one) in DNAME RRSet <{}/{}>";
          if (throwOnIrrelevantRecord) {
            throw new WireParseException(template.replace("{}", "%s"));
          }
          log.warn(
              template,
              sname,
              Type.string(query.getQuestion().getType()),
              DClass.string(query.getQuestion().getDClass()),
              getHeader().getID(),
              rrset.size(),
              rrset.getName(),
              DClass.string(rrset.getDClass()));
          return null;
        }

        // If DNAME was queried, don't attempt to synthesize CNAME
        if (query.getQuestion().getType() != Type.DNAME) {
          // The DNAME is valid, accept it
          cleanedAnswerSection.add(rrset);

          // Check if the next rrset is correct CNAME, otherwise synthesize a CNAME
          RRset nextRRSet = answerSectionSets.size() >= i + 2 ? answerSectionSets.get(i + 1) : null;
          DNAMERecord dname = ((DNAMERecord) rrset.first());
          try {
            // Validate that an existing CNAME matches what we would synthesize
            if (nextRRSet != null
                && nextRRSet.getType() == Type.CNAME
                && nextRRSet.getName().equals(sname)) {
              Name expected =
                  Name.concatenate(
                      nextRRSet.getName().relativize(dname.getName()), dname.getTarget());
              if (expected.equals(((CNAMERecord) nextRRSet.first()).getTarget())) {
                continue;
              }
            }

            // Add a synthesized CNAME; TTL=0 to avoid caching
            Name dnameTarget = sname.fromDNAME(dname);
            cleanedAnswerSection.add(
                new RRset(new CNAMERecord(sname, dname.getDClass(), 0, dnameTarget)));
            sname = dnameTarget;

            // In DNAME ANY response, can have data after DNAME
            if (query.getQuestion().getType() == Type.ANY) {
              for (i++; i < answerSectionSets.size(); i++) {
                rrset = answerSectionSets.get(i);
                if (rrset.getName().equals(oldSname)) {
                  cleanedAnswerSection.add(rrset);
                } else {
                  break;
                }
              }
            }

            continue;
          } catch (NameTooLongException e) {
            String template =
                "Normalization failed in response to <{}/{}/{}> (id {}), could not synthesize CNAME for DNAME <{}/{}>";
            if (throwOnIrrelevantRecord) {
              throw new WireParseException(template.replace("{}", "%s"), e);
            }
            log.warn(
                template,
                sname,
                Type.string(query.getQuestion().getType()),
                DClass.string(query.getQuestion().getDClass()),
                getHeader().getID(),
                rrset.getName(),
                DClass.string(rrset.getDClass()));
            return null;
          }
        }
      }

      // Ignore irrelevant records
      if (!sname.equals(rrset.getName())) {
        logOrThrow(
            throwOnIrrelevantRecord,
            "Ignoring irrelevant RRset <{}/{}/{}> in response to <{}/{}/{}> (id {})",
            rrset,
            sname,
            query);
        continue;
      }

      // Follow CNAMEs
      if (rrset.getType() == Type.CNAME && query.getQuestion().getType() != Type.CNAME) {
        if (rrset.size() > 1) {
          String template =
              "Found {} CNAMEs in <{}/{}> response to <{}/{}/{}> (id {}), removing all but the first";
          if (throwOnIrrelevantRecord) {
            throw new WireParseException(
                String.format(
                    template.replace("{}", "%s"),
                    rrset.rrs(false).size(),
                    rrset.getName(),
                    DClass.string(rrset.getDClass()),
                    sname,
                    Type.string(query.getQuestion().getType()),
                    DClass.string(query.getQuestion().getDClass()),
                    getHeader().getID()));
          }
          log.warn(
              template,
              rrset.rrs(false).size(),
              rrset.getName(),
              DClass.string(rrset.getDClass()),
              sname,
              Type.string(query.getQuestion().getType()),
              DClass.string(query.getQuestion().getDClass()),
              getHeader().getID());
          List<Record> cnameRRset = rrset.rrs(false);
          for (int cnameIndex = 1; cnameIndex < cnameRRset.size(); cnameIndex++) {
            rrset.deleteRR(cnameRRset.get(i));
          }
        }

        sname = ((CNAMERecord) rrset.first()).getTarget();
        cleanedAnswerSection.add(rrset);

        // In CNAME ANY response, can have data after CNAME
        if (query.getQuestion().getType() == Type.ANY) {
          for (i++; i < answerSectionSets.size(); i++) {
            rrset = answerSectionSets.get(i);
            if (rrset.getName().equals(oldSname)) {
              cleanedAnswerSection.add(rrset);
            } else {
              break;
            }
          }
        }

        continue;
      }

      // Remove records that don't match the queried type
      int qtype = getQuestion().getType();
      if (qtype != Type.ANY && rrset.getActualType() != qtype) {
        logOrThrow(
            throwOnIrrelevantRecord,
            "Ignoring irrelevant RRset <{}/{}/{}> in ANSWER section response to <{}/{}/{}> (id {})",
            rrset,
            sname,
            query);
        continue;
      }

      // Mark the additional names from relevant RRset as OK
      cleanedAnswerSection.add(rrset);
      if (sname.equals(rrset.getName())) {
        addAdditionalRRset(rrset, additionalSectionSets, cleanedAdditionalSection);
      }
    }

    for (RRset rrset : authoritySectionSets) {
      switch (rrset.getType()) {
        case Type.DNAME:
        case Type.CNAME:
        case Type.A:
        case Type.AAAA:
          logOrThrow(
              throwOnIrrelevantRecord,
              "Ignoring forbidden RRset <{}/{}/{}> in AUTHORITY section response to <{}/{}/{}> (id {})",
              rrset,
              sname,
              query);
          continue;
      }

      if (!isTypeAllowedInSection(rrset.getType(), Section.AUTHORITY)) {
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
class Message_isTypeAllowedInSection_Test {
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
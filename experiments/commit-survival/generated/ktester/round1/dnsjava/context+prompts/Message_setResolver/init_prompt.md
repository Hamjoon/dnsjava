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
Here is the code of focal method, `setResolver(Resolver)`, with the focal class `Message_setResolver_Test`:
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
    private Resolver resolver;
    int tsigstart;
    int tsigState;
    int sig0start;
    static final int TSIG_UNSIGNED = 0;
    static final int TSIG_VERIFIED = 1;
    static final int TSIG_INTERMEDIATE = 2;
    static final int TSIG_SIGNED = 3;
    static final int TSIG_FAILED = 4;
    public void setResolver(Resolver resolver) {
        this.resolver = resolver;
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
class `org.xbill.DNS.Resolver`:
api document : 
 * Interface describing a resolver.
 *
 * @author Brian Wellington
 
constructor:
```java
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
  private CompletionStage<Message> sendAsync8(final Message query, Executor executor) {
    byte[] queryBytes = prepareQuery(query).toWire();
    String url = getUrl(queryBytes);
    long startTime = System.nanoTime();
    return maxConcurrentRequests
        .acquire(timeout)
        .handleAsync(
            (permit, ex) -> {
              if (ex != null) {
                return this.<Message>timeoutFailedFuture(query, ex);
              } else {
                try {
                  SendAndGetMessageBytesResponse result =
                      sendAndGetMessageBytes(url, queryBytes, startTime);
                  Message response;
                  if (result.rc == Rcode.NOERROR) {
                    response = new Message(result.responseBytes);
                    verifyTSIG(query, response, result.responseBytes, tsig);
                  } else {
                    response = new Message(0);
                    response.getHeader().setRcode(result.rc);
                  }

                  response.setResolver(this);
  }
```
example 2:
```java
  private CompletionStage<Message> sendAsync11WithConcurrentRequestPermit(
      Message query,
      Executor executor,
      long startTime,
      Object requestBuilder,
      Permit initialRequestPermit,
      boolean isInitialRequest,
      Permit maxConcurrentRequestPermit) {
    // check if the stream lock acquisition took too long
    Duration remainingTimeout = timeout.minus(System.nanoTime() - startTime, ChronoUnit.NANOS);
    if (remainingTimeout.isNegative()) {
      if (isInitialRequest) {
        initialRequestPermit.release();
      }
      maxConcurrentRequestPermit.release();
      return timeoutFailedFuture(query, null);
    }

    // var httpRequest = requestBuilder.timeout(remainingTimeout).build();
    // var bodyHandler = HttpResponse.BodyHandlers.ofByteArray();
    // return getHttpClient(executor).sendAsync(httpRequest, bodyHandler)
    try {
      Object httpClient = getHttpClient(executor);
      requestBuilderTimeoutMethod.invoke(requestBuilder, remainingTimeout);
      Object httpRequest = requestBuilderBuildMethod.invoke(requestBuilder);
      Object bodyHandler = byteArrayBodyPublisherMethod.invoke(null);
      CompletableFuture<Message> f =
          ((CompletableFuture<?>)
                  httpClientSendAsyncMethod.invoke(httpClient, httpRequest, bodyHandler))
              .whenComplete(
                  (result, ex) -> {
                    if (ex == null) {
                      lastRequest.set(startTime);
                    }
                    maxConcurrentRequestPermit.release();
                    if (isInitialRequest) {
                      initialRequestPermit.release();
                    }
                  })
              .handleAsync(
                  (response, ex) -> {
                    if (ex != null) {
                      if (ex.getCause().getClass().getSimpleName().equals("HttpTimeoutException")) {
                        return this.<Message>timeoutFailedFuture(query, ex.getCause());
                      } else {
                        return this.<Message>failedFuture(ex);
                      }
                    } else {
                      try {
                        Message responseMessage;
                        // int rc = response.statusCode();
                        int rc = (int) httpResponseStatusCodeMethod.invoke(response);
                        if (rc >= 200 && rc < 300) {
                          // byte[] responseBytes = response.body();
                          byte[] responseBytes = (byte[]) httpResponseBodyMethod.invoke(response);
                          responseMessage = new Message(responseBytes);
                          verifyTSIG(query, responseMessage, responseBytes, tsig);
                        } else {
                          responseMessage = new Message();
                          responseMessage.getHeader().setRcode(Rcode.SERVFAIL);
                        }

                        responseMessage.setResolver(this);
  }
```
example 3:
```java
  CompletableFuture<Message> sendAsync(Message query, boolean forceTcp, Executor executor) {
    int qid = query.getHeader().getID();
    byte[] out = query.toWire(Message.MAXLENGTH);
    int udpSize = maxUDPSize(query);
    boolean tcp = forceTcp || out.length > udpSize;
    if (log.isTraceEnabled()) {
      log.trace(
          "Sending {}/{}, id={} to {}/{}:{}, query:\n{}",
          query.getQuestion().getName(),
          Type.string(query.getQuestion().getType()),
          qid,
          tcp ? "tcp" : "udp",
          address.getAddress().getHostAddress(),
          address.getPort(),
          query);
    } else if (log.isDebugEnabled()) {
      log.debug(
          "Sending {}/{}, id={} to {}/{}:{}",
          query.getQuestion().getName(),
          Type.string(query.getQuestion().getType()),
          qid,
          tcp ? "tcp" : "udp",
          address.getAddress().getHostAddress(),
          address.getPort());
    }

    CompletableFuture<byte[]> result;
    if (tcp) {
      result = NioTcpClient.sendrecv(localAddress, address, query, out, timeoutValue);
    } else {
      result = NioUdpClient.sendrecv(localAddress, address, query, out, udpSize, timeoutValue);
    }

    return result.thenComposeAsync(
        in -> {
          CompletableFuture<Message> f = new CompletableFuture<>();

          // Check that the response is long enough.
          if (in.length < Header.LENGTH) {
            f.completeExceptionally(new WireParseException("invalid DNS header - too short"));
            return f;
          }

          // Check that the response ID matches the query ID. We want
          // to check this before actually parsing the message, so that
          // if there's a malformed response that's not ours, it
          // doesn't confuse us.
          int id = ((in[0] & 0xFF) << 8) + (in[1] & 0xFF);
          if (id != qid) {
            f.completeExceptionally(
                new WireParseException("invalid message id: expected " + qid + "; got id " + id));
            return f;
          }

          Message response;
          try {
            response = parseMessage(in);
          } catch (WireParseException e) {
            f.completeExceptionally(e);
            return f;
          }

          if (response.getQuestion() == null) {
            f.completeExceptionally(
                new WireParseException("invalid message: question section missing"));
            return f;
          }

          // validate name, class and type (rfc5452#section-9.1)
          if (!query.getQuestion().getName().equals(response.getQuestion().getName())) {
            f.completeExceptionally(
                new WireParseException(
                    "invalid name in message: expected "
                        + query.getQuestion().getName()
                        + "; got "
                        + response.getQuestion().getName()));
            return f;
          }

          if (query.getQuestion().getDClass() != response.getQuestion().getDClass()) {
            f.completeExceptionally(
                new WireParseException(
                    "invalid class in message: expected "
                        + DClass.string(query.getQuestion().getDClass())
                        + "; got "
                        + DClass.string(response.getQuestion().getDClass())));
            return f;
          }

          if (query.getQuestion().getType() != response.getQuestion().getType()) {
            f.completeExceptionally(
                new WireParseException(
                    "invalid type in message: expected "
                        + Type.string(query.getQuestion().getType())
                        + "; got "
                        + Type.string(response.getQuestion().getType())));
            return f;
          }

          verifyTSIG(query, response, in);
          if (!tcp && !ignoreTruncation && response.getHeader().getFlag(Flags.TC)) {
            if (log.isTraceEnabled()) {
              log.trace(
                  "Got truncated response for id {}, retrying via TCP, response:\n{}",
                  qid,
                  response);
            } else {
              log.debug("Got truncated response for id {}, retrying via TCP", qid);
            }
            return sendAsync(query, true, executor);
          }

          response.setResolver(this);
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
class Message_setResolver_Test {
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
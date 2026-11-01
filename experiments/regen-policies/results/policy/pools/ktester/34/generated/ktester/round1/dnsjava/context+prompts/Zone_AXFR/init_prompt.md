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
Here is the code of focal method, `AXFR()`, with the focal class `Zone_AXFR_Test`:
```Java
package org.xbill.DNS;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Getter;
import java.util.ArrayList;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.function.Supplier;
import java.util.Map;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
public class Zone implements Serializable , Iterable<RRset> {
    public static final int PRIMARY = 1;
    public static final int SECONDARY = 2;
    public Iterator<RRset> AXFR() {
        return new ZoneIterator(true);
    }
public void addRRset(RRset rrset);
    public <T extends Record> void addRecord(T r);
    public RRset findExactMatch(Name name, int type);
    public SetResponse findRecords(Name name, int type);
    public int getDClass();
    public RRset getNS();
    @Override
public Iterator<RRset> iterator();
    public void removeRRset(Name name, int type);
    public void removeRecord(Record r);
    public String toMasterFile();
    @Override
public String toString();
}
```


@input{context information}

api document of class org.xbill.DNS.Zone: 

 * A DNS zone. This encapsulates all data related to a zone, and provides convenient lookup methods.
 * A zone always contains a {@link SOARecord} and at least one {@link NSRecord}.
 *
 * @implNote This class uses a {@link java.util.concurrent.locks.ReadWriteLock} to ensure access and
 *     manipulation is safe from multiple threads. Iterators are "weakly consistent" (see the
 *     package info from {@code java.util.concurrent}).
 * @author Brian Wellington
 

constructors for class `org.xbill.DNS.Zone`: 
params: org.xbill.DNS.Name zone 
body:
```java
  public Zone(Name zone, String file) throws IOException {
    if (zone == null) {
      throw new IllegalArgumentException("no zone name specified");
    }

    if (file == null) {
      throw new IllegalArgumentException("no file name specified");
    }

    try (Master m = new Master(file, zone)) {
      Record record;

      origin = zone;
      while ((record = m.nextRecord()) != null) {
        maybeAddRecord(record);
      }
    }
    validate();
  }

```
params: org.xbill.DNS.Name zone 
org.xbill.DNS.Record records 
body:
```java
  public Zone(Name zone, Record... records) throws IOException {
    if (zone == null) {
      throw new IllegalArgumentException("no zone name specified");
    }
    if (records == null) {
      throw new IllegalArgumentException("no records are specified");
    }
    origin = zone;
    for (Record record : records) {
      maybeAddRecord(record);
    }
    validate();
  }

```
params: org.xbill.DNS.ZoneTransferIn xfrin 
body:
```java
  public Zone(ZoneTransferIn xfrin) throws IOException, ZoneTransferException {
    if (xfrin == null) {
      throw new IllegalArgumentException("no xfrin specified");
    }

    fromXFR(xfrin);
  }

```
params: org.xbill.DNS.Name zone 
body:
```java
  public Zone(Name zone, int dclass, String remote) throws IOException, ZoneTransferException {
    if (zone == null) {
      throw new IllegalArgumentException("no zone name specified");
    }
    DClass.check(dclass);

    ZoneTransferIn xfrin = ZoneTransferIn.newAXFR(zone, remote, null);
    xfrin.setDClass(dclass);
    fromXFR(xfrin);
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
class `org.xbill.DNS.ZoneTransferIn`:
api document : 
 * An incoming DNS Zone Transfer. To use this class, first initialize an object, then call the run()
 * method. If run() doesn't throw an exception the result will either be an IXFR-style response, an
 * AXFR-style response, or an indication that the zone is up to date.
 *
 * @author Brian Wellington
 
constructor:
```java
  ZoneTransferIn(
      Name zone, int xfrtype, long serial, boolean fallback, SocketAddress address, TSIG key) {
    this.address = address;
    this.tsig = key;
    if (zone.isAbsolute()) {
      zname = zone;
    } else {
      try {
        zname = Name.concatenate(zone, Name.root);
      } catch (NameTooLongException e) {
        throw new IllegalArgumentException("ZoneTransferIn: name too long");
      }
    }
    qtype = xfrtype;
    dclass = DClass.IN;
    ixfr_serial = serial;
    want_fallback = fallback;
    state = INITIALSOA;
  }

```

existing test class: 
```java
// SPDX-License-Identifier: BSD-3-Clause
package org.xbill.DNS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZoneTest {
  private Name ZONE_NAME;
  private SOARecord SOA1;
  private SOARecord SOA2;
  private NSRecord NS1;
  private NSRecord NS2;
  private ARecord A_UNIQUE;
  private ARecord A_TEST;
  private AAAARecord AAAA_1_TEST;
  private AAAARecord AAAA_2_TEST;
  private ARecord A_WILD;
  private TXTRecord TXT_WILD;
  private Zone ZONE;

  @BeforeEach
  void beforeEach() throws IOException {
    ZONE_NAME = Name.fromConstantString("example.");
    SOA1 =
        new SOARecord(
            ZONE_NAME,
            DClass.IN,
            3600L,
            Name.fromConstantString("nameserver."),
            new Name("hostadmin", ZONE_NAME),
            1,
            21600L,
            7200L,
            2160000L,
            3600L);
    SOA2 =
        new SOARecord(
            ZONE_NAME,
            DClass.IN,
            3600L,
            Name.fromConstantString("nameserver."),
            new Name("zoneadmin", ZONE_NAME),
            1,
            21600L,
            7200L,
            2160000L,
            3600L);
    NS1 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("nameserver1."));
    NS2 = new NSRecord(ZONE_NAME, DClass.IN, 300L, Name.fromConstantString("nameserver2."));

    InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
    InetAddress localhost6a = InetAddress.getByName("::1");
    InetAddress localhost6b = InetAddress.getByName("::2");
    A_UNIQUE =
        new ARecord(
            new Name("unique", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.3"));
    A_TEST = new ARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost4);
    AAAA_1_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6a);
    AAAA_2_TEST = new AAAARecord(new Name("test", ZONE_NAME), DClass.IN, 3600, localhost6b);
    A_WILD =
        new ARecord(
            new Name("*.wild", ZONE_NAME), DClass.IN, 3600, InetAddress.getByName("127.0.0.2"));
    TXT_WILD = new TXTRecord(new Name("*.wild", ZONE_NAME), DClass.IN, 3600, "sometext");

    Record[] zoneRecords =
        new Record[] {
          SOA1, NS1, NS2, A_UNIQUE, A_TEST, AAAA_1_TEST, AAAA_2_TEST, A_WILD, TXT_WILD,
        };
    ZONE = new Zone(ZONE_NAME, zoneRecords);
  }

  @Test
  void exactNameNull() {
    assertThatThrownBy(() -> ZONE.findExactMatch(null, Type.A))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null");
    assertThatThrownBy(() -> ZONE.findExactMatch(null, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null");
    assertThatThrownBy(() -> ZONE.findExactMatch(Name.root, -1))
        .isInstanceOf(InvalidTypeException.class)
        .hasMessageContaining("type");
  }
......
  }

  @Test
  void iteratorRemoveNSFails() {
    Iterator<RRset> iterator = ZONE.iterator();
    assertThat(iterator).isNotNull();
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo(new RRset(ZONE.getSOA()));
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo(ZONE.getNS());
    assertThatThrownBy(iterator::remove).hasMessageContaining("remove all NS");
  }

  @Test
  void iteratorRemoveNoNextFails() {
    assertThatThrownBy(ZONE.iterator()::remove)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Not at");
  }

  @Test
  void iteratorRemoveASucceeds() {
    Iterator<RRset> it = ZONE.iterator();
    it.next(); // SOA
    it.next(); // NS
    it.next(); // A
    it.remove();
    assertThat(ZONE.findExactMatch(A_TEST.getName(), Type.A)).isNull();
  }

  @Test
  void iteratorNextAfterLastFails() {
    Iterator<RRset> iterator = ZONE.iterator();
    while (iterator.hasNext()) {
      iterator.next();
    }

    assertThatThrownBy(iterator::next)
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("No more elements");
  }

  private static List<RRset> listOf(RRset... rrsets) {
    return Stream.of(rrsets).collect(Collectors.toList());
  }

  private static List<RRset> oneRRset(Record... r) {
    return Collections.singletonList(new RRset(r));
  }
}
```

invoke examples of focal method: 
example 1:
```java
  byte[] doAXFR(Name name, Message query, TSIG tsig, TSIGRecord qtsig, Socket s) {
    Zone zone = znames.get(name);
    boolean first = true;
    if (zone == null) {
      return errorMessage(query, Rcode.REFUSED);
    }
    try {
      DataOutputStream dataOut;
      dataOut = new DataOutputStream(s.getOutputStream());
      int id = query.getHeader().getID();
      Iterator<RRset> it = zone.AXFR();
  }
```
example 2:
```java
  public static void main(String[] args) throws Exception {
    boolean time = false;
    boolean axfr = false;
    boolean iterator = false;
    int arg = 0;

    if (args.length < 2) {
      usage();
    }

    while (args.length - arg > 2) {
      switch (args[0]) {
        case "-t":
          time = true;
          break;
        case "-a":
          axfr = true;
          break;
        case "-i":
          iterator = true;
          break;
      }
      arg++;
    }

    Name origin = Name.fromString(args[arg++], Name.root);
    String file = args[arg++];

    long start = System.currentTimeMillis();
    Zone zone = new Zone(origin, file);
    long end = System.currentTimeMillis();
    if (axfr) {
      Iterator<RRset> it = zone.AXFR();
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
class Zone_AXFR_Test {
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
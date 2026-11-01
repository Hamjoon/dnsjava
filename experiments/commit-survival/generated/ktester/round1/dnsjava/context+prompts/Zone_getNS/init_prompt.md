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
Here is the code of focal method, `getNS()`, with the focal class `Zone_getNS_Test`:
```Java
package org.xbill.DNS;
import java.util.LinkedList;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Map;
import java.io.Serializable;
import java.util.TreeMap;
public class Zone implements Serializable {
    public static final int PRIMARY = 1;
    public static final int SECONDARY = 2;
    private RRset NS;
    public RRset getNS() {
        return NS;
    }
public Iterator<RRset> AXFR();
    public void addRRset(RRset rrset);
    public <T extends Record> void addRecord(T r);
    public RRset findExactMatch(Name name, int type);
    public SetResponse findRecords(Name name, int type);
    public int getDClass();
    public Name getOrigin();
    public SOARecord getSOA();
    public Iterator<RRset> iterator();
    public void removeRecord(Record r);
    public synchronized String toMasterFile();
    @Override
public String toString();
}
```


@input{context information}

api document of class org.xbill.DNS.Zone: 

 * A DNS Zone. This encapsulates all data related to a Zone, and provides convenient lookup methods.
 *
 * @author Brian Wellington
 

constructors for class `org.xbill.DNS.Zone`: 
params: org.xbill.DNS.Name zone 
body:
```java
  public Zone(Name zone, String file) throws IOException {
    data = new TreeMap<>();

    if (zone == null) {
      throw new IllegalArgumentException("no zone name specified");
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
body:
```java
  public Zone(Name zone, Record[] records) throws IOException {
    data = new TreeMap<>();

    if (zone == null) {
      throw new IllegalArgumentException("no zone name specified");
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
    fromXFR(xfrin);
  }

```
params: org.xbill.DNS.Name zone 
body:
```java
  public Zone(Name zone, int dclass, String remote) throws IOException, ZoneTransferException {
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ZoneTest {
  private static final ARecord A_TEST;
  private static final AAAARecord AAAA_1_TEST;
  private static final AAAARecord AAAA_2_TEST;
  private static final ARecord A_WILD;
  private static final TXTRecord TXT_WILD;
  private static final Zone ZONE;

  static {
    try {
      Name nameZone = new Name("example.");
      InetAddress localhost4 = InetAddress.getByName("127.0.0.1");
      InetAddress localhost6a = InetAddress.getByName("::1");
      InetAddress localhost6b = InetAddress.getByName("::2");
      A_TEST = new ARecord(new Name("test", nameZone), DClass.IN, 3600, localhost4);
      AAAA_1_TEST = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6a);
      AAAA_2_TEST = new AAAARecord(new Name("test", nameZone), DClass.IN, 3600, localhost6b);
      A_WILD = new ARecord(new Name("*", nameZone), DClass.IN, 3600, localhost4);
      TXT_WILD = new TXTRecord(new Name("*", nameZone), DClass.IN, 3600, "sometext");

      Record[] zoneRecords =
          new Record[] {
            new SOARecord(
                nameZone,
                DClass.IN,
                3600L,
                Name.fromConstantString("nameserver."),
                new Name("hostmaster", nameZone),
                1,
                21600L,
                7200L,
                2160000L,
                3600L),
            new NSRecord(nameZone, DClass.IN, 300L, Name.fromConstantString("nameserver.")),
            A_TEST,
            AAAA_1_TEST,
            AAAA_2_TEST,
            A_WILD,
            TXT_WILD,
          };
      ZONE = new Zone(nameZone, zoneRecords);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void exactNameExistingALookup() {
    Name testName = Name.fromConstantString("test.example.");
    SetResponse resp = ZONE.findRecords(testName, Type.A);
    assertEquals(oneRRset(A_TEST), resp.answers());
  }

  @Test
  void exactNameTwoAaaaLookup() {
    Name testName = Name.fromConstantString("test.example.");
    SetResponse resp = ZONE.findRecords(testName, Type.AAAA);
    assertEquals(oneRRset(AAAA_1_TEST, AAAA_2_TEST), resp.answers());
  }

  @Test
  void exactNameAnyLookup() {
    Name testName = Name.fromConstantString("test.example.");
    SetResponse resp = ZONE.findRecords(testName, Type.ANY);
    assertTrue(resp.isSuccessful());
    assertEquals(listOf(new RRset(A_TEST), new RRset(AAAA_1_TEST, AAAA_2_TEST)), resp.answers());
  }

  @Test
  void wildNameExistingALookup() {
    Name testName = Name.fromConstantString("undefined.example.");
    SetResponse resp = ZONE.findRecords(testName, Type.A);
    assertEquals(oneRRset(A_WILD.withName(testName)), resp.answers());
  }

  @Test
  void wildNameExistingTxtLookup() {
    Name testName = Name.fromConstantString("undefined.example.");
    SetResponse resp = ZONE.findRecords(testName, Type.TXT);
    assertEquals(oneRRset(TXT_WILD.withName(testName)), resp.answers());
  }

  @Test
  void wildNameNonExistingMxLookup() {
    SetResponse resp = ZONE.findRecords(Name.fromConstantString("undefined.example."), Type.MX);
    assertTrue(resp.isNXDOMAIN());
  }

  @Test
  void wildNameAnyLookup() {
    Name testName = Name.fromConstantString("undefined.example.");
    SetResponse resp = ZONE.findRecords(testName, Type.ANY);
    assertTrue(resp.isSuccessful());
    assertEquals(
        listOf(new RRset(A_WILD.withName(testName)), new RRset(TXT_WILD.withName(testName))),
        resp.answers());
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
  private void addNS(Message response, Zone zone, int flags) {
    RRset nsRecords = zone.getNS();
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
class Zone_getNS_Test {
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
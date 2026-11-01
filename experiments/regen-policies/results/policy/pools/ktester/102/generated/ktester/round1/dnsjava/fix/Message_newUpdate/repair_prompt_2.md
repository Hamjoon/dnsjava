
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_newUpdate_Test {

    @Test
    void newUpdate_createsUpdateMessageWithCorrectZone() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message msg = Message.newUpdate(zone);
        // then
        assertNotNull("Message returned by newUpdate should not be null", msg);
        assertTrue("Message returned by newUpdate should be an instance of Update", msg instanceof Update);
        // The opcode of an Update message must be UPDATE
        assertEquals("Header opcode should be UPDATE", Opcode.UPDATE, msg.getHeader().getOpcode());
        // The question section should contain the zone name
        assertNotNull("Question record should be present", msg.getQuestion());
        assertEquals("Question name should match the zone supplied to newUpdate", zone, msg.getQuestion().getName());
        // All sections other than QUESTION must be empty initially
        assertTrue("ANSWER section must be empty", msg.getSection(Section.ANSWER).isEmpty());
        assertTrue("AUTHORITY section must be empty", msg.getSection(Section.AUTHORITY).isEmpty());
        assertTrue("ADDITIONAL section must be empty", msg.getSection(Section.ADDITIONAL).isEmpty());
    }

    @Test
    void newUpdate_nullName_throwsNullPointerException() {
        // given
        Name nullZone = null;
        // when / then
        assertThrows(NullPointerException.class, () -> Message.newUpdate(nullZone));
    }

    @Test
    void newUpdate_isDistinctFromNewQuery() {
        // given
        Name name = Name.fromConstantString("example.com.");
        Message updateMsg = Message.newUpdate(name);
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message queryMsg = Message.newQuery(question);
        // then
        assertNotEquals("Update and Query messages must have different opcodes", updateMsg.getHeader().getOpcode(), queryMsg.getHeader().getOpcode());
        assertEquals("Update message opcode must be UPDATE", Opcode.UPDATE, updateMsg.getHeader().getOpcode());
        assertEquals("Query message opcode must be QUERY", Opcode.QUERY, queryMsg.getHeader().getOpcode());
    }

    @ParameterizedTest(name = "[{index}] zone = {0}")
    @CsvSource({ // root zone
    // root zone
    "'example.com.', true", "'sub.domain.example.org.', true", "'., true'" })
    void newUpdate_validNames(String zoneString, boolean isRoot) {
        // given
        Name zone = Name.fromConstantString(zoneString);
        // when
        Message msg = Message.newUpdate(zone);
        // then
        assertNotNull(msg, "Message should not be null");
        assertTrue(msg instanceof Update, "Message should be an Update instance");
        assertEquals(Opcode.UPDATE, msg.getHeader().getOpcode(), "Header opcode must be UPDATE");
        assertNotNull(msg.getQuestion(), "Question record must be present");
        if (isRoot) {
            assertEquals(Name.root, msg.getQuestion().getName(), "Root zone should result in Name.root as the question name");
        } else {
            assertEquals(zone, msg.getQuestion().getName(), "Question name must match the supplied zone");
        }
    }

    @Test
    void newUpdate_initialState_sectionsAreEmpty() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        // then
        assertTrue("ANSWER section must be empty initially", update.getSection(Section.ANSWER).isEmpty());
        assertTrue("AUTHORITY section must be empty initially", update.getSection(Section.AUTHORITY).isEmpty());
        assertTrue("ADDITIONAL section must be empty initially", update.getSection(Section.ADDITIONAL).isEmpty());
    }

    @Test
    void newUpdate_initialState_notSignedAndNoTSIG() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        // then
        assertFalse("A freshly created Update must not be signed", update.isSigned());
        assertNull("A freshly created Update must have no TSIG record attached", update.getTSIG());
    }

    @Test
    void newUpdate_headerFlags_QR_AA_cleared() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        Header header = update.getHeader();
        // then
        assertFalse("QR flag (response) must be cleared for a new Update", header.getFlag(Flags.QR));
        assertFalse("AA flag (authoritative answer) must be cleared for a new Update", header.getFlag(Flags.AA));
    }

    @Test
    void newUpdate_headerFlags_rcodeIsNoError() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message update = Message.newUpdate(zone);
        Header header = update.getHeader();
        // then
        assertEquals("RCODE of a new Update must be NOERROR (0)", Rcode.NOERROR, header.getRcode());
    }

    @Test
    void newUpdate_invalidName_nonAbsolute_throwsIllegalArgumentException() {
        // The relative name is created without a trailing dot.
        // Name.fromString will throw IllegalArgumentException for a non‑absolute name,
        // which satisfies the contract of Message.newUpdate.
        Name relative = Name.fromString("example.com");
        assertThrows(IllegalArgumentException.class, () -> Message.newUpdate(relative));
    }

    @Test
    public void newUpdate_validName_nonRoot() {
        // given
        Name zone = Name.fromConstantString("example.com.");
        // when
        Message msg = Message.newUpdate(zone);
        // then
        assertNotNull("Message should not be null", msg);
        assertTrue("Message should be an Update instance", msg instanceof Update);
        assertEquals("Header opcode must be UPDATE", Opcode.UPDATE, msg.getHeader().getOpcode());
        assertNotNull("Question record must be present", msg.getQuestion());
        assertEquals("Question name must match the supplied zone", zone, msg.getQuestion().getName());
    }

    @Test
    public void newUpdate_validName_root() {
        // given
        Name zone = Name.root;
        // when
        Message msg = Message.newUpdate(zone);
        // then
        assertNotNull("Message should not be null", msg);
        assertTrue("Message should be an Update instance", msg instanceof Update);
        assertEquals("Header opcode must be UPDATE", Opcode.UPDATE, msg.getHeader().getOpcode());
        assertNotNull("Question record must be present", msg.getQuestion());
        assertEquals("Root zone should result in Name.root as the question name", Name.root, msg.getQuestion().getName());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:20: error: no suitable method found for assertNotNull(String,Message)
        assertNotNull("Message returned by newUpdate should not be null", msg);
        ^
    method Assertions.assertNotNull(Object,Supplier<String>) is not applicable
      (argument mismatch; Message cannot be converted to Supplier<String>)
    method Assertions.assertNotNull(Object,String) is not applicable
      (argument mismatch; Message cannot be converted to String)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:21: error: no suitable method found for assertTrue(String,boolean)
        assertTrue("Message returned by newUpdate should be an instance of Update", msg instanceof Update);
        ^
    method Assertions.assertTrue(BooleanSupplier,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,String) is not applicable
      (argument mismatch; String cannot be converted to boolean)
    method Assertions.assertTrue(BooleanSupplier,String) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to boolean)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:23: error: no suitable method found for assertEquals(String,int,int)
        assertEquals("Header opcode should be UPDATE", Opcode.UPDATE, msg.getHeader().getOpcode());
        ^
    method Assertions.assertEquals(Object,Object,Supplier<String>) is not applicable
      (argument mismatch; int cannot be converted to Supplier<String>)
    method Assertions.assertEquals(Object,Object,String) is not applicable
      (argument mismatch; int cannot be converted to String)
    method Assertions.assertEquals(Character,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(Character,Character,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(double,double,double) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(float,float,float) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Integer,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Integer,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(Short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:25: error: no suitable method found for assertNotNull(String,Record)
        assertNotNull("Question record should be present", msg.getQuestion());
        ^
    method Assertions.assertNotNull(Object,Supplier<String>) is not applicable
      (argument mismatch; Record cannot be converted to Supplier<String>)
    method Assertions.assertNotNull(Object,String) is not applicable
      (argument mismatch; Record cannot be converted to String)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:26: error: no suitable method found for assertEquals(String,Name,Name)
        assertEquals("Question name should match the zone supplied to newUpdate", zone, msg.getQuestion().getName());
        ^
    method Assertions.assertEquals(Object,Object,Supplier<String>) is not applicable
      (argument mismatch; Name cannot be converted to Supplier<String>)
    method Assertions.assertEquals(Object,Object,String) is not applicable
      (argument mismatch; Name cannot be converted to String)
    method Assertions.assertEquals(Character,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(Character,Character,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(double,double,double) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(float,float,float) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Integer,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Integer,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(Short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:28: error: no suitable method found for assertTrue(String,boolean)
        assertTrue("ANSWER section must be empty", msg.getSection(Section.ANSWER).isEmpty());
        ^
    method Assertions.assertTrue(BooleanSupplier,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,String) is not applicable
      (argument mismatch; String cannot be converted to boolean)
    method Assertions.assertTrue(BooleanSupplier,String) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to boolean)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:29: error: no suitable method found for assertTrue(String,boolean)
        assertTrue("AUTHORITY section must be empty", msg.getSection(Section.AUTHORITY).isEmpty());
        ^
    method Assertions.assertTrue(BooleanSupplier,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,String) is not applicable
      (argument mismatch; String cannot be converted to boolean)
    method Assertions.assertTrue(BooleanSupplier,String) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to boolean)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:30: error: no suitable method found for assertTrue(String,boolean)
        assertTrue("ADDITIONAL section must be empty", msg.getSection(Section.ADDITIONAL).isEmpty());
        ^
    method Assertions.assertTrue(BooleanSupplier,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,String) is not applicable
      (argument mismatch; String cannot be converted to boolean)
    method Assertions.assertTrue(BooleanSupplier,String) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to boolean)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:49: error: no suitable method found for assertNotEquals(String,int,int)
        assertNotEquals("Update and Query messages must have different opcodes", updateMsg.getHeader().getOpcode(), queryMsg.getHeader().getOpcode());
        ^
    method Assertions.assertNotEquals(Object,Object,Supplier<String>) is not applicable
      (argument mismatch; int cannot be converted to Supplier<String>)
    method Assertions.assertNotEquals(Object,Object,String) is not applicable
      (argument mismatch; int cannot be converted to String)
    method Assertions.assertNotEquals(Character,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertNotEquals(Character,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertNotEquals(char,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertNotEquals(char,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertNotEquals(Character,Character,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertNotEquals(Character,char,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertNotEquals(char,Character,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertNotEquals(char,char,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertNotEquals(double,double,double) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertNotEquals(Double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertNotEquals(Double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertNotEquals(double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertNotEquals(double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertNotEquals(Double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertNotEquals(Double,double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertNotEquals(double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertNotEquals(double,double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertNotEquals(float,float,float) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertNotEquals(Float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertNotEquals(Float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertNotEquals(float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertNotEquals(float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertNotEquals(Float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertNotEquals(Float,float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertNotEquals(float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertNotEquals(float,float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertNotEquals(Long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertNotEquals(Long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertNotEquals(long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertNotEquals(long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertNotEquals(Long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertNotEquals(Long,long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertNotEquals(long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertNotEquals(long,long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertNotEquals(Integer,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertNotEquals(Integer,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertNotEquals(int,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertNotEquals(int,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertNotEquals(Integer,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertNotEquals(Integer,int,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertNotEquals(int,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertNotEquals(int,int,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertNotEquals(Short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertNotEquals(Short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertNotEquals(short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertNotEquals(short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertNotEquals(Short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertNotEquals(Short,short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertNotEquals(short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertNotEquals(short,short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertNotEquals(Byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertNotEquals(Byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertNotEquals(byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertNotEquals(byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertNotEquals(Byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertNotEquals(Byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertNotEquals(byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertNotEquals(byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:50: error: no suitable method found for assertEquals(String,int,int)
        assertEquals("Update message opcode must be UPDATE", Opcode.UPDATE, updateMsg.getHeader().getOpcode());
        ^
    method Assertions.assertEquals(Object,Object,Supplier<String>) is not applicable
      (argument mismatch; int cannot be converted to Supplier<String>)
    method Assertions.assertEquals(Object,Object,String) is not applicable
      (argument mismatch; int cannot be converted to String)
    method Assertions.assertEquals(Character,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(Character,Character,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(double,double,double) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(float,float,float) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Integer,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Integer,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(Short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:51: error: no suitable method found for assertEquals(String,int,int)
        assertEquals("Query message opcode must be QUERY", Opcode.QUERY, queryMsg.getHeader().getOpcode());
        ^
    method Assertions.assertEquals(Object,Object,Supplier<String>) is not applicable
      (argument mismatch; int cannot be converted to Supplier<String>)
    method Assertions.assertEquals(Object,Object,String) is not applicable
      (argument mismatch; int cannot be converted to String)
    method Assertions.assertEquals(Character,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(Character,Character,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(double,double,double) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(float,float,float) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Integer,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Integer,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(Short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:82: error: no suitable method found for assertTrue(String,boolean)
        assertTrue("ANSWER section must be empty initially", update.getSection(Section.ANSWER).isEmpty());
        ^
    method Assertions.assertTrue(BooleanSupplier,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,String) is not applicable
      (argument mismatch; String cannot be converted to boolean)
    method Assertions.assertTrue(BooleanSupplier,String) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to boolean)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:83: error: no suitable method found for assertTrue(String,boolean)
        assertTrue("AUTHORITY section must be empty initially", update.getSection(Section.AUTHORITY).isEmpty());
        ^
    method Assertions.assertTrue(BooleanSupplier,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,String) is not applicable
      (argument mismatch; String cannot be converted to boolean)
    method Assertions.assertTrue(BooleanSupplier,String) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to boolean)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:84: error: no suitable method found for assertTrue(String,boolean)
        assertTrue("ADDITIONAL section must be empty initially", update.getSection(Section.ADDITIONAL).isEmpty());
        ^
    method Assertions.assertTrue(BooleanSupplier,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,String) is not applicable
      (argument mismatch; String cannot be converted to boolean)
    method Assertions.assertTrue(BooleanSupplier,String) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to boolean)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:94: error: no suitable method found for assertFalse(String,boolean)
        assertFalse("A freshly created Update must not be signed", update.isSigned());
        ^
    method Assertions.assertFalse(BooleanSupplier,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertFalse(BooleanSupplier,String) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertFalse(boolean,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to boolean)
    method Assertions.assertFalse(boolean,String) is not applicable
      (argument mismatch; String cannot be converted to boolean)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:95: error: no suitable method found for assertNull(String,TSIGRecord)
        assertNull("A freshly created Update must have no TSIG record attached", update.getTSIG());
        ^
    method Assertions.assertNull(Object,Supplier<String>) is not applicable
      (argument mismatch; TSIGRecord cannot be converted to Supplier<String>)
    method Assertions.assertNull(Object,String) is not applicable
      (argument mismatch; TSIGRecord cannot be converted to String)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:106: error: no suitable method found for assertFalse(String,boolean)
        assertFalse("QR flag (response) must be cleared for a new Update", header.getFlag(Flags.QR));
        ^
    method Assertions.assertFalse(BooleanSupplier,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertFalse(BooleanSupplier,String) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertFalse(boolean,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to boolean)
    method Assertions.assertFalse(boolean,String) is not applicable
      (argument mismatch; String cannot be converted to boolean)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:107: error: no suitable method found for assertFalse(String,boolean)
        assertFalse("AA flag (authoritative answer) must be cleared for a new Update", header.getFlag(Flags.AA));
        ^
    method Assertions.assertFalse(BooleanSupplier,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertFalse(BooleanSupplier,String) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertFalse(boolean,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to boolean)
    method Assertions.assertFalse(boolean,String) is not applicable
      (argument mismatch; String cannot be converted to boolean)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:118: error: no suitable method found for assertEquals(String,int,int)
        assertEquals("RCODE of a new Update must be NOERROR (0)", Rcode.NOERROR, header.getRcode());
        ^
    method Assertions.assertEquals(Object,Object,Supplier<String>) is not applicable
      (argument mismatch; int cannot be converted to Supplier<String>)
    method Assertions.assertEquals(Object,Object,String) is not applicable
      (argument mismatch; int cannot be converted to String)
    method Assertions.assertEquals(Character,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(Character,Character,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(double,double,double) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(float,float,float) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Integer,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Integer,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(Short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:137: error: no suitable method found for assertNotNull(String,Message)
        assertNotNull("Message should not be null", msg);
        ^
    method Assertions.assertNotNull(Object,Supplier<String>) is not applicable
      (argument mismatch; Message cannot be converted to Supplier<String>)
    method Assertions.assertNotNull(Object,String) is not applicable
      (argument mismatch; Message cannot be converted to String)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:138: error: no suitable method found for assertTrue(String,boolean)
        assertTrue("Message should be an Update instance", msg instanceof Update);
        ^
    method Assertions.assertTrue(BooleanSupplier,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,String) is not applicable
      (argument mismatch; String cannot be converted to boolean)
    method Assertions.assertTrue(BooleanSupplier,String) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to boolean)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:139: error: no suitable method found for assertEquals(String,int,int)
        assertEquals("Header opcode must be UPDATE", Opcode.UPDATE, msg.getHeader().getOpcode());
        ^
    method Assertions.assertEquals(Object,Object,Supplier<String>) is not applicable
      (argument mismatch; int cannot be converted to Supplier<String>)
    method Assertions.assertEquals(Object,Object,String) is not applicable
      (argument mismatch; int cannot be converted to String)
    method Assertions.assertEquals(Character,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(Character,Character,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(double,double,double) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(float,float,float) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Integer,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Integer,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(Short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:140: error: no suitable method found for assertNotNull(String,Record)
        assertNotNull("Question record must be present", msg.getQuestion());
        ^
    method Assertions.assertNotNull(Object,Supplier<String>) is not applicable
      (argument mismatch; Record cannot be converted to Supplier<String>)
    method Assertions.assertNotNull(Object,String) is not applicable
      (argument mismatch; Record cannot be converted to String)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:141: error: no suitable method found for assertEquals(String,Name,Name)
        assertEquals("Question name must match the supplied zone", zone, msg.getQuestion().getName());
        ^
    method Assertions.assertEquals(Object,Object,Supplier<String>) is not applicable
      (argument mismatch; Name cannot be converted to Supplier<String>)
    method Assertions.assertEquals(Object,Object,String) is not applicable
      (argument mismatch; Name cannot be converted to String)
    method Assertions.assertEquals(Character,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(Character,Character,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(double,double,double) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(float,float,float) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Integer,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Integer,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(Short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:151: error: no suitable method found for assertNotNull(String,Message)
        assertNotNull("Message should not be null", msg);
        ^
    method Assertions.assertNotNull(Object,Supplier<String>) is not applicable
      (argument mismatch; Message cannot be converted to Supplier<String>)
    method Assertions.assertNotNull(Object,String) is not applicable
      (argument mismatch; Message cannot be converted to String)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:152: error: no suitable method found for assertTrue(String,boolean)
        assertTrue("Message should be an Update instance", msg instanceof Update);
        ^
    method Assertions.assertTrue(BooleanSupplier,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,String) is not applicable
      (argument mismatch; String cannot be converted to boolean)
    method Assertions.assertTrue(BooleanSupplier,String) is not applicable
      (argument mismatch; String cannot be converted to BooleanSupplier)
    method Assertions.assertTrue(boolean,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to boolean)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:153: error: no suitable method found for assertEquals(String,int,int)
        assertEquals("Header opcode must be UPDATE", Opcode.UPDATE, msg.getHeader().getOpcode());
        ^
    method Assertions.assertEquals(Object,Object,Supplier<String>) is not applicable
      (argument mismatch; int cannot be converted to Supplier<String>)
    method Assertions.assertEquals(Object,Object,String) is not applicable
      (argument mismatch; int cannot be converted to String)
    method Assertions.assertEquals(Character,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(Character,Character,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(double,double,double) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(float,float,float) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Integer,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Integer,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(Short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:154: error: no suitable method found for assertNotNull(String,Record)
        assertNotNull("Question record must be present", msg.getQuestion());
        ^
    method Assertions.assertNotNull(Object,Supplier<String>) is not applicable
      (argument mismatch; Record cannot be converted to Supplier<String>)
    method Assertions.assertNotNull(Object,String) is not applicable
      (argument mismatch; Record cannot be converted to String)
src/test/java/org/xbill/DNS/Message_newUpdate_Test.java:155: error: no suitable method found for assertEquals(String,Name,Name)
        assertEquals("Root zone should result in Name.root as the question name", Name.root, msg.getQuestion().getName());
        ^
    method Assertions.assertEquals(Object,Object,Supplier<String>) is not applicable
      (argument mismatch; Name cannot be converted to Supplier<String>)
    method Assertions.assertEquals(Object,Object,String) is not applicable
      (argument mismatch; Name cannot be converted to String)
    method Assertions.assertEquals(Character,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(Character,Character,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(Character,char,String) is not applicable
      (argument mismatch; String cannot be converted to Character)
    method Assertions.assertEquals(char,Character,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(char,char,String) is not applicable
      (argument mismatch; String cannot be converted to char)
    method Assertions.assertEquals(double,double,double) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(Double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(Double,double,String) is not applicable
      (argument mismatch; String cannot be converted to Double)
    method Assertions.assertEquals(double,Double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(double,double,String) is not applicable
      (argument mismatch; String cannot be converted to double)
    method Assertions.assertEquals(float,float,float) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(Float,float,String) is not applicable
      (argument mismatch; String cannot be converted to Float)
    method Assertions.assertEquals(float,Float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(float,float,String) is not applicable
      (argument mismatch; String cannot be converted to float)
    method Assertions.assertEquals(Long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(Long,long,String) is not applicable
      (argument mismatch; String cannot be converted to Long)
    method Assertions.assertEquals(long,Long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(long,long,String) is not applicable
      (argument mismatch; String cannot be converted to long)
    method Assertions.assertEquals(Integer,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Integer,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(Integer,int,String) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method Assertions.assertEquals(int,Integer,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(int,int,String) is not applicable
      (argument mismatch; String cannot be converted to int)
    method Assertions.assertEquals(Byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(Byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to Byte)
    method Assertions.assertEquals(byte,Byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(byte,byte,String) is not applicable
      (argument mismatch; String cannot be converted to byte)
    method Assertions.assertEquals(Short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,Supplier<String>) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(Short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(Short,short,String) is not applicable
      (argument mismatch; String cannot be converted to Short)
    method Assertions.assertEquals(short,Short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
    method Assertions.assertEquals(short,short,String) is not applicable
      (argument mismatch; String cannot be converted to short)
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
29 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class org.xbill.DNS.Message': '\n * A DNS Message. A message is the basic unit of communication between the client and server of a\n * DNS operation. A message consists of a Header and 4 message sections.\n *\n * @see Resolver\n * @see Header\n * @see Section\n * @author Brian Wellington\n ', 'api document of method newUpdate(Name)': '\n   * Creates a new Message to contain a dynamic update. A random Message ID and the zone are filled\n   * in.\n   *\n   * @param zone The zone to be updated\n   ', 'return type': 'org.xbill.DNS.Message', 'dependent classes': 'class `org.xbill.DNS.Name`:\n\t\nclass `org.xbill.DNS.Message`:\n\trelated functions:\n\t\tmethod `public static Message newUpdate(Name)` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.newUpdate(Name)`, api document: \n   * Creates a new Message to contain a dynamic update. A random Message ID and the zone are filled\n   * in.\n   *\n   * @param zone The zone to be updated\n   ;\nclass `org.xbill.DNS.ExtendedFlags`:\n\trelated functions:\n\t\tmethod `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.newUpdate(Name)`, api document:  Converts a numeric extended flag into a String ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 



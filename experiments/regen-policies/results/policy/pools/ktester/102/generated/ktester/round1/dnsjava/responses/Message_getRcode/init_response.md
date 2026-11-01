```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Message_getRcode_Test {

    /** The Message instance under test – created as a spy so we can stub its collaborators. */
    private Message messageSpy;

    /** Mocked Header returned by {@code messageSpy.getHeader()}. */
    @Mock
    private Header mockHeader;

    /** Mocked OPTRecord returned by {@code messageSpy.getOPT()}. */
    @Mock
    private OPTRecord mockOpt;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a real Message (using the no‑arg constructor) and then spy it.
        Message realMessage = new Message();
        messageSpy = Mockito.spy(realMessage);

        // Stub the getHeader() call to return our mockHeader.
        doReturn(mockHeader).when(messageSpy).getHeader();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        Mockito.reset(mockHeader, mockOpt, messageSpy);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that {@code getRcode()} returns the value supplied by the Header when
     * no OPTRecord is present in the message.
     */
    @Test
    void getRcode_withoutOPT_returnsHeaderRcode() {
        // Arrange: Header reports an rcode of 2 (e.g., SERVFAIL).
        when(mockHeader.getRcode()).thenReturn(2);
        // Ensure getOPT() returns null (no OPTRecord in the message).
        doReturn(null).when(messageSpy).getOPT();

        // Act
        int rcode = messageSpy.getRcode();

        // Assert
        assertEquals(2, rcode, "Rcode should equal the header's rcode when no OPTRecord is present");
        // Verify interactions
        verify(mockHeader, times(1)).getRcode();
        verify(messageSpy, times(1)).getOPT();
    }

    /**
     * Verify that {@code getRcode()} adds the extended rcode from an OPTRecord
     * (shifted left by 4 bits) to the base rcode from the Header.
     */
    @Test
    void getRcode_withOPT_addsExtendedRcode() {
        // Arrange: Header reports a base rcode of 1 (e.g., FORMERR).
        when(mockHeader.getRcode()).thenReturn(1);
        // OPTRecord reports an extended rcode of 3.
        when(mockOpt.getExtendedRcode()).thenReturn(3);
        // Stub getOPT() to return our mock OPTRecord.
        doReturn(mockOpt).when(messageSpy).getOPT();

        // Act
        int rcode = messageSpy.getRcode();

        // Expected: base rcode + (extended << 4) = 1 + (3 << 4) = 1 + 48 = 49
        int expected = 1 + (3 << 4);
        assertEquals(expected, rcode,
                "Rcode should combine header rcode with extended rcode from OPTRecord");

        // Verify interactions
        verify(mockHeader, times(1)).getRcode();
        verify(mockOpt, times(1)).getExtendedRcode();
        verify(messageSpy, times(1)).getOPT();
    }
}
```



You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package org.xbill.DNS;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Message_getResolver_Test {

    private Message message;

    @Mock
    private Resolver mockResolver;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Message instance before each test
        message = new Message();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid interaction leakage between tests
        reset(mockResolver);
    }

    @Test
    void getResolver_whenNotSet_returnsEmptyOptional() {
        Optional<Resolver> opt = message.getResolver();
        assertTrue(opt.isEmpty(), "Expected Optional to be empty when resolver is not set");
    }

    @Test
    void getResolver_whenSet_returnsOptionalContainingResolver() {
        // Arrange
        message.setResolver(mockResolver);
        // Act
        Optional<Resolver> opt = message.getResolver();
        // Assert
        assertTrue(opt.isPresent(), "Expected Optional to contain a resolver after setting it");
        assertSame(mockResolver, opt.get(), "Expected the resolver returned to be the same instance that was set");
    }

    @Test
    void getResolver_afterSettingNull_returnsEmptyOptional() {
        // Arrange: explicitly set null (allowed by the setter)
        message.setResolver(null);
        // Act
        Optional<Resolver> opt = message.getResolver();
        // Assert
        assertTrue(opt.isEmpty(), "Expected Optional to be empty after setting resolver to null");
    }

    @Test
    void getResolver_afterClone_returnsSameResolverInstance() {
        // Arrange
        message.setResolver(mockResolver);
        // Act & Assert
        Message cloned = assertDoesNotThrow(() -> (Message) message.clone(), "Cloning a Message should not throw");
        Optional<Resolver> clonedOpt = cloned.getResolver();
        assertTrue(clonedOpt.isPresent(), "Cloned message should still contain a resolver");
        assertSame(mockResolver, clonedOpt.get(), "Resolver instance must be identical after cloning (reference equality)");
    }

    @Test
    void getResolver_independentInstances_doNotShareResolver() {
        // Arrange: first message gets a resolver, second stays untouched
        Message first = new Message();
        Message second = new Message();
        first.setResolver(mockResolver);
        // Act
        Optional<Resolver> firstOpt = first.getResolver();
        Optional<Resolver> secondOpt = second.getResolver();
        // Assert
        assertTrue(firstOpt.isPresent(), "First message should contain the resolver");
        assertSame(mockResolver, firstOpt.get(), "First message must return the resolver that was set");
        assertTrue(secondOpt.isEmpty(), "Second message should have no resolver");
    }

    @Test
    void getResolver_subsequentSetResolver_overwritesPrevious() {
        // Arrange
        Resolver firstResolver = mock(Resolver.class);
        Resolver secondResolver = mock(Resolver.class);
        message.setResolver(firstResolver);
        // Act: overwrite with a different resolver
        message.setResolver(secondResolver);
        Optional<Resolver> opt = message.getResolver();
        // Assert
        assertTrue(opt.isPresent(), "Resolver should be present after being overwritten");
        assertSame(secondResolver, opt.get(), "The resolver returned must be the most recently set instance");
    }

    @Test
    void getResolver_factoryMethodsReturnEmptyOptional() {
        // Arrange: create dummy arguments for the factories
        Record dummyRecord = mock(Record.class);
        Name dummyName = mock(Name.class);
        // Act
        Message queryMessage = Message.newQuery(dummyRecord);
        Message updateMessage = Message.newUpdate(dummyName);
        // Assert
        assertTrue(queryMessage.getResolver().isEmpty(), "Message created via newQuery() must not have a resolver");
        assertTrue(updateMessage.getResolver().isEmpty(), "Message created via newUpdate() must not have a resolver");
    }

    @Test
    void getResolver_multipleCallsAlwaysReturnSameInstance() {
        // Arrange
        message.setResolver(mockResolver);
        // Act & Assert: call getResolver several times and verify the result stays identical
        for (int i = 0; i < 5; i++) {
            Optional<Resolver> opt = message.getResolver();
            assertTrue(opt.isPresent(), "Call #" + (i + 1) + " should return a present Optional");
            assertSame(mockResolver, opt.get(), "Call #" + (i + 1) + " must return the same resolver instance");
        }
    }
}

```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/repo/experiments/regen-policies/results/policy/pools/ktester/34/work/ktester/ktester/projects/dnsjava/libs/slf4j-simple-1.7.36.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/work/code/dependencies/evosuite-1.2.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.SimpleLoggerFactory]

╷
├─ JUnit Jupiter ✔
│  └─ Message_getResolver_Test ✔
│     ├─ getResolver_multipleCallsAlwaysReturnSameInstance() ✔
│     ├─ getResolver_afterSettingNull_returnsEmptyOptional() ✔
│     ├─ getResolver_factoryMethodsReturnEmptyOptional() ✘ 'Mock for Name, hashCode: 481792876' is not an absolute name
│     ├─ getResolver_afterClone_returnsSameResolverInstance() ✔
│     ├─ getResolver_whenSet_returnsOptionalContainingResolver() ✔
│     ├─ getResolver_whenNotSet_returnsEmptyOptional() ✔
│     ├─ getResolver_independentInstances_doNotShareResolver() ✔
│     └─ getResolver_subsequentSetResolver_overwritesPrevious() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (1):
  JUnit Jupiter:Message_getResolver_Test:getResolver_factoryMethodsReturnEmptyOptional()
    MethodSource [className = 'org.xbill.DNS.Message_getResolver_Test', methodName = 'getResolver_factoryMethodsReturnEmptyOptional', methodParameterTypes = '']
    => org.xbill.DNS.RelativeNameException: 'Mock for Name, hashCode: 481792876' is not an absolute name
       org.xbill.DNS.Update.<init>(Update.java:27)
       org.xbill.DNS.Update.<init>(Update.java:43)
       org.xbill.DNS.Message.newUpdate(Message.java:98)
       org.xbill.DNS.Message_getResolver_Test.getResolver_factoryMethodsReturnEmptyOptional(Message_getResolver_Test.java:105)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]

Test run finished after 524 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         8 tests found           ]
[         0 tests skipped         ]
[         8 tests started         ]
[         0 tests aborted         ]
[         7 tests successful      ]
[         1 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class org.xbill.DNS.Message: 

 * A DNS Message. A message is the basic unit of communication between the client and server of a
 * DNS operation. A message consists of a Header and 4 message sections.
 *
 * @see Resolver
 * @see Header
 * @see Section
 * @author Brian Wellington
 

api document of method getResolver(): 
 Gets the resolver that originally received this Message from a server. 

dependent classes: 
class `org.xbill.DNS.Message`:
	dependent fields:
		org.xbill.DNS.Resolver org.xbill.DNS.Message.resolver;
	related functions:
		method `public void setResolver(Resolver)` returns `void`, related with `org.xbill.DNS.Message.getResolver()`, api document:  Sets the resolver that originally received this Message from a server. ;
class `org.xbill.DNS.CookieOption`:
	related functions:
		method `public Optional<byte[]> getServerCookie()` returns `java.util.Optional<byte[]>`, related with `org.xbill.DNS.Message.getResolver()`, api document: 
   * Returns the server cookie.
   *
   * @return the server cookie
   ;
class `org.xbill.DNS.lookup.LookupSession`:
	related functions:
		method `private Message maybeAddToCache(Message)` returns `org.xbill.DNS.Message`, related with `org.xbill.DNS.Message.getResolver()`;
		method `private CompletionStage<LookupResult> lookupWithCache(Record, List<Name>)` returns `java.util.concurrent.CompletionStage<org.xbill.DNS.lookup.LookupResult>`, related with `org.xbill.DNS.Message.getResolver()`;
class `org.xbill.DNS.hosts.HostsFileParser`:
	related functions:
		method `public synchronized Optional<InetAddress> getAddressForHost(Name, int)` returns `java.util.Optional<java.net.InetAddress>`, related with `org.xbill.DNS.Message.getResolver()`, api document: 
   * Performs on-demand parsing and caching of the local hosts database.
   *
   * @param name the hostname to search for.
   * @param type Record type to search for, see {@link org.xbill.DNS.Type}.
   * @return The first address found for the requested hostname.
   * @throws IOException When the parsing fails.
   * @throws IllegalArgumentException when {@code type} is not {@link org.xbill.DNS.Type#A} or{@link
   *     org.xbill.DNS.Type#AAAA}.
   ;
class `org.xbill.DNS.ExtendedFlags`:
	related functions:
		method `public static String string(int)` returns `java.lang.String`, related with `org.xbill.DNS.Message.getResolver()`, api document:  Converts a numeric extended flag into a String ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 

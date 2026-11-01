```java
package org.xbill.DNS;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.Random;
import java.util.Arrays;
import java.lang.reflect.Field;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TSIG_generate_Test {

    /**
     * Fixed instant used for deterministic TSIG timestamps.
     */
    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

    /**
     * The TSIG instance under test – HMAC‑SHA256 algorithm with a known key.
     */
   
package cn.ibax.loaderbridge.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeResultTest {
    @Test
    void mapShouldPreserveSuccessValue() {
        BridgeResult<String> result = BridgeResult.success("桥接");

        BridgeResult<Integer> mapped = result.map(String::length);

        assertTrue(mapped.success());
        assertEquals(2, mapped.value());
    }

    @Test
    void failureShouldStayFailureAfterMap() {
        BridgeResult<String> result = BridgeResult.failure("失败");

        BridgeResult<Integer> mapped = result.map(String::length);

        assertFalse(mapped.success());
        assertEquals("失败", mapped.message());
    }
}

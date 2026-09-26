package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.util.LogSanitizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogSanitizerTest {

    @Test
    fun testEmptyAndNullString() {
        assertEquals("", LogSanitizer.sanitize(null))
        assertEquals("", LogSanitizer.sanitize(""))
    }

    @Test
    fun testMasksAuthHeaders() {
        val headerLog = "Request headers: x-api-key: secret_immich_key_987654321, user-agent: okhttp"
        val sanitized = LogSanitizer.sanitize(headerLog)
        assertFalse(sanitized.contains("secret_immich_key_987654321"))
        assertTrue(sanitized.contains("x-api-key: [REDACTED]"))

        val authHeaderLog = "Authorization: Bearer my_super_secret_jwt_token_12345"
        val sanitizedAuth = LogSanitizer.sanitize(authHeaderLog)
        assertFalse(sanitizedAuth.contains("my_super_secret_jwt_token_12345"))
        assertTrue(sanitizedAuth.contains("Authorization: [REDACTED]"))
    }

    @Test
    fun testMasksJsonCredentials() {
        val jsonLog = """{"serverUrl": "http://192.168.1.50:2283", "apiKey": "xyz9876543210"}"""
        val sanitized = LogSanitizer.sanitize(jsonLog)
        assertFalse(sanitized.contains("xyz9876543210"))
        assertTrue(sanitized.contains(""""apiKey": "[REDACTED]""""))
        assertFalse(sanitized.contains("192.168.1.50"))
        assertTrue(sanitized.contains("[PRIVATE_IP]:2283"))
    }

    @Test
    fun testMasksUrlQueryParams() {
        val urlLog = "Fetching from https://api.unsplash.com/photos/random?client_id=mySecretClientId123&orientation=portrait"
        val sanitized = LogSanitizer.sanitize(urlLog)
        // Note: query param regex masks tokens/keys
        val urlWithKey = "Fetching from https://api.example.com/wallpapers?api_key=secretKey123&page=1"
        val sanitizedKey = LogSanitizer.sanitize(urlWithKey)
        assertFalse(sanitizedKey.contains("secretKey123"))
        assertTrue(sanitizedKey.contains("api_key=[REDACTED]"))
    }

    @Test
    fun testMasksPrivateIpAddresses() {
        val ipsLog = "Connecting to 192.168.1.100 and 10.0.0.15 and 172.16.0.5"
        val sanitized = LogSanitizer.sanitize(ipsLog)
        assertFalse(sanitized.contains("192.168.1.100"))
        assertFalse(sanitized.contains("10.0.0.15"))
        assertFalse(sanitized.contains("172.16.0.5"))
        assertEquals("Connecting to [PRIVATE_IP] and [PRIVATE_IP] and [PRIVATE_IP]", sanitized)
    }

    @Test
    fun testLeavesNormalLogUnchanged() {
        val normalLog = "Wallpaper successfully applied: target=BOTH, title='Mountain Sunset'"
        val sanitized = LogSanitizer.sanitize(normalLog)
        assertEquals(normalLog, sanitized)
    }
}

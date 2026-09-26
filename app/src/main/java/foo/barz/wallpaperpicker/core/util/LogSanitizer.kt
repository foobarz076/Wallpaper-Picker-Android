package foo.barz.wallpaperpicker.core.util

import java.util.regex.Pattern

/**
 * Utility responsible for sanitizing sensitive credentials, private IP addresses,
 * and confidential tokens from log messages before diagnostics export or display.
 */
object LogSanitizer {

    // Regex to detect and mask API keys, auth tokens, and secrets in JSON, key-value, or header patterns
    private val CREDENTIAL_KEY_VALUE_PATTERN: Pattern = Pattern.compile(
        """(?i)(["']?(?:api[_-]?key|access[_-]?token|secret|password|token)["']?\s*[:=]\s*["']?)([^"'\s&,;}{]{3,})(["']?)"""
    )

    // Regex to detect and mask HTTP headers like X-Api-Key or Authorization
    private val AUTH_HEADER_PATTERN: Pattern = Pattern.compile(
        """(?i)\b(x-api-key|api-key|authorization)\b(\s*[:=]\s*)(?:Bearer\s+)?([^\s,;]+)"""
    )

    // Regex to mask query parameters in URLs containing tokens or keys
    private val QUERY_PARAM_PATTERN: Pattern = Pattern.compile(
        """(?i)([?&](?:api[_-]?key|access[_-]?token|token|auth|key|secret|client[_-]?secret)=)[^&\s]+"""
    )

    // Regex to mask standalone Bearer tokens
    private val BEARER_TOKEN_PATTERN: Pattern = Pattern.compile(
        """(?i)\b(Bearer\s+)[A-Za-z0-9_\-\.]{8,}\b"""
    )

    // Regex to mask private IPv4 subnets (192.168.x.x, 10.x.x.x, 172.16.x.x - 172.31.x.x)
    private val PRIVATE_IP_PATTERN: Pattern = Pattern.compile(
        """\b(192\.168\.\d{1,3}\.\d{1,3}|10\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.(?:1[6-9]|2\d|3[0-1])\.\d{1,3}\.\d{1,3})\b"""
    )

    /**
     * Sanitizes sensitive information from the input text.
     *
     * @param input Raw text potentially containing credentials or private network information.
     * @return Cleaned string with sensitive items masked.
     */
    fun sanitize(input: String?): String {
        if (input.isNullOrEmpty()) return ""

        var result = input
        result = AUTH_HEADER_PATTERN.matcher(result).replaceAll("$1$2[REDACTED]")
        result = CREDENTIAL_KEY_VALUE_PATTERN.matcher(result).replaceAll("$1[REDACTED]$3")
        result = QUERY_PARAM_PATTERN.matcher(result).replaceAll("$1[REDACTED]")
        result = BEARER_TOKEN_PATTERN.matcher(result).replaceAll("$1[REDACTED]")
        result = PRIVATE_IP_PATTERN.matcher(result).replaceAll("[PRIVATE_IP]")

        return result
    }
}

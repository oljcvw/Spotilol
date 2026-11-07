package com.respotfck.spotifck.proxy

import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.HttpsURLConnection

/** Secure DNS settings shared by the proxy and the settings screen. */
object DnsSettings {
    const val ENABLED = "DnsEnabled"
    const val PROVIDER = "DnsProvider"
    const val CUSTOM_URL = "DnsCustomUrl"
    const val FALLBACK = "DnsFallback"

    data class Provider(val key: String, val label: String, val url: String)

    val providers = listOf(
        Provider("adguard", "AdGuard (blocks ads & trackers)", "https://dns.adguard-dns.com/dns-query"),
        Provider("adguard_family", "AdGuard Family", "https://family.adguard-dns.com/dns-query"),
        Provider("adguard_unfiltered", "AdGuard Unfiltered", "https://unfiltered.adguard-dns.com/dns-query"),
        Provider("cloudflare", "Cloudflare", "https://cloudflare-dns.com/dns-query"),
        Provider("quad9", "Quad9", "https://dns.quad9.net/dns-query"),
        Provider("google", "Google", "https://dns.google/dns-query"),
        Provider("custom", "Custom DoH URL", "")
    )
    const val DEFAULT_PROVIDER = "adguard"

    fun enabled(prefs: SharedPreferences) = prefs.getBoolean(ENABLED, true)
    fun fallback(prefs: SharedPreferences) = prefs.getBoolean(FALLBACK, true)
    fun provider(prefs: SharedPreferences): Provider {
        val key = prefs.getString(PROVIDER, DEFAULT_PROVIDER)
        return providers.firstOrNull { it.key == key } ?: providers.first()
    }
    fun customUrl(prefs: SharedPreferences) = prefs.getString(CUSTOM_URL, "") ?: ""

    fun endpoint(prefs: SharedPreferences): String {
        val p = provider(prefs)
        return if (p.key == "custom") customUrl(prefs).trim().ifEmpty { providers.first().url } else p.url
    }

    fun isValidDohUrl(url: String): Boolean {
        val u = try { URL(url.trim()) } catch (_: Exception) { return false }
        return u.protocol == "https" && !u.host.isNullOrEmpty()
    }

    fun summary(prefs: SharedPreferences): String {
        if (!enabled(prefs)) return "Off (uses the device's DNS)"
        val p = provider(prefs)
        return if (p.key == "custom") "Custom: ${endpoint(prefs)}" else p.label.substringBefore(" (")
    }
}

/** The DoH server answered authoritatively that [host] doesn't exist; no point asking the system resolver. */
class NxDomainException(host: String) : UnknownHostException("$host does not exist (NXDOMAIN)")

/** Thrown when the DNS provider deliberately blocks a host (0.0.0.0 / ::). */
class DnsBlockedException(host: String) : UnknownHostException("$host is blocked by the DNS provider")

/**
 * Minimal RFC 8484 DNS-over-HTTPS client used by [LocalProxyManager] for upstream connections.
 * The DoH server's own hostname is resolved by the system resolver (bootstrap).
 */
object DohResolver {
    private const val TAG = "DohResolver"
    private const val TIMEOUT_MS = 4000
    private const val MIN_TTL_S = 30L
    private const val MAX_TTL_S = 3600L
    private const val TYPE_A = 1
    private const val TYPE_AAAA = 28

    @Volatile var enabled = true
        private set
    @Volatile var endpoint = DnsSettings.providers.first().url
        private set
    @Volatile var fallbackToSystem = true
    @Volatile var lastFallbackAt = 0L
        private set

    private class Entry(val addresses: List<InetAddress>, val expiresAt: Long)
    private val cache = ConcurrentHashMap<String, Entry>()

    fun configure(prefs: SharedPreferences) {
        enabled = DnsSettings.enabled(prefs)
        endpoint = DnsSettings.endpoint(prefs)
        fallbackToSystem = DnsSettings.fallback(prefs)
        cache.clear()
        Log.d(TAG, "DNS ${if (enabled) "DoH via $endpoint" else "system resolver"}")
    }

    fun clearCache() = cache.clear()

    /** Resolves [host] with the configured resolver, honouring the system-DNS fallback setting. */
    fun resolve(host: String): List<InetAddress> {
        if (!enabled || isIpLiteral(host)) return InetAddress.getAllByName(host).toList()
        cache[host]?.let { if (it.expiresAt > System.currentTimeMillis()) return checkBlocked(host, it.addresses) }
        return try {
            checkBlocked(host, lookup(endpoint, host))
        } catch (e: DnsBlockedException) {
            throw e
        } catch (e: NxDomainException) {
            throw e
        } catch (e: Exception) {
            if (!fallbackToSystem) throw UnknownHostException("DoH lookup for $host failed: ${e.message}")
            lastFallbackAt = System.currentTimeMillis()
            Log.w(TAG, "DoH lookup for $host failed (${e.message}), using system DNS")
            InetAddress.getAllByName(host).toList()
        }
    }

    /** Queries [server] directly (no cache, no fallback). Used by the settings "Test" action. */
    fun lookup(server: String, host: String): List<InetAddress> {
        var (addrs, ttl) = query(server, host, TYPE_A)
        if (addrs.isEmpty()) {
            val v6 = query(server, host, TYPE_AAAA)
            addrs = v6.first
            ttl = v6.second
        }
        if (addrs.isEmpty()) throw UnknownHostException("No DNS records for $host")
        val ttlS = ttl.coerceIn(MIN_TTL_S, MAX_TTL_S)
        cache[host] = Entry(addrs, System.currentTimeMillis() + ttlS * 1000)
        return addrs
    }

    private fun checkBlocked(host: String, addrs: List<InetAddress>): List<InetAddress> {
        if (addrs.all { it.isAnyLocalAddress }) throw DnsBlockedException(host)
        return addrs
    }

    private fun isIpLiteral(host: String) =
        host.contains(':') || host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$"))

    private fun query(server: String, host: String, type: Int): Pair<List<InetAddress>, Long> {
        val msg = Base64.encodeToString(buildQuery(host, type), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val sep = if (server.contains('?')) '&' else '?'
        val conn = URL("$server${sep}dns=$msg").openConnection() as HttpsURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("Accept", "application/dns-message")
            val code = conn.responseCode
            if (code != 200) throw java.io.IOException("DoH server returned HTTP $code")
            val body = conn.inputStream.use { it.readBytes() }
            return parseResponse(host, body, type)
        } finally {
            conn.disconnect()
        }
    }

    private fun buildQuery(host: String, type: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0))
        for (label in host.trimEnd('.').split('.')) {
            val b = label.toByteArray(Charsets.US_ASCII)
            require(b.size in 1..63) { "Invalid hostname: $host" }
            out.write(b.size)
            out.write(b)
        }
        out.write(0)
        out.write(type shr 8); out.write(type and 0xFF)
        out.write(0); out.write(1)
        return out.toByteArray()
    }

    private fun parseResponse(host: String, buf: ByteArray, type: Int): Pair<List<InetAddress>, Long> {
        fun u8(i: Int) = buf[i].toInt() and 0xFF
        fun u16(i: Int) = (u8(i) shl 8) or u8(i + 1)
        fun u32(i: Int) = (u16(i).toLong() shl 16) or u16(i + 2).toLong()
        fun skipName(start: Int): Int {
            var i = start
            while (true) {
                val len = u8(i)
                when {
                    len == 0 -> return i + 1
                    len and 0xC0 == 0xC0 -> return i + 2
                    else -> i += len + 1
                }
            }
        }
        if (buf.size < 12) throw java.io.IOException("Short DNS response")
        val rcode = u8(3) and 0x0F
        if (rcode == 3) throw NxDomainException(host)
        if (rcode != 0) throw java.io.IOException("DNS error rcode=$rcode")
        val qd = u16(4)
        val an = u16(6)
        var i = 12
        repeat(qd) { i = skipName(i) + 4 }
        val addrs = mutableListOf<InetAddress>()
        var minTtl = MAX_TTL_S
        repeat(an) {
            i = skipName(i)
            val rType = u16(i)
            val ttl = u32(i + 4)
            val len = u16(i + 8)
            val data = i + 10
            if (rType == type && ((type == TYPE_A && len == 4) || (type == TYPE_AAAA && len == 16))) {
                val addr = InetAddress.getByAddress(buf.copyOfRange(data, data + len))
                if (addr is Inet4Address || addr is Inet6Address) addrs += addr
                minTtl = minOf(minTtl, ttl)
            }
            i = data + len
        }
        return addrs to minTtl
    }
}

/** Local proxy settings. */
object ProxySettings {
    const val ENABLED = "ProxyEnabled"
    const val PORT = "ProxyPort"
    const val REUSE = "ProxyReuse"

    fun enabled(prefs: SharedPreferences) = prefs.getBoolean(ENABLED, true)
    /** 0 means "pick a free port automatically". */
    fun port(prefs: SharedPreferences) = prefs.getInt(PORT, 0)
    fun reuseConnections(prefs: SharedPreferences) = prefs.getBoolean(REUSE, true)
}

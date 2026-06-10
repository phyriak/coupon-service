package com.example.coupon.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

//remoteAddr = informacja z połączenia TCP
//X-Forwarded-For = informacja z nagłówka HTTP
//prefer remote address or take xff if is aligned with trusted ip policy
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    // trusted-proxy CIDRs + enabled flag
    private final IpResolutionProperties properties;
    /**
     * Resolves real client IP, accounting for reverse proxies via X-Forwarded-For.
     * Takes the first entry (original client), not the last (proxy).
     */
    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
            if (!properties.trustForwardedHeaders() || !isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        // Only honor XFF if the direct peer is one of OUR proxies
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isEmpty()) {
            return request.getRemoteAddr();
        }

        // Walk from the RIGHT, skipping our own proxies; the first
        // untrusted hop is the real client. Never trust the leftmost
        // value blindly — that's the part the attacker controls.
        String[] hops = xff.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String candidate = hops[i].trim();
            if (!isTrustedProxy(candidate)) {
                return isIpLiteral(candidate) ? candidate : remoteAddr;
            }
        }
        return remoteAddr;
    }


    private boolean isIpLiteral(String value) {
        // InetAddresses.isInetAddress from Guava, or a regex/IPAddress lib —
        // the point is: never pass a hostname to InetAddress.getByName,
        // which would do a blocking DNS lookup on attacker input
        return InetAddressValidator.getInstance().isValid(value); // commons-validator
    }


    private boolean isTrustedProxy(String ip) {
        return properties.trustedProxies().stream()
                .anyMatch(cidr -> new IpAddressMatcher(cidr).matches(ip));
    }
}

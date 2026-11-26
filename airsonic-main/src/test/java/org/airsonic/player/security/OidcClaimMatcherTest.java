package org.airsonic.player.security;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OidcClaimMatcherTest {

    private static Map<String, Object> claims(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void blankClaimDisablesTheGate() {
        assertTrue(OidcClaimMatcher.matches(claims(), null, "x"));
        assertTrue(OidcClaimMatcher.matches(claims(), "", "x"));
        assertTrue(OidcClaimMatcher.matches(claims(), "  ", "x"));
        assertTrue(OidcClaimMatcher.matches(null, "", ""));
    }

    @Test
    void arrayContainingValueIsAllowed() {
        assertTrue(OidcClaimMatcher.matches(claims("groups", List.of("a", "airsonic-users")), "groups", "airsonic-users"));
        assertTrue(OidcClaimMatcher.matches(claims("groups", new String[] { "a", "airsonic-users" }), "groups", "airsonic-users"));
    }

    @Test
    void arrayMissingValueIsRejected() {
        assertFalse(OidcClaimMatcher.matches(claims("groups", List.of("a", "b")), "groups", "airsonic-users"));
        assertFalse(OidcClaimMatcher.matches(claims("groups", List.of()), "groups", "airsonic-users"));
    }

    @Test
    void stringEqualOrSpaceSeparatedTokenIsAllowed() {
        assertTrue(OidcClaimMatcher.matches(claims("roles", "airsonic-users"), "roles", "airsonic-users"));
        assertTrue(OidcClaimMatcher.matches(claims("scope", "openid profile airsonic-users"), "scope", "airsonic-users"));
    }

    @Test
    void partialStringMatchMustNotPass() {
        assertFalse(OidcClaimMatcher.matches(claims("roles", "administrators"), "roles", "admin"));
        assertFalse(OidcClaimMatcher.matches(claims("roles", "xadmin admins"), "roles", "admin"));
        assertFalse(OidcClaimMatcher.matches(claims("groups", List.of("administrators")), "groups", "admin"));
    }

    @Test
    void comparisonIsCaseSensitive() {
        assertFalse(OidcClaimMatcher.matches(claims("groups", List.of("Airsonic-Users")), "groups", "airsonic-users"));
        assertFalse(OidcClaimMatcher.matches(claims("roles", "Airsonic-Users"), "roles", "airsonic-users"));
    }

    @Test
    void nestedDotPathIsResolved() {
        Map<String, Object> c = claims("realm_access", Map.of("roles", List.of("offline_access", "airsonic-users")));
        assertTrue(OidcClaimMatcher.matches(c, "realm_access.roles", "airsonic-users"));
        assertFalse(OidcClaimMatcher.matches(c, "realm_access.roles", "other"));
        assertFalse(OidcClaimMatcher.matches(c, "realm_access.missing", "airsonic-users"));
        assertFalse(OidcClaimMatcher.matches(c, "other.roles", "airsonic-users"));
    }

    @Test
    void literalClaimNameWithDotsIsPreferred() {
        Map<String, Object> c = claims("https://example.com/roles", List.of("airsonic-users"));
        assertTrue(OidcClaimMatcher.matches(c, "https://example.com/roles", "airsonic-users"));
    }

    @Test
    void absentClaimIsRejected() {
        assertFalse(OidcClaimMatcher.matches(claims(), "groups", "airsonic-users"));
        assertFalse(OidcClaimMatcher.matches(null, "groups", "airsonic-users"));
        assertFalse(OidcClaimMatcher.matches(claims("groups", null), "groups", "airsonic-users"));
    }

    @Test
    void blankValueRequiresPresenceOnly() {
        assertTrue(OidcClaimMatcher.matches(claims("groups", List.of("a")), "groups", ""));
        assertTrue(OidcClaimMatcher.matches(claims("groups", List.of("a")), "groups", null));
        assertTrue(OidcClaimMatcher.matches(claims("roles", "x"), "roles", ""));
        // present but empty fails closed
        assertFalse(OidcClaimMatcher.matches(claims("groups", List.of()), "groups", ""));
        assertFalse(OidcClaimMatcher.matches(claims("roles", ""), "roles", ""));
        assertFalse(OidcClaimMatcher.matches(claims("roles", "   "), "roles", ""));
        assertFalse(OidcClaimMatcher.matches(claims(), "groups", ""));
    }

    @Test
    void unsupportedTypesAreRejected() {
        assertFalse(OidcClaimMatcher.matches(claims("groups", 42), "groups", "42"));
        assertFalse(OidcClaimMatcher.matches(claims("groups", true), "groups", "true"));
        assertFalse(OidcClaimMatcher.matches(claims("groups", Map.of("a", "b")), "groups", "a"));
        assertFalse(OidcClaimMatcher.matches(claims("groups", List.of(1, 2)), "groups", "1"));
        assertFalse(OidcClaimMatcher.matches(claims("groups", 42), "groups", ""));
    }
}

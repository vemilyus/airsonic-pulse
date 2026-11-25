package org.airsonic.player.security;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * Evaluates the OIDC required-claim access gate.
 * <p>
 * The claim is looked up by its name (dot-paths such as {@code realm_access.roles} traverse nested
 * claims; a claim whose own name contains dots is matched first as a literal key). The claim value may
 * be a collection/array (any element equals the required value), a string (equals, or any
 * whitespace separated token equals). Comparison is exact and case-sensitive. A configured gate fails
 * closed: a missing claim, an empty value or an unsupported claim type never matches.
 */
public final class OidcClaimMatcher {

    private OidcClaimMatcher() {
    }

    /**
     * @param claims        the merged ID token and UserInfo claims
     * @param claimPath     the claim name or dot-path; blank disables the gate (always matches)
     * @param requiredValue the value the claim must contain; blank means the claim must merely be
     *                      present and non-empty
     * @return true if the gate is passed
     */
    public static boolean matches(Map<String, Object> claims, String claimPath, String requiredValue) {
        if (StringUtils.isBlank(claimPath)) {
            return true;
        }
        if (claims == null) {
            return false;
        }

        Object claim = resolve(claims, claimPath.trim());
        if (claim == null) {
            return false;
        }

        boolean valueRequired = StringUtils.isNotEmpty(requiredValue);

        if (claim instanceof CharSequence cs) {
            String s = cs.toString();
            if (!valueRequired) {
                return StringUtils.isNotBlank(s);
            }
            if (s.equals(requiredValue)) {
                return true;
            }
            for (String token : s.trim().split("\\s+")) {
                if (token.equals(requiredValue)) {
                    return true;
                }
            }
            return false;
        }

        if (claim instanceof Collection<?> collection) {
            return anyMatches(collection.toArray(), valueRequired, requiredValue);
        }

        if (claim.getClass().isArray() && !claim.getClass().getComponentType().isPrimitive()) {
            int length = Array.getLength(claim);
            Object[] elements = new Object[length];
            for (int i = 0; i < length; i++) {
                elements[i] = Array.get(claim, i);
            }
            return anyMatches(elements, valueRequired, requiredValue);
        }

        // numbers, booleans, maps and other types are not supported
        return false;
    }

    private static boolean anyMatches(Object[] elements, boolean valueRequired, String requiredValue) {
        for (Object element : elements) {
            if (element instanceof CharSequence cs) {
                String s = cs.toString();
                if (valueRequired ? s.equals(requiredValue) : StringUtils.isNotBlank(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Object resolve(Map<String, Object> claims, String path) {
        if (claims.containsKey(path)) {
            return claims.get(path);
        }
        Object current = claims;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }
}

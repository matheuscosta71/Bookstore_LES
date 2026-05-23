package com.matheusgn.ecommerce.audit.support;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuditActorResolver {

    private static final String ATTR_AUDIT_ACTOR = "com.matheusgn.audit.actor";

    public String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servlet) {
            String header = servlet.getRequest().getHeader("X-Audit-Actor");
            if (header != null && !header.isBlank()) {
                return header.trim();
            }
        }
        return "SYSTEM";
    }

    public static void setActorForRequest(String actor) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servlet) {
            servlet.setAttribute(ATTR_AUDIT_ACTOR, actor, RequestAttributes.SCOPE_REQUEST);
        }
    }
}

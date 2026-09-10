package sigurdws.NB69.house;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded, per-process throttling. Deploy one backend instance or add a shared limiter. */
final class AuthenticationThrottle extends OncePerRequestFilter {
    private record Attempts(long until, int count) {}
    private final Map<String, Attempts> attempts = new HashMap<>();
    private synchronized boolean allow(String key, int limit) {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> entry.getValue().until() <= now);
        if (!attempts.containsKey(key) && attempts.size() >= 10000) return false;
        Attempts old = attempts.getOrDefault(key, new Attempts(now + 15 * 60 * 1000, 0));
        if (old.count() >= limit) return false;
        attempts.put(key, new Attempts(old.until(), old.count()+1)); return true;
    }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        if (req.getMethod().equals("POST") && (req.getServletPath().equals("/api/login") || req.getServletPath().equals("/api/activate"))) {
            boolean permitted = allow("ip:" + req.getRemoteAddr(), 60);
            if (req.getServletPath().equals("/api/login")) permitted &= allow("user:" + Objects.toString(req.getParameter("username"), "").toLowerCase(Locale.ROOT), 15);
            if (!permitted) {
                res.setStatus(429); res.setHeader("Retry-After", "900"); res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"message\":\"For mange forsøk. Vent 15 minutter før du prøver igjen.\"}"); return;
            }
        }
        chain.doFilter(req,res);
    }
}

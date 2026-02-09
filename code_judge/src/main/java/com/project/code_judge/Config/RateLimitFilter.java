package com.project.code_judge.Config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BandwidthBuilder;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {
    private final Map<String,Bucket> bucketCache = new ConcurrentHashMap<>();


    private Bucket resolveBucket(String key){
        return bucketCache.computeIfAbsent(key, k -> createNewBucket());
    }
    private Bucket createNewBucket(){
        Bandwidth limit = BandwidthBuilder.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getRequestURI();
        boolean isRateLimited = path.startsWith("/api/submissions");
        if(!isRateLimited){
            filterChain.doFilter(request, response);
            return;
        }
        String client = request.getRemoteAddr();
        Bucket bucket = resolveBucket(client);

        response.addHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
        if(bucket.tryConsume(1)){
            filterChain.doFilter(request, response);
        }else{
            response.setStatus(429);
            response.getWriter().write("Too many requests");
        }
    }
}

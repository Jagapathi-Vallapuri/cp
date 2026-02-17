package com.project.code_judge.Config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitFilter implements Filter {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private RedisClient redisClient;
    private StatefulRedisConnection<String, byte[]> connection;
    private LettuceBasedProxyManager<String> proxyManager;

    @PostConstruct
    public void init(){
        redisClient = RedisClient.create("redis://" + redisHost + ":" + redisPort);
        connection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(1)))
                .build();
    }

    @PreDestroy
    public void cleanUp(){
        if(connection != null) connection.close();
        if(redisClient != null) redisClient.shutdown();
    }

    private Bucket resolveBucket(String key){
        BucketConfiguration config = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofMinutes(1))
                        .build())
                .build();
        return proxyManager.builder().build(key, config);
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
        String bucketKey = "rate_limit:" + client;
        Bucket bucket = resolveBucket(bucketKey);

        var probe = bucket.tryConsumeAndReturnRemaining(1);


        if(bucket.tryConsume(1)){
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        }else{
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.setStatus(429);
            response.getWriter().write("Too many requests. Please wait " + waitForRefill + " seconds.");
        }
    }
}

package com.project.code_judge.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 172800)
public class RedisConfig {

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer(){
        return new JacksonJsonRedisSerializer<>(Object.class);
    }
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory){
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<Object>(Object.class));

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<Object>(Object.class));
        template.afterPropertiesSet();
        return template;
    }
}

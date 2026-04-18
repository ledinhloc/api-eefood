package com.eefood.reactionservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
@EnableScheduling
public class RedisConfig {
  @Bean
  public RedisCacheConfiguration cacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofMinutes(30)) // TTL 30 phút
      .disableCachingNullValues()
      .serializeValuesWith(
        RedisSerializationContext.SerializationPair.fromSerializer(
          new GenericJackson2JsonRedisSerializer()
        )
      );
  }

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration defaultConfig = cacheConfiguration();
    // Tách TTL theo từng loại cache để poll state/result hết hạn phù hợp hơn.
    Map<String, RedisCacheConfiguration> configs = Map.of(
            "rag-embeddings",
            defaultConfig
                    .entryTtl(Duration.ofHours(6)),
            "poll-vote-metadata",
            defaultConfig
                    .entryTtl(Duration.ofMinutes(10)),
            "poll-results",
            defaultConfig
                    .entryTtl(Duration.ofMinutes(10)),
            "poll-user-vote-state",
            defaultConfig
                    .entryTtl(Duration.ofHours(6))
    );

    return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(configs)
            .build();
  }
}

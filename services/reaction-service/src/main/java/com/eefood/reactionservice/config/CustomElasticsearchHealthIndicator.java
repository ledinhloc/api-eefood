package com.eefood.reactionservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("elasticsearch")
@RequiredArgsConstructor
@Slf4j
public class CustomElasticsearchHealthIndicator implements HealthIndicator {

  private final ElasticsearchClient elasticsearchClient;

  @Override
  public Health health() {
    log.info("Running custom Elasticsearch health check...");
    try {
      var response = elasticsearchClient.info();
      return Health.up()
        .withDetail("cluster_name", response.clusterName())
        .withDetail("version", response.version().number())
        .build();
    } catch (Exception e) {
      log.error("Elasticsearch health check failed", e);
      return Health.down()
        .withDetail("error", e.getMessage())
        .build();
    }
  }
}
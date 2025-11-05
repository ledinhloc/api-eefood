package com.eefood.reactionservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

  @Value("${elasticsearch.url}")
  private String ELASTIC_URL;
  @Value("${elasticsearch.api-key}")
  private String API_KEY; // thay bằng API KEY thật

  @Bean
  public ElasticsearchClient elasticsearchClient() {
    RestClient restClient = RestClient.builder(
        new HttpHost(ELASTIC_URL, 443, "https"))
      .setDefaultHeaders(new BasicHeader[]{
        new BasicHeader("Authorization", "ApiKey " + API_KEY)
      })
      .build();

    // Tạo ObjectMapper với JavaT
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    return new ElasticsearchClient(transport);
  }
}

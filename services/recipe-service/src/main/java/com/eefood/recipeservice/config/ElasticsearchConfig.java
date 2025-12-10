package com.eefood.recipeservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

  @Value("${elasticsearch.host}")
  private String elasticHost;

  @Value("${elasticsearch.port}")
  private int elasticPort;

  @Value("${elasticsearch.username}")
  private String username;

  @Value("${elasticsearch.password}")
  private String password;

  @Bean
  public ElasticsearchClient elasticsearchClient() {

    final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(
      AuthScope.ANY,
      new UsernamePasswordCredentials(username, password)
    );

    RestClient restClient = RestClient.builder(
        new HttpHost(elasticHost, elasticPort, "http")
    ).setHttpClientConfigCallback(httpClientBuilder ->
        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
      .build();

    // Tạo ObjectMapper với JavaT
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    return new ElasticsearchClient(transport);
  }
}

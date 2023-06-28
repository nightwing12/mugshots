/*
 *
 * Copyright 2017 Uncharted Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.uncharted.mugshots.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uncharted.mugshots.model.Mugshot;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.IndexTemplatesExistRequest;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Data
public class ElasticsearchService {

    private final ObjectMapper mapper;

    private ElasticsearchClient client = null;

    private RestHighLevelClient restHighLevelClient = null;

    @Value("classpath:index-templates/*.json")
    private Resource[] resourceIndexTemplates;

    @PostConstruct
    public void init() {
        RestClientBuilder httpClientBuilder = RestClient.builder(
                new HttpHost("localhost", 9200)
        );

        // Create the HLRC
        restHighLevelClient = new RestHighLevelClient(httpClientBuilder);

        // Create the new Java Client with the same low level client
        ElasticsearchTransport transport = new RestClientTransport(
                restHighLevelClient.getLowLevelClient(),
                new JacksonJsonpMapper(mapper)
        );

        client = new ElasticsearchClient(transport);
        try {
            var test = client.ping();
            int x = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
        // installIndexTemplates(); //todo fix this later
    }

    private boolean containsTemplate(String name) {
        IndexTemplatesExistRequest request = new IndexTemplatesExistRequest(name);
        boolean exists = false;
        try {
            exists = restHighLevelClient.indices().existsTemplate(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            if (e.status() != RestStatus.NOT_FOUND) {
                log.error("Error checking existence of template, unexpected ElasticsearchStatusException result {}", name, e);
            }
        } catch (IOException e) {
            log.error("Error checking existence of template {}", name, e);
        }
        return exists;
    }

    private boolean putIndexTemplate(String name, String json) throws IOException {
        PutIndexTemplateRequest req = new PutIndexTemplateRequest(name);
        req.source(json, XContentType.JSON);
        AcknowledgedResponse resp = restHighLevelClient.indices().putTemplate(req, RequestOptions.DEFAULT);
        return resp.isAcknowledged();
    }

    private void installIndexTemplates() {
        for (final Resource resource : resourceIndexTemplates) {
            final String filename = resource.getFilename();
            if (filename != null) {
                final String indexTemplateName = filename.substring(0, filename.length() - 5);
                if (!containsTemplate(indexTemplateName)) {
                    final JsonNode templateJson;
                    try {
                        templateJson = mapper.readValue(resource.getInputStream(), JsonNode.class);
                        final boolean acknowledged = putIndexTemplate(indexTemplateName, templateJson.toString());
                        if (acknowledged) {
                            log.info("Added index template: {}", indexTemplateName);
                        } else {
                            log.error("Error adding index template: {}", indexTemplateName);
                        }
                    } catch (final IOException e) {
                        log.error("Error parsing index template: {}", resource.getFilename(), e);
                    }
                }
            }
        }
    }

    public BulkResponse index(final String indexName, final List<Mugshot> mugshots) throws Exception {
        if (mugshots == null || mugshots.isEmpty()) {
            return null;
        }

        BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

        mugshots.forEach(d -> {
            IndexOperation<Mugshot> operation = new IndexOperation.Builder<Mugshot>().document(d).index(indexName).id(UUID.randomUUID().toString()).build();
            bulkRequest.operations(operationBuilder -> operationBuilder.index(operation));
        });

        log.info("Writing {} documents to {}", mugshots.size(), indexName);
        final BulkResponse response = client.bulk(bulkRequest.build());
        if (response.errors()) {
            //todo handle errors
        }
        return response;
    }

}

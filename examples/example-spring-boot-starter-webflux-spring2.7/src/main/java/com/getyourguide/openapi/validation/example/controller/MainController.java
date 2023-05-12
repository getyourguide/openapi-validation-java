package com.getyourguide.openapi.validation.example.controller;

import com.getyourguide.openapi.validation.example.openapi.DefaultApi;
import com.getyourguide.openapi.validation.example.openapi.model.IndexRequest;
import com.getyourguide.openapi.validation.example.openapi.model.IndexResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class MainController implements DefaultApi {
    @Override
    public Mono<ResponseEntity<Void>> deleteIndex(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.noContent().build());
    }

    @Override
    public Mono<ResponseEntity<IndexResponse>> getIndex(String fromDate, ServerWebExchange exchange) {
        return Mono.just(new ResponseEntity<>(new IndexResponse().name(null), HttpStatus.OK));
    }

    @Override
    public Mono<ResponseEntity<IndexResponse>> postIndex(Mono<IndexRequest> indexRequest, ServerWebExchange exchange) {
        return indexRequest
            .map(body -> new ResponseEntity<>(new IndexResponse().name(body.getName()), HttpStatus.OK))
            .switchIfEmpty(Mono.just(ResponseEntity.badRequest().build()));
    }
}

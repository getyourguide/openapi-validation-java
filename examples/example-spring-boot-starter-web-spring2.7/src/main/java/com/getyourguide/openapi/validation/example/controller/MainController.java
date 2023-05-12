package com.getyourguide.openapi.validation.example.controller;

import com.getyourguide.openapi.validation.example.openapi.DefaultApi;
import com.getyourguide.openapi.validation.example.openapi.model.IndexRequest;
import com.getyourguide.openapi.validation.example.openapi.model.IndexResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController implements DefaultApi {
    @Override
    public ResponseEntity<Void> deleteIndex() {
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<IndexResponse> getIndex(String fromDate) {
        return ResponseEntity.ok(new IndexResponse().name(null));
    }

    @Override
    public ResponseEntity<IndexResponse> postIndex(IndexRequest indexRequest) {
        return ResponseEntity.ok(new IndexResponse().name(indexRequest.getName()));
    }

}

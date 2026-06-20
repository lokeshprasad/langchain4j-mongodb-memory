package com.langchain4j.memory.service;

public interface EmbeddingService {
    double[] embed(String text);
}

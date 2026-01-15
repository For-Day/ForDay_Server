package com.example.ForDay.global.ai.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

@Component
public class AiCallLimiter {

    private final Semaphore semaphore = new Semaphore(1);

    public <T> T execute(Supplier<T> task) {
        try {
            semaphore.acquire();
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            semaphore.release();
        }
    }
}

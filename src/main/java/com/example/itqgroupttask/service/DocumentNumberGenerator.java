package com.example.itqgroupttask.service;

import java.util.UUID;

public final class DocumentNumberGenerator {
    private DocumentNumberGenerator() {
    }

    public static String nextNumber() {
        return "DOC-" + UUID.randomUUID();
    }
}


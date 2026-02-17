package com.example.itqgroupttask.exception;

public class RegistryException extends ApiException {
    public RegistryException(String message) {
        super("REGISTRY_ERROR", message);
    }
}


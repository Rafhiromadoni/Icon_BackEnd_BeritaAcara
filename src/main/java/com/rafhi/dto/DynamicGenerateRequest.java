package com.rafhi.dto;
import java.util.Map;

// DTO BARU untuk request generasi dokumen
// Buat file: src/main/java/com/rafhi/dto/DynamicGenerateRequest.java
public class DynamicGenerateRequest {
    public Long templateId;
    public Map<String, String> data; // Key: "${placeholder}", Value: "isian pengguna"
}
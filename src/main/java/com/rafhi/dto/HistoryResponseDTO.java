package com.rafhi.dto;

import java.time.LocalDateTime;

public class HistoryResponseDTO {
    public Long id;
    public String nomorBA;
    public String jenisBeritaAcara;
    public String judulPekerjaan;
    public LocalDateTime generationTimestamp;

    // Constructor default (tanpa argumen) sudah cukup untuk Panache .project()
    public HistoryResponseDTO() {
    }

    // Constructor dengan argumen boleh tetap ada, tidak akan mengganggu.
    public HistoryResponseDTO(Long id, String nomorBA, String jenisBeritaAcara, String judulPekerjaan, LocalDateTime generationTimestamp) {
        this.id = id;
        this.nomorBA = nomorBA;
        this.jenisBeritaAcara = jenisBeritaAcara;
        this.judulPekerjaan = judulPekerjaan;
        this.generationTimestamp = generationTimestamp;
    }
}
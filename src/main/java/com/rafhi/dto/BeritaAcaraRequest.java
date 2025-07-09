// src/main/java/com/rafhi/dto/BeritaAcaraRequest.java
package com.rafhi.dto;

import java.util.List;

public class BeritaAcaraRequest {
    // Info Umum
    public String jenisBeritaAcara; // UAT atau Deployment
    public String kategoriAplikasi;
    public String tipeRequest;
    public String nomorBA;
    public String tahun;
    public String judulPekerjaan;

    // Info Rujukan
    public String nomorSuratRequest;
    public String tanggalSuratRequest;
    public String nomorBaUat;           // Opsional: Untuk referensi di BA Deployment

    // Info Tambahan
    public String namaAplikasiSpesifik; // Opsional: Untuk judul 2 baris
    public String tahap;                // Opsional: ex: "tahap I"
    public String tanggalPelaksanaan;
    
    // Konten Dinamis
    public List<Fitur> fiturList;
    public List<Signatory> signatoryList;
}
package com.rafhi.dto;

import java.util.List;

public class BeritaAcaraRequest {
    
    public Long templateId; // ID dari Template yang digunakan
    
    // Informasi untuk pembuatan Berita Acara
    // Informasi untuk logika dan judul
    public String jenisBeritaAcara; // "UAT" atau "Deployment"
    public String jenisRequest; // "Change Request" atau "Job Request"
    public String namaAplikasiSpesifik;
    public String judulPekerjaan;
    public String tahap; // "tahap I", "tahap II", dll.

    // Informasi Nomor
    public String nomorBA;
    public String nomorSuratRequest;
    public String nomorBaUat; // Khusus untuk Deployment

    // Informasi Tanggal (format: "yyyy-MM-dd")
    public String tanggalBA;
    public String tanggalSuratRequest;
    public String tanggalPengerjaan;
    
    // Daftar dinamis
    public List<Fitur> fiturList;
    public List<Signatory> signatoryList;
    
}
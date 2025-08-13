package com.rafhi.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class BeritaAcaraHistory extends PanacheEntity {

    public String nomorBA;
    public String jenisBeritaAcara;
    public String judulPekerjaan;
    public LocalDateTime generationTimestamp;


    // MODIFIKASI: Ganti 'String username' dengan relasi ke AppUser
    // Asumsi Anda punya entity AppUser. Jika tidak, tetap gunakan String username
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Kolom foreign key di DB
    public AppUser user;

    // public String username; // Menyimpan username yang membuat berita acara

    @Column(columnDefinition = "TEXT")
    public String requestJson; // Menyimpan seluruh data request sebagai JSON

    @Lob
    public byte[] fileContent; // Menyimpan file .docx sebagai blob

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    public Template template;

}
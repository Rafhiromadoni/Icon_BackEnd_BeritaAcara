package com.rafhi.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "templates")
public class Template extends PanacheEntity {

    @Column(nullable = false)
    public String templateName; // Nama yang mudah dibaca, mis: "BAST 4 Penandatangan"

    @Column(nullable = false, unique = true)
    public String fileNameStored; // Nama file unik di storage, mis: "uuid-asli.docx"

    @Column(nullable = false)
    public String originalFileName; // Nama file asli saat diunggah

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(nullable = false)
    public boolean isActive = true; // Untuk menonaktifkan template

    @CreationTimestamp
    public Instant createdAt;

    @UpdateTimestamp
    public Instant updatedAt;
}
package com.rafhi.entity;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "templates")
public class Template extends PanacheEntity {

    @Column(nullable = false)
    public String templateName;

    @Column(nullable = false, unique = true)
    public String fileNameStored;

    @Column(nullable = false)
    public String originalFileName;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(nullable = false)
    public boolean isActive = true;

    @CreationTimestamp
    public Instant createdAt;

    @UpdateTimestamp
    public Instant updatedAt;

    @OneToMany(
        mappedBy = "template",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @JsonManagedReference // <-- TAMBAHKAN ANOTASI INI
    private List<TemplatePlaceholder> placeholders;

    // <-- PERBAIKAN: Tambahkan Getter dan Setter di bawah ini -->
    public List<TemplatePlaceholder> getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(List<TemplatePlaceholder> placeholders) {
        this.placeholders = placeholders;
    }
}
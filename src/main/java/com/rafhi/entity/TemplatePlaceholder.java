package com.rafhi.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "template_placeholders")
public class TemplatePlaceholder extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @JsonBackReference
    public Template template;

    @Column(nullable = false)
    public String placeholderKey; // e.g., "${nomor_surat_dinas}"

    @Column(nullable = false)
    public String label; // e.g., "Nomor Surat Dinas"

    @Column(nullable = false)
    public String dataType; // "TEXT", "DATE", "RICH_TEXT"

    public boolean isRequired = true;
}
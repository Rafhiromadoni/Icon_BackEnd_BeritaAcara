package com.rafhi.dto;

import java.util.List;

import com.rafhi.entity.TemplatePlaceholder;

public class DefineTemplateRequest {
    
    public String templateName;
    public String description;
    public String tempFilePath;
    public String originalFileName;
    public List<TemplatePlaceholder> placeholders;

    // <-- PERBAIKAN: Tambahkan field ini untuk menerima data dari checkbox -->
    public boolean isActive;
}
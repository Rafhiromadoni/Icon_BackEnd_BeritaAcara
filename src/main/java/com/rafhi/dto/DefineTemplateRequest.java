package com.rafhi.dto;
import com.rafhi.entity.TemplatePlaceholder;
import java.util.List;

// DTO Baru untuk request definisi metadata
public class DefineTemplateRequest {
    public String templateName;
    public String description;
    public String tempFilePath;
    public String originalFileName;
    public List<TemplatePlaceholder> placeholders; // Frontend mengirimkan list ini
}
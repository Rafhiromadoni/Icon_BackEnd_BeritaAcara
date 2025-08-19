package com.rafhi.service;

import com.rafhi.entity.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.apache.poi.xwpf.usermodel.*;
import org.hibernate.Hibernate; // <-- PERUBAHAN: Import ditambahkan

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class TemplateService {

    @ConfigProperty(name = "template.upload.path")
    String uploadPath;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public List<Template> listAllActive() {
        return Template.list("isActive", true);
    }

    public List<Template> listAllForAdmin() {
        return Template.listAll();
    }

    public Template findById(Long id) {
        return Template.findById(id);
    }

    // <-- PERUBAHAN: Method baru ditambahkan di sini -->
    @Transactional
    public Template findByIdWithPlaceholders(Long id) {
        Template template = findById(id);
        if (template != null) {
            // Perintah ini memaksa Hibernate untuk memuat list placeholders
            // dari database sebelum sesi transaksi berakhir.
            Hibernate.initialize(template.getPlaceholders());
        }
        return template;
    }
    // <-- Akhir dari method baru -->

    public Path getTemplatePath(Template template) {
        return Paths.get(uploadPath, template.fileNameStored);
    }

    public Set<String> scanPlaceholders(Path filePath) throws IOException {
        Set<String> placeholders = new HashSet<>();
        try (InputStream is = Files.newInputStream(filePath); XWPFDocument document = new XWPFDocument(is)) {
            for (XWPFParagraph p : document.getParagraphs()) {
                findPlaceholdersInText(p.getText(), placeholders);
            }
            for (XWPFTable tbl : document.getTables()) {
                for (XWPFTableRow row : tbl.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            findPlaceholdersInText(p.getText(), placeholders);
                        }
                    }
                }
            }
        }
        return placeholders;
    }

    private void findPlaceholdersInText(String text, Set<String> placeholders) {
        if (text == null || text.isEmpty()) return;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            placeholders.add(matcher.group(0));
        }
    }

    @Transactional
    public void delete(Long id) throws IOException {
        Template template = findById(id);
        if (template == null) throw new NotFoundException("Template dengan ID " + id + " tidak ditemukan.");
        Files.deleteIfExists(getTemplatePath(template));
        template.delete();
    }
}
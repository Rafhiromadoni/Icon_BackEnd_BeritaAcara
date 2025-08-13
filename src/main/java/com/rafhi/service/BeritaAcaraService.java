package com.rafhi.service;

import com.rafhi.entity.AppUser;
import com.rafhi.entity.BeritaAcaraHistory;
import com.rafhi.entity.Template;
import com.rafhi.entity.TemplatePlaceholder;
import com.rafhi.helper.DateToWordsHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class BeritaAcaraService {

    @Inject TemplateService templateService;
    @Inject Jsonb jsonb;

    @Transactional
    public byte[] generateDocument(Long templateId, Map<String, String> requestData) throws Exception {
        Template template = templateService.findById(templateId);
        if (template == null) throw new NotFoundException("Template tidak ditemukan.");

        List<TemplatePlaceholder> placeholderDefinitions = TemplatePlaceholder.list("template.id", templateId);
        Map<String, TemplatePlaceholder> definitionMap = placeholderDefinitions.stream()
                .collect(Collectors.toMap(p -> p.placeholderKey, Function.identity()));

        Map<String, String> textReplacements = new HashMap<>();
        Map<String, String> htmlReplacements = new HashMap<>();

        for (Map.Entry<String, String> entry : requestData.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            TemplatePlaceholder definition = definitionMap.get(key);
            if (definition == null) continue;

            if ("DATE".equalsIgnoreCase(definition.dataType) && value != null && !value.isEmpty()) {
                expandDatePlaceholders(key, value, textReplacements);
            } else if ("RICH_TEXT".equalsIgnoreCase(definition.dataType)) {
                htmlReplacements.put(key, value);
            } else {
                textReplacements.put(key, value);
            }
        }

        Path templatePath = templateService.getTemplatePath(template);
        try (InputStream templateInputStream = Files.newInputStream(templatePath);
             XWPFDocument document = new XWPFDocument(templateInputStream)) {
            replacePlaceholders(document, textReplacements);
            for (Map.Entry<String, String> htmlEntry : htmlReplacements.entrySet()) {
                replacePlaceholderWithHtml(document, htmlEntry.getKey(), htmlEntry.getValue());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }

    @Transactional
    public BeritaAcaraHistory saveHistory(Long templateId, AppUser user, Map<String, String> requestData, byte[] fileContent) {
        Template template = templateService.findById(templateId);
        if (template == null) throw new NotFoundException("Template tidak ditemukan saat menyimpan riwayat.");

        BeritaAcaraHistory history = new BeritaAcaraHistory();
        history.user = user;
        history.template = template;
        history.generationTimestamp = LocalDateTime.now();
        history.fileContent = fileContent;
        history.requestJson = jsonb.toJson(requestData);

        // Ambil data penting untuk kolom shortcut. Sesuaikan key jika perlu.
        history.nomorBA = requestData.getOrDefault("${nomor_ba}", "N/A"); 
        history.judulPekerjaan = requestData.getOrDefault("${judul_pekerjaan}", "Tidak Ada Judul");
        history.jenisBeritaAcara = template.templateName;

        history.persistAndFlush();
        return history;
    }

    // Ganti metode expandDatePlaceholders dengan versi baru ini
    private void expandDatePlaceholders(String basePlaceholder, String dateValue, Map<String, String> replacements) {
        DateToWordsHelper helper = new DateToWordsHelper(dateValue);

        // Dapatkan nama dasar dari placeholder, contoh: "tanggal_ba_lengkap"
        String baseNameWithSuffix = basePlaceholder.substring(2, basePlaceholder.length() - 1);
        
        // Tentukan prefix berdasarkan nama placeholder yang dikirim dari frontend
        if (baseNameWithSuffix.startsWith("tanggal_ba")) {
            // --- Mengisi placeholder sesuai template baru Anda ---
            replacements.put("${hari_ba_terbilang}", helper.getDayOfWeek());
            replacements.put("${tanggal_ba_terbilang}", helper.getDay());
            replacements.put("${bulan_ba_terbilang}", helper.getMonth());
            replacements.put("${tahun_ba_terbilang}", helper.getYear());
            replacements.put("${tanggal_ba_lengkap}", helper.getFullDate()); // Ini adalah format DD-MM-YYYY
        } 
        else if (baseNameWithSuffix.startsWith("tanggal_pengerjaan")) {
            replacements.put("${hari_pengerjaan_terbilang}", helper.getDayOfWeek());
            replacements.put("${tanggal_pengerjaan_terbilang}", helper.getDay());
            replacements.put("${bulan_pengerjaan_terbilang}", helper.getMonth());
            replacements.put("${tahun_pengerjaan_terbilang}", helper.getYear());
            replacements.put("${tanggal_pengerjaan}", helper.getFullDate());
        }
        // Jika ada jenis tanggal lain (seperti tanggal_surat_request), tambahkan blok else if di sini
        else if (baseNameWithSuffix.startsWith("tanggal_surat_request")) {
            replacements.put("${tanggal_surat_request}", helper.getFullDate());
        }
    }

    // --- METODE HELPER UNTUK MANIPULASI DOKUMEN WORD ---

    /**
     * Metode utama untuk mengganti placeholder teks biasa di seluruh dokumen.
     */
    private void replacePlaceholders(XWPFDocument document, Map<String, String> replacements) {
        // Ganti di paragraf utama
        for (XWPFParagraph p : document.getParagraphs()) {
            replaceInParagraph(p, replacements);
        }
        // Ganti di dalam tabel
        for (XWPFTable tbl : document.getTables()) {
            for (XWPFTableRow row : tbl.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        replaceInParagraph(p, replacements);
                    }
                }
            }
        }
    }

    /**
     * Logika inti penggantian placeholder yang cerdas, mampu menangani placeholder
     * yang terpecah di antara beberapa "run" (potongan teks dengan style berbeda).
     */
    private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String placeholder = entry.getKey();
            String replacement = entry.getValue();

            if (replacement == null || !paragraph.getText().contains(placeholder)) {
                continue;
            }

            List<XWPFRun> runs = paragraph.getRuns();
            if (runs.isEmpty()) continue;

            String accumulatedText = "";
            int startRun = -1;

            for (int i = 0; i < runs.size(); i++) {
                String runText = runs.get(i).getText(0);
                if (runText == null) continue;

                if (startRun == -1) {
                    if (runText.contains("$")) {
                        startRun = i;
                        accumulatedText = runText;
                    }
                } else {
                    accumulatedText += runText;
                }

                if (startRun != -1) {
                    if (accumulatedText.contains(placeholder)) {
                        int endRun = i;
                        XWPFRun styleRun = runs.get(startRun);
                        String newText = accumulatedText.replace(placeholder, replacement);
                        
                        XWPFRun firstRun = runs.get(startRun);
                        if (newText.contains("\n")) {
                            String[] lines = newText.split("\n");
                            firstRun.setText(lines[0], 0);
                            for (int k = 1; k < lines.length; k++) {
                                firstRun.addBreak();
                                firstRun.setText(lines[k]);
                            }
                        } else {
                            firstRun.setText(newText, 0);
                        }

                        // Terapkan kembali style
                        if(styleRun.isBold()) firstRun.setBold(styleRun.isBold());
                        if(styleRun.isItalic()) firstRun.setItalic(styleRun.isItalic());
                        if(styleRun.getFontFamily() != null) firstRun.setFontFamily(styleRun.getFontFamily());
                        if(styleRun.getFontSize() != -1) firstRun.setFontSize(styleRun.getFontSize());
                        
                        // Kosongkan run sisa
                        for (int k = startRun + 1; k <= endRun; k++) {
                            paragraph.removeRun(startRun + 1);
                        }

                        // Ulangi dari awal karena struktur paragraf berubah
                        replaceInParagraph(paragraph, replacements);
                        return; 
                    } else if (!placeholder.startsWith(accumulatedText)) {
                        startRun = -1;
                        accumulatedText = "";
                    }
                }
            }
        }
    }


    /**
     * Metode khusus untuk mengganti placeholder dengan konten HTML dari rich text editor.
     * Ini mencari placeholder, menghapusnya, dan menyisipkan konten berformat.
     */
    private void replacePlaceholderWithHtml(XWPFDocument document, String placeholder, String html) {
        if (html == null || html.isEmpty()) return;

        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (int p = 0; p < cell.getParagraphs().size(); p++) {
                        XWPFParagraph paragraph = cell.getParagraphs().get(p);
                        if (paragraph.getText() != null && paragraph.getText().contains(placeholder)) {
                            // Simpan style dari placeholder
                            String fontFamily = null;
                            int fontSize = -1;
                            if (!paragraph.getRuns().isEmpty()) {
                                XWPFRun firstRun = paragraph.getRuns().get(0);
                                fontFamily = firstRun.getFontFamily();
                                fontSize = firstRun.getFontSize();
                            }

                            // Hapus paragraf placeholder
                            cell.removeParagraph(p);
                            p--; // Sesuaikan indeks karena ada penghapusan

                            // Parse HTML dan sisipkan konten baru
                            Document htmlDoc = Jsoup.parse(html);
                            XWPFNumbering numbering = document.getNumbering();
                            if(numbering == null) numbering = document.createNumbering();
                            
                            for (Element element : htmlDoc.body().children()) {
                                if (element.tagName().equals("p")) {
                                    XWPFParagraph targetParagraph = cell.addParagraph();
                                    targetParagraph.setSpacingAfter(60);
                                    targetParagraph.setSpacingBefore(240);
                                    targetParagraph.setSpacingBetween(1.0, LineSpacingRule.AUTO);
                                    applyRuns(targetParagraph, element, fontFamily, fontSize);
                                } else if (element.tagName().equals("ul") || element.tagName().equals("ol")) {
                                    BigInteger numId = createNumbering(numbering, element.tagName());
                                    for (Element li : element.select("li")) {
                                        XWPFParagraph listParagraph = cell.addParagraph();
                                        listParagraph.setNumID(numId);

                                        int indentLevel = 0;
                                        if (li.hasClass("ql-indent-1")) indentLevel = 1;
                                        if (li.hasClass("ql-indent-2")) indentLevel = 2;
                                        if (li.hasClass("ql-indent-3")) indentLevel = 3;
                                        
                                        listParagraph.setSpacingAfter(60);
                                        listParagraph.setSpacingBefore(60);
                                        listParagraph.setSpacingBetween(1.0, LineSpacingRule.AUTO);
                                        listParagraph.setNumILvl(BigInteger.valueOf(indentLevel));
                                        applyRuns(listParagraph, li, fontFamily, fontSize);
                                    }
                                }
                            }
                            return; 
                        }
                    }
                }
            }
        }
    }
        
    /**
     * Menerapkan style (bold, italic, dll) ke potongan teks (run)
     * berdasarkan tag HTML.
     */
    private void applyRuns(XWPFParagraph paragraph, Element element, String fontFamily, int fontSize) {
        for (Node node : element.childNodes()) {
            XWPFRun run = paragraph.createRun();
            if (fontFamily != null) run.setFontFamily(fontFamily);
            if (fontSize != -1) run.setFontSize(fontSize);

            if (node instanceof TextNode) {
                run.setText(((TextNode) node).text());
            } else if (node instanceof Element) {
                Element childElement = (Element) node;
                run.setText(childElement.text());
                if (childElement.tagName().equals("strong") || childElement.tagName().equals("b")) {
                    run.setBold(true);
                }
                if (childElement.tagName().equals("em") || childElement.tagName().equals("i")) {
                    run.setItalic(true);
                }
                if (childElement.tagName().equals("u")) {
                    run.setUnderline(UnderlinePatterns.SINGLE);
                }
            }
        }
    }
        
    /**
     * Membuat definisi numbering baru (baik bulleted atau ordered)
     * untuk digunakan dalam list.
     */
    private BigInteger createNumbering(XWPFNumbering numbering, String listType) {
        CTAbstractNum cTAbstractNum = CTAbstractNum.Factory.newInstance();
        // --- PERBAIKAN UNTUK MENDAPATKAN ID UNIK ---
        // Gunakan ukuran list abstractNum yang sudah ada, ditambah waktu saat ini
        // untuk memastikan ID tidak bertabrakan.
        long uniqueId = numbering.getAbstractNums().size() + System.currentTimeMillis() % 10000;
        cTAbstractNum.setAbstractNumId(BigInteger.valueOf(uniqueId));

        if ("ul".equals(listType)) {
            addNumberingLevel(cTAbstractNum, 0, STNumberFormat.BULLET, "•");
            addNumberingLevel(cTAbstractNum, 1, STNumberFormat.BULLET, "o");
            addNumberingLevel(cTAbstractNum, 2, STNumberFormat.BULLET, "▪");
        } else { // "ol"
            addNumberingLevel(cTAbstractNum, 0, STNumberFormat.DECIMAL, "%1.");
            addNumberingLevel(cTAbstractNum, 1, STNumberFormat.LOWER_LETTER, "%2)");
            addNumberingLevel(cTAbstractNum, 2, STNumberFormat.LOWER_ROMAN, "%3.");
        }

        XWPFAbstractNum abstractNum = new XWPFAbstractNum(cTAbstractNum, numbering);
        BigInteger abstractNumId = numbering.addAbstractNum(abstractNum);
        return numbering.addNum(abstractNumId);
    }

    /**
     * Helper untuk mendefinisikan setiap level indentasi dalam sebuah list,
     * termasuk format (angka, huruf, bullet) dan indentasinya.
     */
    private void addNumberingLevel(CTAbstractNum abstractNum, int level, STNumberFormat.Enum format, String lvlText) {
        CTLvl cTLvl = abstractNum.addNewLvl();
        cTLvl.setIlvl(BigInteger.valueOf(level));
        cTLvl.addNewNumFmt().setVal(format);
        cTLvl.addNewLvlText().setVal(lvlText);
        cTLvl.addNewStart().setVal(BigInteger.valueOf(1));

        long indentMultiplier = 720L; // 720 TWIPs = 0.5 inch
        long hangingIndent = 360L;

        cTLvl.addNewPPr().addNewInd().setLeft(BigInteger.valueOf((level + 1) * indentMultiplier));
        cTLvl.addNewPPr().addNewInd().setHanging(BigInteger.valueOf(hangingIndent));
    }
}
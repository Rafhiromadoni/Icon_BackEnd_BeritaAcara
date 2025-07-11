package com.rafhi.controller;

import com.rafhi.dto.BeritaAcaraRequest;
import com.rafhi.dto.Fitur;
import com.rafhi.dto.Signatory;
import com.rafhi.helper.DateToWordsHelper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

@Path("/berita-acara")
@Consumes("application/json")
public class BeritaAcaraResource {

    @POST
    @Path("/generate-docx")
    @Produces("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public Response generateDocx(BeritaAcaraRequest request) throws Exception {

        String templateFileName = "UAT".equalsIgnoreCase(request.jenisBeritaAcara)
                ? "template_uat.docx"
                : "template_deploy.docx";

        String templatePath = "/templates/" + templateFileName;
        InputStream templateInputStream = getClass().getResourceAsStream(templatePath);

        if (templateInputStream == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Template file not found at path: " + templatePath)
                           .build();
        }

        try (XWPFDocument document = new XWPFDocument(templateInputStream)) {
            
            Map<String, String> replacements = buildReplacementsMap(request);

            // Ganti placeholder di paragraf
            for (XWPFParagraph p : document.getParagraphs()) {
                replaceInParagraph(p, replacements);
            }

            // Ganti placeholder di tabel
            for (XWPFTable tbl : document.getTables()) {
                for (XWPFTableRow row : tbl.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            replaceInParagraph(p, replacements);
                        }
                    }
                }
            }
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);

            ResponseBuilder response = Response.ok(new ByteArrayInputStream(out.toByteArray()));
            response.header("Content-Disposition", "inline; filename=BA-" + request.nomorBA + ".docx");
            return response.build();
        }
    }

    private Map<String, String> buildReplacementsMap(BeritaAcaraRequest request) {
        Map<String, String> replacements = new HashMap<>();
        DateToWordsHelper baDate = new DateToWordsHelper(request.tanggalBA);
        DateToWordsHelper pengerjaanDate = new DateToWordsHelper(request.tanggalPengerjaan);

        Signatory utama1 = request.signatoryList.stream().filter(s -> "utama1".equals(s.tipe)).findFirst().orElse(new Signatory());
        Signatory utama2 = request.signatoryList.stream().filter(s -> "utama2".equals(s.tipe)).findFirst().orElse(new Signatory());
        Signatory mengetahui = request.signatoryList.stream().filter(s -> "mengetahui".equals(s.tipe)).findFirst().orElse(new Signatory());
        Fitur fitur = (request.fiturList != null && !request.fiturList.isEmpty()) ? request.fiturList.get(0) : new Fitur();

        replacements.put("${jenisRequest}", "Change Request".equalsIgnoreCase(request.jenisRequest) ? "PERUBAHAN" : "PENGEMBANGAN");
        replacements.put("${namaAplikasiSpesifik}", Objects.toString(request.namaAplikasiSpesifik, ""));
        replacements.put("${nomorBA}", Objects.toString(request.nomorBA, ""));
        replacements.put("${judulPekerjaan}", Objects.toString(request.judulPekerjaan, ""));
        replacements.put("${tahap}", Objects.toString(request.tahap, ""));
        replacements.put("${nomorSuratRequest}", Objects.toString(request.nomorSuratRequest, ""));
        replacements.put("${tanggalSuratRequest}", Objects.toString(request.tanggalSuratRequest, ""));
        replacements.put("${nomorBaUat}", Objects.toString(request.nomorBaUat, ""));

        replacements.put("${hariBATerbilang}", baDate.getDayOfWeek());
        replacements.put("${tanggalBATerbilang}", baDate.getDay());
        replacements.put("${bulanBATerbilang}", baDate.getMonth());
        replacements.put("${tahunBATerbilang}", baDate.getYear());
        replacements.put("${tanggalBA}", baDate.getFullDate());
            
        replacements.put("${hariPengerjaanTerbilang}", pengerjaanDate.getDayOfWeek());
        replacements.put("${tanggalPengerjaanTerbilang}", pengerjaanDate.getDay());
        replacements.put("${bulanPengerjaanTerbilang}", pengerjaanDate.getMonth());
        replacements.put("${tahunPengerjaanTerbilang}", pengerjaanDate.getYear());
        replacements.put("${tanggalPengerjaan}", pengerjaanDate.getFullDate());
        
        replacements.put("${fitur.deskripsi}", Objects.toString(fitur.deskripsi, ""));
        replacements.put("${fitur.status}", Objects.toString(fitur.status, ""));
        replacements.put("${fitur.keterangan}", Objects.toString(fitur.catatan, ""));

        replacements.put("${signatory.utama1.perusahaan}", Objects.toString(utama1.perusahaan, ""));
        replacements.put("${signatory.utama1.nama}", Objects.toString(utama1.nama, ""));
        replacements.put("${signatory.utama1.jabatan}", Objects.toString(utama1.jabatan, ""));
        replacements.put("${signatory.utama2.perusahaan}", Objects.toString(utama2.perusahaan, ""));
        replacements.put("${signatory.utama2.nama}", Objects.toString(utama2.nama, ""));
        replacements.put("${signatory.utama2.jabatan}", Objects.toString(utama2.jabatan, ""));
        replacements.put("${signatory.mengetahui.perusahaan}", Objects.toString(mengetahui.perusahaan, ""));
        replacements.put("${signatory.mengetahui.nama}", Objects.toString(mengetahui.nama, ""));
        replacements.put("${signatory.mengetahui.jabatan}", Objects.toString(mengetahui.jabatan, ""));
        
        return replacements;
    }
    
    /**
     * Metode pengganti placeholder final yang dapat menangani placeholder terpisah
     * dan menjaga style (bold, italic, etc.).
     */
    private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> replacements) {
        String paragraphText = paragraph.getText();
        if (paragraphText == null || !paragraphText.contains("$")) {
            return;
        }

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String placeholder = entry.getKey();
            if (paragraph.getText().contains(placeholder)) {
                String replacement = entry.getValue();

                List<XWPFRun> runs = paragraph.getRuns();
                for (int i = 0; i < runs.size(); i++) {
                    XWPFRun run = runs.get(i);
                    String text = run.getText(0);
                    if (text == null) continue;

                    if (text.contains(placeholder)) {
                        text = text.replace(placeholder, replacement);
                        run.setText(text, 0);
                        continue;
                    }

                    // Logika untuk menangani placeholder yang terpisah antar run
                    if (text.contains("$") && (i + 1 < runs.size())) {
                        StringBuilder placeholderBuilder = new StringBuilder(text);
                        for (int j = i + 1; j < runs.size(); j++) {
                            XWPFRun nextRun = runs.get(j);
                            String nextRunText = nextRun.getText(0);
                            if (nextRunText == null) continue;
                            placeholderBuilder.append(nextRunText);
                            if (placeholderBuilder.toString().equals(placeholder)) {
                                run.setText(replacement, 0);
                                // Hapus run yang sudah digabungkan
                                for (int k = j; k > i; k--) {
                                    paragraph.removeRun(k);
                                }
                                break;
                            }
                            if (!placeholder.startsWith(placeholderBuilder.toString())) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
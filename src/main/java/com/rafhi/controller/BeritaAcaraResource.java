package com.rafhi.controller;

import java.awt.Color;
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

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
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

    @POST
    @Path("/generate-pdf")
    @Produces("application/pdf")
    @Consumes("application/json")
    public Response generatePdf(BeritaAcaraRequest request) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document pdf = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(pdf, out);
            pdf.open();

            // 1. Header (Logo kiri-kanan + Judul Tengah)
            PdfPTable headerTable = new PdfPTable(3);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new int[]{1, 3, 1});

            // Logo kiri
            Image leftLogo = Image.getInstance(getClass().getResource("/images/pln_logo.png")); // Sesuaikan path
            leftLogo.scaleToFit(50, 50);
            PdfPCell leftCell = new PdfPCell(leftLogo);
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            headerTable.addCell(leftCell);

            // Judul tengah
            Font titleFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font subTitleFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.addElement(new Paragraph("BERITA ACARA USER ACCEPTANCE TEST (UAT)", titleFont));
            titleCell.addElement(new Paragraph("PENGEMBANGAN", titleFont));
            titleCell.addElement(new Paragraph("No. " + request.nomorBA, subTitleFont));
            headerTable.addCell(titleCell);

            // Logo kanan
            Image rightLogo = Image.getInstance(getClass().getResource("/images/iconplus_logo.png")); // Sesuaikan path
            rightLogo.scaleToFit(60, 50);
            PdfPCell rightCell = new PdfPCell(rightLogo);
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            headerTable.addCell(rightCell);

            pdf.add(headerTable);
                        // 2. Paragraf pembuka
            Font normal = new Font(Font.HELVETICA, 10);
            Font bold = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font red = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.RED);

            DateToWordsHelper baDate = new DateToWordsHelper(request.tanggalBA);
            DateToWordsHelper pengerjaanDate = new DateToWordsHelper(request.tanggalPengerjaan);

            // Paragraf pembuka dengan campuran teks biasa dan bold
            Paragraph para1 = new Paragraph();
            para1.setAlignment(Element.ALIGN_JUSTIFIED);
            para1.setFont(normal);

            para1.add("Pada hari ini " + baDate.getDayOfWeek() + " tanggal ");
            para1.add(new Chunk(baDate.getDay() + " ", bold));
            para1.add("bulan ");
            para1.add(new Chunk(baDate.getMonth() + " ", bold));
            para1.add("tahun ");
            para1.add(new Chunk(baDate.getYear() + " ", bold));
            para1.add("(" + baDate.getFullDate() + ") telah dibuat Berita Acara ");
            para1.add(new Chunk("User Acceptance Test (UAT)", bold));
            para1.add(" terhadap permohonan ");
            para1.add(new Chunk(request.jenisRequest.equalsIgnoreCase("Change Request") ? "perubahan aplikasi" : "pengembangan aplikasi", bold));
            para1.add(" merujuk pada ");
            para1.add(new Chunk("change / job request", bold));
            para1.add(" dengan nomor ");
            para1.add(new Chunk(Objects.toString(request.nomorSuratRequest, "-"), bold));
            para1.add(" perihal ");
            para1.add(new Chunk(Objects.toString(request.judulPekerjaan, "-"), bold));
            para1.add(" yang dilakukan oleh ");
            para1.add(new Chunk("Divisi Sistem dan Teknologi Informasi", bold));
            para1.add(" PT PLN (Persero).");

            pdf.add(para1);
            // 3. Tabel Fitur
            PdfPTable table = new PdfPTable(new float[]{0.5f, 4.5f, 1.5f, 2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 9);
            Font redFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.RED);

            // Header cells
            String[] headers = {"No", "Kegiatan", "Status", "Keterangan"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(Color.BLACK);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(cell);
            }

            // Baris fitur pertama (HTML -> plain text)
            Fitur fiturUtama = request.fiturList.get(0);
            table.addCell(new PdfPCell(new Phrase("1", cellFont)));

            PdfPCell kegiatanCell = new PdfPCell();
            kegiatanCell.setPhrase(new Phrase(stripHtml(fiturUtama.deskripsi), cellFont));
            table.addCell(kegiatanCell);

            PdfPCell statusCell = new PdfPCell(new Phrase(fiturUtama.status, fiturUtama.status.equalsIgnoreCase("Selesai") ? redFont : cellFont));
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(statusCell);

            PdfPCell ketCell = new PdfPCell(new Phrase(fiturUtama.catatan, cellFont));
            ketCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(ketCell);

            // Baris dummy 2
            table.addCell(new PdfPCell(new Phrase("2", cellFont)));
            table.addCell(new PdfPCell(new Phrase("Cacat fitur < 5%", cellFont)));
            table.addCell(new PdfPCell(new Phrase("OK", cellFont)));
            table.addCell(new PdfPCell(new Phrase("-", cellFont)));

            // Baris dummy 3
            table.addCell(new PdfPCell(new Phrase("3", cellFont)));
            table.addCell(new PdfPCell(new Phrase("Penyebaran Aplikasi", redFont)));
            table.addCell(new PdfPCell(new Phrase("OK", cellFont)));
            table.addCell(new PdfPCell(new Phrase("-", cellFont)));

            pdf.add(table);
            pdf.add(new Paragraph("\n\n"));

            // 4. Tabel Tanda Tangan
            PdfPTable signTable = new PdfPTable(3);
            signTable.setWidthPercentage(100f);
            signTable.setSpacingBefore(30f);
            signTable.setWidths(new float[]{1f, 1f, 1f});

            Signatory utama1 = request.signatoryList.stream().filter(s -> "utama1".equals(s.tipe)).findFirst().orElse(new Signatory());
            Signatory utama2 = request.signatoryList.stream().filter(s -> "utama2".equals(s.tipe)).findFirst().orElse(new Signatory());
            Signatory mengetahui = request.signatoryList.stream().filter(s -> "mengetahui".equals(s.tipe)).findFirst().orElse(new Signatory());

            Font signFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            // Header perusahaan
            signTable.addCell(makeCell(utama1.perusahaan, signFont, Element.ALIGN_CENTER));
            signTable.addCell(makeCell(utama2.perusahaan, signFont, Element.ALIGN_CENTER));
            signTable.addCell(makeCell("Mengetahui", signFont, Element.ALIGN_CENTER));

            // Spasi kosong tanda tangan
            signTable.addCell(makeCell("\n\n\n", signFont, Element.ALIGN_CENTER));
            signTable.addCell(makeCell("\n\n\n", signFont, Element.ALIGN_CENTER));
            signTable.addCell(makeCell("\n\n\n", signFont, Element.ALIGN_CENTER));

            // Nama
            signTable.addCell(makeCell(utama1.nama, signFont, Element.ALIGN_CENTER));
            signTable.addCell(makeCell(utama2.nama, signFont, Element.ALIGN_CENTER));
            signTable.addCell(makeCell(mengetahui.nama, signFont, Element.ALIGN_CENTER));

            // Jabatan
            signTable.addCell(makeCell(utama1.jabatan, signFont, Element.ALIGN_CENTER));
            signTable.addCell(makeCell(utama2.jabatan, signFont, Element.ALIGN_CENTER));
            signTable.addCell(makeCell(mengetahui.jabatan, signFont, Element.ALIGN_CENTER));

            pdf.add(signTable);

            pdf.add(new Paragraph("\n")); // Spacer

            pdf.close();

            return Response.ok(out.toByteArray())
                    .header("Content-Disposition", "attachment; filename=berita-acara.pdf")
                    .build();

        } catch (Exception e) {
            return Response.serverError().entity("Gagal membuat PDF: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/generate")
    @Consumes("application/json")
    public Response generateBeritaAcara(
            @jakarta.ws.rs.QueryParam("format") String format,
            BeritaAcaraRequest request) {

        try {
            if ("pdf".equalsIgnoreCase(format)) {
                return generatePdf(request);
            } else if ("docx".equalsIgnoreCase(format)) {
                return generateDocx(request);
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Format tidak dikenal. Gunakan format=pdf atau format=docx")
                    .build();
            }
        } catch (Exception e) {
            return Response.serverError()
                    .entity("Terjadi kesalahan: " + e.getMessage())
                    .build();
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

    private String toIndoNumber(int n) {
        String[] angka = { "", "Satu", "Dua", "Tiga", "Empat", "Lima", "Enam", "Tujuh", "Delapan", "Sembilan" };
        if (n < 10) return angka[n];
        if (n == 10) return "Sepuluh";
        if (n == 11) return "Sebelas";
        if (n < 20) return angka[n % 10] + " Belas";
        if (n < 100) return angka[n / 10] + " Puluh " + angka[n % 10];
        if (n < 200) return "Seratus " + toIndoNumber(n % 100);
        if (n < 1000) return angka[n / 100] + " Ratus " + toIndoNumber(n % 100);
        if (n < 2000) return "Seribu " + toIndoNumber(n % 1000);
        return String.valueOf(n);
    }
    private String stripHtml(String html) {
        return html == null ? "" : html.replaceAll("<[^>]*>", "").replace("&nbsp;", " ");
        }

        private PdfPCell makeCell(String content, Font font, int align) {
            PdfPCell cell = new PdfPCell(new Phrase(content, font));
            cell.setHorizontalAlignment(align);
            cell.setBorder(Rectangle.NO_BORDER);
            return cell;
    }

}
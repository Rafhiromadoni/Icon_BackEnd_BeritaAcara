// src/main/java/com/rafhi/controller/BeritaAcaraResource.java
package com.rafhi.controller;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rafhi.dto.BeritaAcaraRequest;
import com.rafhi.dto.Fitur;
import com.rafhi.dto.Signatory;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("/berita-acara")
@Produces("application/pdf")
@Consumes("application/json")
public class BeritaAcaraResource {

    @POST
    @Path("/generate")
    public Response generateBeritaAcara(BeritaAcaraRequest request) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36); // Margin: kiri, kanan, atas, bawah
        PdfWriter.getInstance(doc, out);
        doc.open();

        // 1. Definisikan Font dan Spacing
        Font fontJudul = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font fontIsi = FontFactory.getFont(FontFactory.HELVETICA, 12);
        Font fontIsiBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        float spacingAfter = 12f;

        // 2. Tambahkan Logo Header
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        
        // Coba muat logo dari resources
        try (InputStream plnIs = getClass().getResourceAsStream("/images/iconplus_logo.png");
             InputStream iconIs = getClass().getResourceAsStream("/images/pln_logo.png")) {
            
            if (iconIs != null) {
                Image iconLogo = Image.getInstance(iconIs.readAllBytes());
                iconLogo.scaleToFit(100, 50);
                PdfPCell iconCell = new PdfPCell(iconLogo);
                iconCell.setBorder(Rectangle.NO_BORDER);
                iconCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                headerTable.addCell(iconCell);
            }

            if (plnIs != null) {
                Image plnLogo = Image.getInstance(plnIs.readAllBytes());
                plnLogo.scaleToFit(100, 50);
                PdfPCell plnCell = new PdfPCell(plnLogo);
                plnCell.setBorder(Rectangle.NO_BORDER);
                plnCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                headerTable.addCell(plnCell);
            }
            doc.add(headerTable);
        } catch (Exception e) {
            // Jika logo tidak ditemukan, cetak pesan error di console
            System.err.println("Gagal memuat logo: " + e.getMessage());
        }

        // 3. Tata Ulang Judul dengan Tabel
        Paragraph judul = new Paragraph("BERITA ACARA " + request.jenisBeritaAcara.toUpperCase(), fontJudul);
        judul.setAlignment(Element.ALIGN_CENTER);
        judul.setSpacingAfter(2f);
        doc.add(judul);

        Paragraph subJudul = new Paragraph("PERUBAHAN " + request.kategoriAplikasi.toUpperCase(), fontJudul);
        subJudul.setAlignment(Element.ALIGN_CENTER);
        doc.add(subJudul);

        if (request.namaAplikasiSpesifik != null && !request.namaAplikasiSpesifik.isEmpty()) {
            Paragraph appName = new Paragraph(request.namaAplikasiSpesifik.toUpperCase(), fontJudul);
            appName.setAlignment(Element.ALIGN_CENTER);
            doc.add(appName);
        }

        Paragraph nomor = new Paragraph("No. " + request.nomorBA, fontIsi);
        nomor.setAlignment(Element.ALIGN_CENTER);
        nomor.setSpacingAfter(spacingAfter);
        doc.add(nomor);

        // --- Logika Kondisional untuk Konten ---
        String jenisBA = request.jenisBeritaAcara;

        if ("UAT".equalsIgnoreCase(jenisBA)) {
            String kalimatUat = "Pada hari ini " + getTanggalTextual(request.tanggalPelaksanaan) +
                    ", telah dibuat Berita Acara " + request.jenisBeritaAcara +
                    (request.tahap != null ? " " + request.tahap : "") +
                    " terhadap permohonan perubahan aplikasi merujuk pada " +
                    request.tipeRequest + " dengan nomor surat " + request.nomorSuratRequest +
                    " tanggal " + getTanggalTextual(request.tanggalSuratRequest) +
                    " perihal \"" + request.judulPekerjaan + "\".";
            Paragraph paraUat = new Paragraph(kalimatUat, fontIsi);
            paraUat.setSpacingAfter(spacingAfter);
            doc.add(paraUat);

            PdfPTable tableUat = new PdfPTable(new float[]{1, 5, 2, 3}); // No, Kegiatan, Status, Keterangan
            tableUat.setWidthPercentage(100f);
            tableUat.addCell(getCell("No", Element.ALIGN_CENTER, fontIsiBold, true));
            tableUat.addCell(getCell("Kegiatan", Element.ALIGN_CENTER, fontIsiBold, true));
            tableUat.addCell(getCell("Status", Element.ALIGN_CENTER, fontIsiBold, true));
            tableUat.addCell(getCell("Keterangan", Element.ALIGN_CENTER, fontIsiBold, true));

            if (request.fiturList != null) {
                int i = 1;
                for (Fitur f : request.fiturList) {
                    tableUat.addCell(getCell(String.valueOf(i++), Element.ALIGN_CENTER, fontIsi, true));
                    tableUat.addCell(getCell(f.deskripsi, Element.ALIGN_LEFT, fontIsi, true));
                    tableUat.addCell(getCell(f.status, Element.ALIGN_CENTER, fontIsi, true));
                    tableUat.addCell(getCell(f.catatan != null ? f.catatan : "-", Element.ALIGN_CENTER, fontIsi, true));
                }
                doc.add(tableUat);
            }
        } else if ("Deployment".equalsIgnoreCase(jenisBA)) {
            String kalimatDeploy = "Pada hari ini " + getTanggalTextual(request.tanggalPelaksanaan) +
                    ", telah dibuat Berita Acara Penyebaran (" + request.jenisBeritaAcara + ")" +
                    (request.tahap != null ? " " + request.tahap : "") +
                    " fitur tambahan berdasarkan BA UAT nomor " + request.nomorBaUat +
                    " tentang permohonan perubahan aplikasi merujuk pada " +
                    request.tipeRequest + " dengan nomor surat " + request.nomorSuratRequest +
                    " tanggal " + getTanggalTextual(request.tanggalSuratRequest) +
                    " perihal \"" + request.judulPekerjaan + "\".";
            Paragraph paraDeploy = new Paragraph(kalimatDeploy, fontIsi);
            paraDeploy.setSpacingAfter(spacingAfter);
            doc.add(paraDeploy);

            PdfPTable tableDeploy = new PdfPTable(new float[]{1, 7, 2}); // No, Aktivitas, Status
            tableDeploy.setWidthPercentage(100f);
            tableDeploy.addCell(getCell("No.", Element.ALIGN_CENTER, fontIsiBold, true));
            tableDeploy.addCell(getCell("Aktifitas", Element.ALIGN_CENTER, fontIsiBold, true));
            tableDeploy.addCell(getCell("Status", Element.ALIGN_CENTER, fontIsiBold, true));

            tableDeploy.addCell(getCell("1.", Element.ALIGN_CENTER, fontIsi, true));
            tableDeploy.addCell(getCell("Pengecekan validasi sesuai dengan UAT", Element.ALIGN_LEFT, fontIsi, true));
            tableDeploy.addCell(getCell("OK", Element.ALIGN_CENTER, fontIsi, true));

            tableDeploy.addCell(getCell("2.", Element.ALIGN_CENTER, fontIsi, true));
            tableDeploy.addCell(getCell("Penyebaran / deployment fitur baru", Element.ALIGN_LEFT, fontIsi, true));
            tableDeploy.addCell(getCell("OK", Element.ALIGN_CENTER, fontIsi, true));

            tableDeploy.addCell(getCell("3.", Element.ALIGN_CENTER, fontIsi, true));
            tableDeploy.addCell(getCell("Pengujian hasil proses Penyebaran/ deployment", Element.ALIGN_LEFT, fontIsi, true));
            tableDeploy.addCell(getCell("OK", Element.ALIGN_CENTER, fontIsi, true));

            doc.add(tableDeploy);
        }
        // --- Kalimat Penutup ---
        Paragraph pPenutup = new Paragraph("Demikian Berita Acara ini dibuat untuk dipergunakan sebagaimana mestinya.", fontIsi);
        pPenutup.setSpacingBefore(spacingAfter);
        pPenutup.setSpacingAfter(spacingAfter);
        doc.add(pPenutup);

        // 5. Refinement Tabel Tanda Tangan
        List<Signatory> mengetahuiList = request.signatoryList.stream()
                .filter(s -> "mengetahui".equalsIgnoreCase(s.tipe))
                .toList();
        List<Signatory> utamaList = request.signatoryList.stream()
                .filter(s -> "utama".equalsIgnoreCase(s.tipe))
                .toList();


        buildTandaTangan(doc, request, fontIsi);

        doc.close();
        String fileName = request.nomorBA != null && !request.nomorBA.isBlank() 
            ? "berita-acara-" + request.nomorBA + ".pdf" 
            : "berita-acara.pdf";

        return Response.ok(out.toByteArray())
            .header("Content-Disposition", "attachment; filename=" + fileName)
            .build();
    }
    @POST
    @Path("/preview")
    @Produces("application/json")
    public Response previewBeritaAcara(BeritaAcaraRequest request) throws Exception {
        byte[] pdfBytes = (byte[]) generateBeritaAcara(request).getEntity();
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);
        return Response.ok(Map.of("base64", base64)).build();
    }

    private void buildTandaTangan(Document doc, BeritaAcaraRequest request, Font font) throws Exception {
        List<Signatory> mengetahuiList = request.signatoryList.stream().filter(s -> "mengetahui".equalsIgnoreCase(s.tipe)).toList();
        List<Signatory> utamaList = request.signatoryList.stream().filter(s -> "utama".equalsIgnoreCase(s.tipe)).toList();

        if (request.jumlahKolomTtd == 3) {
            if (utamaList.size() != 2 || mengetahuiList.size() != 1) {
                throw new IllegalArgumentException("Format tanda tangan 3 kolom memerlukan 2 utama dan 1 mengetahui");
            }

            PdfPTable atas = new PdfPTable(2);
            atas.setWidthPercentage(100f);
            atas.addCell(getCell(utamaList.get(0).perusahaan, Element.ALIGN_CENTER, font, false));
            atas.addCell(getCell(utamaList.get(1).perusahaan, Element.ALIGN_CENTER, font, false));
            atas.addCell(getCell(utamaList.get(0).nama, Element.ALIGN_CENTER, font, false));
            atas.addCell(getCell(utamaList.get(1).nama, Element.ALIGN_CENTER, font, false));
            atas.addCell(getCell(utamaList.get(0).jabatan, Element.ALIGN_CENTER, font, false));
            atas.addCell(getCell(utamaList.get(1).jabatan, Element.ALIGN_CENTER, font, false));
            doc.add(atas);

            Paragraph bawah = new Paragraph();
            bawah.setSpacingBefore(40f);
            bawah.setAlignment(Element.ALIGN_CENTER);
            bawah.add(new Phrase(request.labelTengah + "\n\n", font));
            bawah.add(new Phrase(mengetahuiList.get(0).perusahaan + "\n", font));
            bawah.add(new Phrase(mengetahuiList.get(0).nama + "\n", font));
            bawah.add(new Phrase(mengetahuiList.get(0).jabatan, font));
            doc.add(bawah);
        } else {
            PdfPTable table = new PdfPTable(utamaList.size());
            table.setWidthPercentage(100f);
            for (Signatory s : utamaList) table.addCell(getCell(s.perusahaan, Element.ALIGN_CENTER, font, false));
            for (Signatory s : utamaList) table.addCell(getCell(s.nama, Element.ALIGN_CENTER, font, false));
            for (Signatory s : utamaList) table.addCell(getCell(s.jabatan, Element.ALIGN_CENTER, font, false));
            doc.add(table);
        }
    }

    private PdfPCell getCell(String text, int alignment, Font font, boolean border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        if (!border) cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private String getTanggalTextual(String dateStr) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate d = LocalDate.parse(dateStr, fmt);
            return d.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("id", "ID"))
                    + " tanggal " + toIndoNumber(d.getDayOfMonth())
                    + " bulan " + d.getMonth().getDisplayName(TextStyle.FULL, new Locale("id", "ID"))
                    + " tahun " + toIndoNumber(d.getYear());
        } catch (Exception e) {
            return dateStr;
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
}
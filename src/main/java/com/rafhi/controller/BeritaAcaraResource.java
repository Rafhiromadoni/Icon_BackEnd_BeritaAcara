// src/main/java/com/rafhi/controller/BeritaAcaraResource.java
package com.rafhi.controller;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import com.lowagie.text.Chunk;
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
        Font fontJudul = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font fontIsi = FontFactory.getFont(FontFactory.HELVETICA, 11);
        Font fontIsiBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
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
            String kalimatPembuka = "Pada hari ini " + getTanggalFormal(request.tanggalPelaksanaan) +
                    ", telah dibuat Berita Acara " + request.jenisBeritaAcara +
                    (request.tahap != null ? " " + request.tahap : "") +
                    " terhadap permohonan perubahan aplikasi merujuk pada " +
                    request.tipeRequest + " dengan nomor surat " + request.nomorSuratRequest +
                    " tanggal " + getTanggalFormal(request.tanggalSuratRequest) +
                    " perihal \"" + request.judulPekerjaan + "\".";
            Paragraph p1 = new Paragraph(kalimatPembuka, fontIsi);
            p1.setSpacingAfter(spacingAfter);
            doc.add(p1);

            // 4. Implementasi Tabel Aktivitas (dengan styling)
            PdfPTable table = new PdfPTable(new float[]{1, 5, 2, 3}); // Proporsi lebar kolom
            table.setWidthPercentage(100f);
            table.addCell(getCell("No", Element.ALIGN_CENTER, fontIsiBold, true));
            table.addCell(getCell("Kegiatan", Element.ALIGN_CENTER, fontIsiBold, true));
            table.addCell(getCell("Status", Element.ALIGN_CENTER, fontIsiBold, true));
            table.addCell(getCell("Keterangan", Element.ALIGN_CENTER, fontIsiBold, true));

            int i = 1;
            for (Fitur f : request.fiturList) {
                table.addCell(getCell(String.valueOf(i++), Element.ALIGN_CENTER, fontIsi, true));
                table.addCell(getCell(f.deskripsi, Element.ALIGN_LEFT, fontIsi, true));
                table.addCell(getCell(f.status, Element.ALIGN_CENTER, fontIsi, true));
                table.addCell(getCell(f.catatan != null ? f.catatan : "-", Element.ALIGN_CENTER, fontIsi, true));
            }
            doc.add(table);

        } else if ("Deployment".equalsIgnoreCase(jenisBA)) {
            String kalimatPembuka = "Pada hari ini " + getTanggalFormal(request.tanggalPelaksanaan) +
                    ", telah dibuat Berita Acara Penyebaran (" + request.jenisBeritaAcara + ")" +
                    (request.tahap != null ? " " + request.tahap : "") +
                    " fitur tambahan berdasarkan BA UAT nomor " + request.nomorBaUat +
                    " tentang permohonan perubahan aplikasi merujuk pada " +
                    request.tipeRequest + " dengan nomor surat " + request.nomorSuratRequest +
                    " tanggal " + getTanggalFormal(request.tanggalSuratRequest) +
                    " perihal \"" + request.judulPekerjaan + "\".";
            Paragraph p1 = new Paragraph(kalimatPembuka, fontIsi);
            p1.setSpacingAfter(spacingAfter);
            doc.add(p1);

            PdfPTable table = new PdfPTable(new float[]{1, 7, 2}); // Proporsi lebar kolom
            table.setWidthPercentage(100f);
            table.addCell(getCell("No.", Element.ALIGN_CENTER, fontIsiBold, true));
            table.addCell(getCell("Aktifitas", Element.ALIGN_CENTER, fontIsiBold, true));
            table.addCell(getCell("Status", Element.ALIGN_CENTER, fontIsiBold, true));

            table.addCell(getCell("1.", Element.ALIGN_CENTER, fontIsi, true));
            table.addCell(getCell("Pengecekan validasi sesuai dengan UAT", Element.ALIGN_LEFT, fontIsi, true));
            table.addCell(getCell("OK", Element.ALIGN_CENTER, fontIsi, true));
            table.addCell(getCell("2.", Element.ALIGN_CENTER, fontIsi, true));
            table.addCell(getCell("Penyebaran / deployment fitur baru", Element.ALIGN_LEFT, fontIsi, true));
            table.addCell(getCell("OK", Element.ALIGN_CENTER, fontIsi, true));
            table.addCell(getCell("3.", Element.ALIGN_CENTER, fontIsi, true));
            table.addCell(getCell("Pengujian hasil proses Penyebaran/ deployment", Element.ALIGN_LEFT, fontIsi, true));
            table.addCell(getCell("OK", Element.ALIGN_CENTER, fontIsi, true));
            doc.add(table);
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

        if (!mengetahuiList.isEmpty()) {
            doc.add(new Paragraph("Mengetahui,", fontIsi));
            PdfPTable mengetahuiTable = new PdfPTable(mengetahuiList.size());
            mengetahuiTable.setWidthPercentage(100f);
            mengetahuiTable.setSpacingBefore(4f);
            for (Signatory s : mengetahuiList) {
                mengetahuiTable.addCell(getCell(s.perusahaan, Element.ALIGN_CENTER, fontIsi, false));
            }
            for (Signatory s : mengetahuiList) {
                PdfPCell namaCell = getCell(s.nama, Element.ALIGN_CENTER, fontIsi, false);
                namaCell.setFixedHeight(60f); // Memberi ruang untuk tanda tangan
                namaCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
                mengetahuiTable.addCell(namaCell);
            }
            for (Signatory s : mengetahuiList) {
                mengetahuiTable.addCell(getCell(s.jabatan, Element.ALIGN_CENTER, fontIsi, false));
            }
            doc.add(mengetahuiTable);
        }

        PdfPTable utamaTable = new PdfPTable(utamaList.size());
        utamaTable.setWidthPercentage(100f);
        utamaTable.setSpacingBefore(spacingAfter);

        for (Signatory s : utamaList) {
            utamaTable.addCell(getCell(s.perusahaan, Element.ALIGN_CENTER, fontIsi, false));
        }
        for (Signatory s : utamaList) {
            PdfPCell namaCell = getCell(s.nama, Element.ALIGN_CENTER, fontIsi, false);
            namaCell.setFixedHeight(60f); // Memberi ruang untuk tanda tangan
            namaCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
            utamaTable.addCell(namaCell);
        }
        for (Signatory s : utamaList) {
            utamaTable.addCell(getCell(s.jabatan, Element.ALIGN_CENTER, fontIsi, false));
        }
        doc.add(utamaTable);

        doc.close();
        return Response.ok(out.toByteArray())
                .header("Content-Disposition", "attachment; filename=berita-acara-" + request.nomorBA + ".pdf")
                .build();
    }

    private PdfPCell getCell(String text, int alignment, Font font, boolean border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5f); // Memberi padding pada semua sel
        if (!border) {
            cell.setBorder(Rectangle.NO_BORDER);
        }
        return cell;
    }

    private String getTanggalFormal(String dateStr) {
        try {
            DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate date = LocalDate.parse(dateStr, inputFormat);
            DayOfWeek day = date.getDayOfWeek();
            String hari = day.getDisplayName(TextStyle.FULL, new Locale("id", "ID"));
            DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));
            return hari + ", " + date.format(outputFormat);
        } catch (Exception e) {
            return dateStr;
        }
    }
}
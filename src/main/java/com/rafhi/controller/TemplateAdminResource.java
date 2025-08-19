package com.rafhi.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.rafhi.dto.DefineTemplateRequest;
import com.rafhi.entity.Template;
import com.rafhi.entity.TemplatePlaceholder;
import com.rafhi.service.TemplateService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

@Path("/api/admin/templates")
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TemplateAdminResource {

    @Inject
    TemplateService templateService;

    @ConfigProperty(name = "template.upload.path")
    String uploadPath;

    // Method untuk CREATE (POST) dan GET All (sudah benar, tidak diubah)
    // ... (kode untuk uploadAndScan, defineAndSave, getAllTemplatesForAdmin)

    @POST
    @Path("/upload-and-scan")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadAndScan(MultipartFormDataInput input) {
        // ... (kode Anda yang sudah ada, tidak perlu diubah)
        try {
            Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
            List<InputPart> inputParts = uploadForm.get("file");
            if (inputParts == null || inputParts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Bagian 'file' tidak ditemukan.").build();
            }

            InputPart filePart = inputParts.get(0);
            String originalFileName = getFileName(filePart.getHeaders());
            InputStream inputStream = filePart.getBody(InputStream.class, null);

            java.nio.file.Path tempPath = Files.createTempFile("template-", ".docx");
            Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);

            Set<String> placeholders = templateService.scanPlaceholders(tempPath);

            Map<String, Object> response = new HashMap<>();
            response.put("tempFilePath", tempPath.toString());
            response.put("placeholders", placeholders);
            response.put("originalFileName", originalFileName);

            return Response.ok(response).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Gagal memproses file: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/define-and-save")
    @Transactional
    public Response defineAndSave(DefineTemplateRequest request) {
        // ... (kode Anda yang sudah ada, tidak perlu diubah)
        try {
            java.nio.file.Path tempPath = Paths.get(request.tempFilePath);
            if (!Files.exists(tempPath)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("File sementara tidak ditemukan.").build();
            }

            String newFileName = UUID.randomUUID() + ".docx";
            java.nio.file.Path finalPath = Paths.get(uploadPath, newFileName);
            Files.createDirectories(finalPath.getParent());
            Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);

            Template template = new Template();
            template.templateName = request.templateName;
            template.description = request.description;
            template.originalFileName = request.originalFileName;
            template.fileNameStored = newFileName;
            template.setPlaceholders(new ArrayList<>()); // Inisialisasi list
            template.persist();

            for (TemplatePlaceholder ph : request.placeholders) {
                ph.template = template;
                ph.persist();
                template.getPlaceholders().add(ph);
            }

            return Response.status(Response.Status.CREATED).entity(template).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Gagal menyimpan template: " + e.getMessage()).build();
        }
    }

    @GET
    public Response getAllTemplatesForAdmin() {
        return Response.ok(templateService.listAllForAdmin()).build();
    }
    
    @GET
    @Path("/{id}")
    public Response getTemplateById(@PathParam("id") Long id) {
        Template template = templateService.findByIdWithPlaceholders(id);
        if (template == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(template).build();
    }

    // <-- PERBAIKAN UTAMA ADA DI METHOD DI BAWAH INI -->
    /**
     * Menyimpan perubahan pada template yang sudah ada.
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateTemplate(@PathParam("id") Long id, DefineTemplateRequest request) {
        // 1. Ambil template LENGKAP dengan list placeholder-nya
        Template template = templateService.findByIdWithPlaceholders(id);
        if (template == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // 2. Update field utama dari template
        template.templateName = request.templateName;
        template.description = request.description;
        template.isActive = request.isActive;

        // 3. Ambil referensi ke list yang sudah ada (dikelola Hibernate)
        List<TemplatePlaceholder> managedPlaceholders = template.getPlaceholders();

        // 4. BERSIHKAN list yang ada. Karena ada `orphanRemoval=true`,
        //    Hibernate akan secara otomatis menghapus placeholder lama dari database.
        managedPlaceholders.clear();

        // 5. Buat dan tambahkan placeholder baru ke list YANG SAMA.
        for (TemplatePlaceholder phFromRequest : request.placeholders) {
            TemplatePlaceholder newPh = new TemplatePlaceholder();
            newPh.template = template;
            newPh.placeholderKey = phFromRequest.placeholderKey;
            newPh.label = phFromRequest.label;
            newPh.dataType = phFromRequest.dataType;
            newPh.isRequired = phFromRequest.isRequired;
            // Tidak perlu `newPh.persist()` karena relasi di-cascade dari `template`
            managedPlaceholders.add(newPh);
        }

        // 6. Simpan perubahan pada template. Cascade akan menangani penyimpanan placeholder baru.
        template.persist();

        return Response.ok(template).build();
    }


    @DELETE
    @Path("/{id}")
    public Response deleteTemplate(@PathParam("id") Long id) {
        try {
            templateService.delete(id);
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Gagal menghapus file template fisik: " + e.getMessage())
                    .build();
        }
    }

    private String getFileName(MultivaluedMap<String, String> headers) {
        String[] contentDisposition = headers.getFirst("Content-Disposition").split(";");
        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                return filename.split("=")[1].trim().replaceAll("\"", "");
            }
        }
        return "unknown";
    }
}
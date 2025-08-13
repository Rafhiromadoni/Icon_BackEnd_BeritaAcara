package com.rafhi.controller;

import com.rafhi.dto.DefineTemplateRequest;
import com.rafhi.entity.Template;
import com.rafhi.entity.TemplatePlaceholder;
import com.rafhi.service.TemplateService;

// --- PERBAIKAN IMPORT ---
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path; // <-- KITA IMPOR YANG INI
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.io.InputStream;
// import java.nio.file.Path; // <-- JANGAN IMPOR YANG INI
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


@Path("/api/admin/templates") // Ini akan merujuk ke jakarta.ws.rs.Path
@RolesAllowed("ADMIN")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class TemplateAdminResource {

    @Inject TemplateService templateService;
    @ConfigProperty(name = "template.upload.path") String uploadPath;

    @POST
    @Path("/upload-and-scan")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadAndScan(MultipartFormDataInput input) throws IOException {
        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("file");
        if (inputParts == null || inputParts.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Bagian 'file' tidak ditemukan.").build();
        }
        InputPart filePart = inputParts.get(0);
        String originalFileName = getFileName(filePart.getHeaders());
        InputStream inputStream = filePart.getBody(InputStream.class, null);

        // --- PERBAIKAN PENGGUNAAN PATH ---
        // Gunakan nama paket lengkap (fully qualified name) di sini
        java.nio.file.Path tempPath = Files.createTempFile("template-", ".docx");
        Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
        
        Set<String> placeholders = templateService.scanPlaceholders(tempPath);
        
        Map<String, Object> response = new HashMap<>();
        response.put("tempFilePath", tempPath.toString());
        response.put("placeholders", placeholders);
        response.put("originalFileName", originalFileName);
        
        return Response.ok(response).build();
    }
    
    @POST
    @Path("/define-and-save")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response defineAndSave(DefineTemplateRequest request) throws IOException {
        // --- PERBAIKAN PENGGUNAAN PATH ---
        java.nio.file.Path tempPath = Paths.get(request.tempFilePath);
        String newFileName = UUID.randomUUID() + ".docx";
        java.nio.file.Path finalPath = Paths.get(uploadPath, newFileName);
        Files.createDirectories(finalPath.getParent());
        Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        
        Template template = new Template();
        template.templateName = request.templateName;
        template.description = request.description;
        template.originalFileName = request.originalFileName;
        template.fileNameStored = newFileName;
        template.persist();
        
        for (TemplatePlaceholder ph : request.placeholders) {
            ph.template = template;
            ph.persist();
        }
        
        return Response.status(Response.Status.CREATED).entity(template).build();
    }
    
    @GET
    public Response getAllTemplatesForAdmin() { return Response.ok(templateService.listAllForAdmin()).build(); }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteTemplate(@PathParam("id") Long id) throws IOException {
        templateService.delete(id);
        return Response.noContent().build();
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
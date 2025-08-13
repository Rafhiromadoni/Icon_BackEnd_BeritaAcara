package com.rafhi.controller;

import com.rafhi.dto.LoginRequest;
import com.rafhi.dto.RegisterRequest;
import com.rafhi.entity.AppUser;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.util.Set; // Gunakan Set untuk groups

@Path("/auth")
public class AuthResource {

    @Inject
    SecurityIdentity securityIdentity;

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @PermitAll // Menandakan endpoint ini bisa diakses semua orang
    public Response register(RegisterRequest data) {
        if (AppUser.find("username", data.username).count() > 0) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"Username sudah terdaftar.\"}")
                    .build();
        }
        
        // --- PERBAIKAN: Set peran saat registrasi ---
        // Asumsi pengguna baru selalu memiliki peran "USER".
        // Gunakan metode `add` yang sudah ada di AppUser.
        AppUser.add(data.username, data.password, "USER");
        
        return Response.status(Response.Status.CREATED)
                       .entity("{\"message\":\"Registrasi berhasil.\"}")
                       .build();
    }

    // @POST
    // @Path("/login")
    // @Consumes(MediaType.APPLICATION_JSON)
    // @Produces(MediaType.APPLICATION_JSON) // Lebih baik mengembalikan JSON
    // @PermitAll
    // public Response login(LoginRequest credentials) {
    //     // Logika verifikasi password sudah diurus oleh Quarkus Security Realm,
    //     // tapi kita perlu mencari user untuk mendapatkan rolenya.
    //     AppUser user = AppUser.find("username", credentials.username).firstResult();
        
    //     // BcryptUtil.matches sudah tidak diperlukan jika realm aktif,
    //     // tapi kita tetap memerlukannya di sini untuk verifikasi manual sebelum membuat token.
    //     // Ini adalah pendekatan yang aman.
    //     if (user != null && io.quarkus.elytron.security.common.BcryptUtil.matches(credentials.password, user.password)) {
            
    //         // --- PERBAIKAN: Pastikan `groups` diisi dengan `user.role` ---
    //         String token = Jwt.issuer("https://yourdomain.com/issuer")
    //                           .upn(user.username) // upn (User Principal Name) adalah klaim standar
    //                           .groups(Set.of(user.role)) // Klaim 'groups' diisi dengan peran
    //                           .expiresIn(Duration.ofHours(24))
    //                           .sign();

    //         // Kembalikan token dalam format JSON
    //         return Response.ok("{\"token\":\"" + token + "\"}").build();
    //     }

    //     return Response.status(Response.Status.UNAUTHORIZED)
    //                    .entity("{\"error\":\"Username atau password salah.\"}")
    //                    .build();
    // }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response login(LoginRequest credentials) {
        // --- TAMBAHKAN LOGGING DI SINI ---
        System.out.println("--- Mencoba Login ---");
        System.out.println("Username dari Request: '" + credentials.username + "'");
        System.out.println("Password dari Request: '" + credentials.password + "'");

        AppUser user = AppUser.find("username", credentials.username).firstResult();
        
        if (user != null) {
            System.out.println("User Ditemukan di DB: '" + user.username + "'");
            System.out.println("Hash Password dari DB: '" + user.password + "'");

            boolean passwordMatches = io.quarkus.elytron.security.common.BcryptUtil.matches(credentials.password, user.password);
            System.out.println("Apakah password cocok? " + passwordMatches);

            if (passwordMatches) {
                System.out.println("Login Berhasil! Membuat token...");
                String token = Jwt.issuer("https://yourdomain.com/issuer")
                                  .upn(user.username)
                                  .groups(Set.of(user.role))
                                  .expiresIn(Duration.ofHours(24))
                                  .sign();
                return Response.ok("{\"token\":\"" + token + "\"}").build();
            }
        } else {
            System.out.println("User TIDAK Ditemukan di DB untuk username: '" + credentials.username + "'");
        }

        System.out.println("Login Gagal. Mengembalikan 401 Unauthorized.");
        return Response.status(Response.Status.UNAUTHORIZED)
                       .entity("{\"error\":\"Username atau password salah.\"}")
                       .build();
    }

    @GET
    @Path("/me")
    @Authenticated // Hanya untuk pengguna yang sudah login
    @Produces(MediaType.APPLICATION_JSON)
    public Response me() {
        String username = securityIdentity.getPrincipal().getName();
        // --- PERBAIKAN: Kembalikan juga role pengguna ---
        // Ambil role dari SecurityIdentity
        Set<String> roles = securityIdentity.getRoles();
        // Ambil role pertama, asumsi hanya ada satu role per user
        String role = roles.isEmpty() ? "USER" : roles.iterator().next(); 
        
        // Buat objek JSON yang lebih informatif untuk frontend
        String jsonResponse = String.format("{\"username\":\"%s\", \"role\":\"%s\"}", username, role);
        
        return Response.ok(jsonResponse).build();
    }
}
package com.rafhi.entity;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.Username;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user") // Pastikan nama tabel ini sama dengan di database
public class AppUser extends PanacheEntity {

    /**
     * Nama pengguna, harus unik. Digunakan untuk login.
     * Anotasi @Username menandai field ini untuk Quarkus Security.
     */
    @Username
    public String username;

    /**
     * Password yang sudah di-hash (misalnya, menggunakan bcrypt).
     * Anotasi @Password menandai field ini untuk Quarkus Security.
     */
    @Password
    public String password;

    /**
     * Peran pengguna, misalnya "ADMIN" atau "USER".
     * Anotasi @Roles menandai field ini untuk Quarkus Security.
     * Bisa juga berupa List<String> jika satu pengguna bisa punya banyak peran,
     * tapi untuk kasus ini, satu String cukup.
     */
    @Roles
    public String role;

    /**
     * Metode helper untuk menambahkan pengguna baru dengan password yang di-hash.
     * Ini sangat berguna di AuthResource saat registrasi.
     *
     * @param username Nama pengguna baru.
     * @param password Password mentah (plain text).
     * @param role     Peran pengguna.
     */
    public static void add(String username, String password, String role) {
        AppUser user = new AppUser();
        user.username = username;
        user.password = BcryptUtil.bcryptHash(password);
        user.role = role;
        user.persist();
    }
}
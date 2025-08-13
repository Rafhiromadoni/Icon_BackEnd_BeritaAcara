-- Hapus semua data lama untuk memastikan awal yang bersih setiap kali restart
-- (PENTING: Jangan gunakan TRUNCATE di produksi!)
TRUNCATE TABLE app_user RESTART IDENTITY CASCADE;

-- Masukkan data pengguna biasa
-- Password mentahnya adalah 'password'
INSERT INTO app_user(id, username, password, role) VALUES (1, 'user', '$2a$10$O9Tb7Zito4XOLbXQTsPWY.bBcXIIFfOaGWpGRj7HOYPTE6nzE6adO', 'USER');

-- Masukkan data pengguna admin
-- Password mentahnya adalah 'admin'
INSERT INTO app_user(id, username, password, role) VALUES (2, 'admin', '$2a$10$kqVdfe07Tdma1rxcu8184utXwC4T33/eUmHIXTTMfS0GU4vZvHi/q', 'ADMIN');

-- Atur ulang urutan ID agar ID selanjutnya dimulai dari 3
ALTER SEQUENCE app_user_seq RESTART WITH 3;
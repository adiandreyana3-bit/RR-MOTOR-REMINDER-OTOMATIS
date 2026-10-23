RR MOTOR REMINDER OTOMATIS — PROYEK AWAL
Package: com.rrmotor.remindergantioli
Firebase project: rr-motor-reminder

PENTING:
1. Letakkan google-services.json dari proyek Firebase rr-motor-reminder di folder app/google-services.json.
2. Aktifkan Cloud Firestore pada Firebase Console.
3. Untuk pengujian awal, buat aturan Firestore yang sesuai dan aman. Jangan gunakan aturan publik terbuka untuk produksi.
4. Aplikasi ini adalah starter project. Daftar jatuh tempo dan tombol WhatsApp dasar tersedia, tetapi alarm tanggal jatuh tempo otomatis per pelanggan dan pemulihan alarm setelah reboot BELUM selesai di versi ini.
5. Tombol WhatsApp membuka percakapan dengan teks siap kirim; pengguna tetap harus menekan Kirim di WhatsApp.
6. Workflow GitHub Actions membangun APK debug dan menyimpan APK sebagai artifact.

Cara pakai GitHub:
- Buat repository baru atau siapkan repository yang ingin dipakai.
- Unggah semua isi folder ini ke root repository.
- Tambahkan google-services.json ke folder app/.
- Commit perubahan ke branch main.
- Buka tab Actions dan jalankan "Build RR MOTOR Reminder APK".
- Unduh artifact RR-MOTOR-REMINDER-APK setelah workflow sukses.

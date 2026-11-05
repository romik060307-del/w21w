# RK Server Guard

Aplikasi Android pendamping untuk memilih maksimal dua aplikasi server yang sudah terpasang, membuka aplikasi tersebut, membuka halaman Info Aplikasi/baterainya, lalu menjalankan layanan penjaga yang terlihat melalui notifikasi.

## Yang sudah ada

- Dua pilihan aplikasi server (Server 1 dan Server 2).
- Tombol untuk membuka setiap aplikasi yang dipilih.
- Tombol menuju halaman Info Aplikasi untuk mengatur aktivitas latar belakang/baterai aplikasi tujuan.
- Foreground service dengan notifikasi tetap dan tombol Hentikan.
- Opsi `PARTIAL_WAKE_LOCK` agar CPU perangkat tetap aktif selama penjagaan dinyalakan.
- Tombol meminta pengecualian optimasi baterai untuk RK Server Guard sendiri.
- Dukungan minimum Android 8.0 (API 26), dengan target SDK 35.
- GitHub Actions untuk menghasilkan APK debug.

## Batas penting — baca sebelum dipakai

APK ini **tidak dapat memaksa proses APK lain agar selalu hidup**. Android tidak menyediakan izin biasa bagi satu aplikasi untuk mengunci proses aplikasi lain supaya tidak dihentikan sistem. Penjaga ini menjaga CPU selama layanan penjaga hidup dan menyediakan pintasan pengaturan baterai aplikasi tujuan; hasilnya dapat membantu, tetapi bukan jaminan server tujuan terus berjalan.

Agar server benar-benar terus hidup, kode server lokal dan server online perlu dijalankan sebagai layanan latar depan di dalam aplikasi server itu sendiri, atau kedua mesin server perlu digabungkan ke APK host ini. Itu memerlukan source code dua server (misalnya ZIP proyek Android/Node/PHP/dll.), bukan hanya nama APK terpasang.

Status “Penjagaan aktif” berarti layanan penjaga hidup. Status tersebut **bukan** pemeriksaan bahwa server tujuan sedang merespons.

## Cara membangun APK melalui GitHub

1. Ekstrak ZIP proyek ini di komputer.
2. Buat repository GitHub baru.
3. Unggah seluruh isi folder proyek ke akar repository. Pastikan folder `.github/workflows` ikut terunggah.
4. Buka tab **Actions** di repository.
5. Pilih **Build RK Server Guard APK**, lalu tekan **Run workflow**. Push ke branch `main` atau `master` juga akan memicu build.
6. Setelah workflow selesai dengan status sukses, buka hasil **Artifacts** dan unduh `RK-Server-Guard-APK`.
7. Ekstrak artifact untuk mengambil `app-debug.apk`, lalu instal di HP.

APK debug cocok untuk pengujian/sideload. Untuk distribusi resmi, gunakan signing key milik sendiri dan build release.

## Pemakaian

1. Buka RK Server Guard.
2. Pilih APK server pertama dan/atau kedua.
3. Tekan **Info APK / baterai** pada masing-masing pilihan. Di halaman pengaturan Android/Vivo, izinkan penggunaan latar belakang dan kurangi pembatasan baterai bila opsinya tersedia.
4. Kembali ke RK Server Guard.
5. Biarkan **Jaga CPU saat layar mati** aktif bila server memang perlu terus bekerja. Perlu diketahui opsi ini mengonsumsi baterai lebih banyak.
6. Tekan **MULAI PENJAGAAN** dan izinkan notifikasi.
7. Pastikan notifikasi RK Server Guard terlihat. Gunakan tombol **Hentikan** pada notifikasi atau aplikasi untuk mematikannya.

Di beberapa HP Vivo, pengaturan tambahan seperti Autostart atau pengelolaan konsumsi daya latar belakang berada di menu sistem yang berbeda-beda.

## Detail teknis

- Foreground service dimulai dari tombol yang ditekan pengguna saat aplikasi terlihat.
- Layanan memakai tipe `specialUse` untuk layanan penjaga yang dimulai pengguna pada Android 14+.
- Partial wake lock dilepas ketika penjagaan dihentikan atau layanan dihancurkan.
- Layanan menggunakan `START_STICKY`, tetapi Android/OEM tetap dapat menghentikan atau membatasi aplikasi.
- Tidak ada root, eksploitasi, atau akses tersembunyi ke proses aplikasi lain.

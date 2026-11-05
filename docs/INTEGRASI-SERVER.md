# Integrasi agar server sungguhan berjalan di dalam APK

RK Server Guard saat ini adalah penjaga daya untuk aplikasi lain. Agar server lokal dan server online benar-benar berada di bawah kendali layanan yang sama, integrasikan kode mesin server ke aplikasi yang menjadi host.

## Pola arsitektur yang diperlukan

- `ServerLocal.start()` dan `ServerLocal.stop()` mengendalikan server lokal.
- `ServerOnline.start()` dan `ServerOnline.stop()` mengendalikan proses server kedua.
- `ServerHostService` memanggil `startForeground()` sebelum memulai proses server.
- Saat pengguna memilih Server 1, Server 2, atau keduanya, service memulai modul yang dipilih.
- Saat pengguna menekan Stop, service menghentikan modul yang aktif dan melepas wake lock.
- Tambahkan pemeriksaan `/health` untuk masing-masing server sehingga aplikasi bisa menampilkan status server sungguhan, bukan hanya status layanan penjaga.

## Informasi source yang dibutuhkan

Untuk mengerjakan integrasi nyata, siapkan ZIP source code dari kedua server atau satu ZIP proyek APK yang memuat keduanya. Sertakan juga cara server sekarang dijalankan, port lokal yang digunakan, dan apakah server online memerlukan token/konfigurasi khusus. Jangan masukkan password, private key, atau token rahasia ke ZIP.

Nama paket APK saja tidak cukup untuk menggabungkan proses server ke dalam layanan Android. Android juga tidak mengizinkan aplikasi biasa memaksa proses APK pihak lain tetap hidup.

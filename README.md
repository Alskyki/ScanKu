# ScanKu — Aplikasi Scanner Dokumen untuk Android

Aplikasi scanner dokumen (Kotlin + Jetpack Compose) dengan alur kerja seperti aplikasi scanner populer:
foto dokumen, potong otomatis, beri filter, gabung jadi PDF, lalu bagikan.

## Fitur

| Fitur | Cara kerja |
|---|---|
| Scan dengan kamera | ML Kit Document Scanner: deteksi tepi, crop dan perataan perspektif otomatis |
| Impor dari galeri | Tersedia di dalam layar scanner |
| Multi-halaman | Tambah halaman ke dokumen, atur urutan, hapus halaman |
| Mode Kartu ID | Scan 2 sisi kartu sekaligus |
| Filter | Asli, Magic, Terang, Abu-abu, Hitam Putih (ambang adaptif, tahan bayangan) |
| Rotasi | Putar kiri/kanan, dengan opsi "filter ke semua halaman" |
| Ekspor PDF | 4 pilihan kualitas (Kecil / Sedang / Tinggi / Asli) |
| Atur ukuran file | Tentukan batas ukuran (100 KB – 5 MB), aplikasi mengompres otomatis |
| Bagikan / Simpan | Share ke aplikasi lain atau simpan ke penyimpanan lewat pemilih berkas |
| OCR | Ambil teks dari satu halaman atau seluruh dokumen, offline, bisa disalin dan dibagikan |
| Cari dokumen | Pencarian berdasarkan nama, ubah nama, hapus |

## Cara membuka dan menjalankan

1. Pasang **Android Studio** versi terbaru (Ladybug 2024.2 atau lebih baru).
2. `File > Open` lalu pilih folder `ScanKu`.
3. Tunggu Gradle sync selesai (butuh internet; Android Studio mengunduh Gradle 8.9 otomatis).
4. Sambungkan HP Android (aktifkan Debugging USB) lalu tekan **Run**.
5. Untuk membuat APK: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

Catatan:
- Android 8.0 (API 26) ke atas.
- Scanner dan OCR memakai Google Play Services / ML Kit, jadi butuh HP dengan Google Play Services.
  Jika memakai emulator, pilih image yang memiliki ikon Play Store.
- Saat pertama kali scanner dibuka, Play Services mungkin mengunduh modul scanner sebentar.

## Struktur kode

```
app/src/main/java/com/scanku/app/
├── MainActivity.kt          navigasi antar layar
├── AppViewModel.kt          state aplikasi dan operasi dokumen
├── data/
│   ├── Models.kt            ScanDocument, ScanPage, PageFilter
│   └── DocumentRepository.kt  penyimpanan berkas dan meta.json
├── core/
│   ├── ImageProcessor.kt    decode, rotasi, filter
│   ├── PdfExporter.kt       penulis PDF (JPEG DCT) + kontrol ukuran
│   └── OcrHelper.kt         OCR ML Kit
└── ui/
    ├── HomeScreen.kt        daftar dokumen, tombol scan
    ├── DocumentScreen.kt    halaman-halaman dokumen
    ├── PageEditorScreen.kt  filter dan rotasi
    ├── ExportDialog.kt      ekspor PDF dan pengaturan ukuran
    ├── OcrScreen.kt         hasil teks
    ├── ScannerLauncher.kt   pembuka scanner ML Kit
    └── Utils.kt, theme/Theme.kt
```

## Ide pengembangan lanjutan

- Crop manual dengan 4 titik sudut untuk gambar yang gagal terdeteksi otomatis
- Tanda tangan dan watermark
- Kunci PDF dengan kata sandi
- Folder / tag dan sinkronisasi cloud
- Ekspor ke Word atau teks dengan tata letak

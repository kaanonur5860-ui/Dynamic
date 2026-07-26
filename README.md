# Android Dynamic Island Overlay

Diğer uygulamaların/oyunların ÜZERİNDE gösterilen, iPhone Dynamic Island'a benzeyen
küçük, dokunulunca genişleyen bir "hap" (pill) overlay penceresi.

## Nasıl çalışır
- `OverlayService`, `WindowManager` ile `TYPE_APPLICATION_OVERLAY` tipinde bir pencere açar.
- Ekranın üst ortasında sabit durur, parmakla sürüklenebilir.
- Üzerine dokununca (sürüklemeden) genişler/daralır (Dynamic Island animasyonu gibi).
- `updateContent(title, subtitle)` fonksiyonu ile dışarıdan (örn. bir FPS ölçer veya
  bildirim geldiğinde) metin güncellenebilir.

## Kurulum
1. Bu klasörü mevcut bir Android Studio projesine kopyala ya da yeni bir proje açıp
   dosyaları üzerine yerleştir (paket adı `com.example.dynamicisland`; değiştirmek
   istersen hem klasör yolunu hem `package` satırlarını güncelle).
2. Gradle sync yap.
3. Uygulamayı çalıştır, "Overlay'i Başlat" butonuna bas.
4. Sistem "diğer uygulamaların üzerinde göster" izni ekranını açacak, izni ver.
5. Ana ekrana dön ya da bir oyun aç — hap şeklindeki overlay ekranın üstünde görünecek.

## Önemli notlar / kısıtlamalar
- **SYSTEM_ALERT_WINDOW izni**: Kullanıcının manuel olarak ayarlardan onaylaması gerekir,
  koddan otomatik alınamaz.
- **Android 14 (API 34)+**: Foreground service tipi olarak `specialUse` kullanıldı ve
  manifestte gerekçe (`PROPERTY_SPECIAL_USE_FGS_SUBTYPE`) eklendi. Google Play'e
  yayınlarken bu kullanım için Play Console'da ek açıklama/gerekçe istenebilir.
- **Bazı oyunlar tam ekran/immersive mod** kullandığında sistem overlay'leri
  gizleyebilir; bu OS/oyun davranışına bağlıdır, uygulama tarafından tam kontrol edilemez.
- **Play Store politikası**: "Görüntüle diğer uygulamaların üzerinde" iznini kullanan
  uygulamalar incelemeden geçmek için net bir kullanım amacı belirtmelidir (performans
  göstergesi, bildirim vb.). Kötüye kullanım (örn. reklam bombardımanı) politika ihlalidir.
- FPS değeri örnekte sabit "60" yazıyor; gerçek FPS ölçümü için `Choreographer.FrameCallback`
  ile frame süresi ölçülüp `updateContent()` çağrılabilir.

## Dosyalar
- `app/src/main/AndroidManifest.xml` — izinler ve servis tanımı
- `app/src/main/java/.../OverlayService.kt` — overlay penceresi ve animasyon mantığı
- `app/src/main/java/.../MainActivity.kt` — izin isteme ve servisi başlatma/durdurma
- `app/src/main/res/layout/activity_main.xml` — basit başlat/durdur ekranı

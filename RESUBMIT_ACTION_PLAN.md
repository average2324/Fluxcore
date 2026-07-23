# FluxCore — 4.3(a) Yeniden Gönderim Aksiyon Planı

Build 1.0 (17) · Submission ID `749f4899-adda-41f4-9d50-3137422e0eed`

---

## Önce stratejik karar: itirazı sürümle birlikte mi göndereceğiz?

**Hayır. Önce sadece mesajı gönder, build'i beklet.** Sebebi:

Her reddediliş bir sicil kaydı. 4.3(a)'da arka arkaya red almak hesap kapanmasına giden yol.
Resolution Center'a mesaj yazmak **review hakkı harcamaz** — inceleme sırasına girmez, sadece
ekibe ulaşır. Yani hiçbir maliyeti olmadan şunu sorabilirsin:

> "Bizi hangi uygulamayla eşleştirdiniz?"

Cevap gelirse, hedefli düzeltme yapıp öyle gönderirsin. Cevap gelmezse 2-3 gün sonra zaten
build 17'yi gönderirsin — kaybın olmaz. Bu yüzden **A aşamasını B'den önce yap.**

---

## AŞAMA A — Şimdi yap (build'i göndermeden)

### A1. Resolution Center'a itiraz mesajını yaz

1. Aç: **https://appstoreconnect.apple.com/apps**
2. **FluxCore** uygulamasına tıkla
3. Sol menüde reddedilen sürümü seç (**iOS App 1.0 — Rejected**)
4. Sayfada **"App Review"** / **"Resolution Center"** bölümünü aç
   (doğrudan link: https://appstoreconnect.apple.com/apps → app → App Review)
5. Apple'ın mesajının altındaki **"Reply"** kutusuna,
   [`APP_REVIEW_4.3_RESPONSE.md`](APP_REVIEW_4.3_RESPONSE.md) dosyasındaki
   **"## Draft reply"** başlığı ile **"---"** arasındaki metni **olduğu gibi** yapıştır.
   - `## Notes for you, not for Apple` bölümünü **gönderme**. O bölüm sana ait.
   - Doldurulacak boşluk kalmadı, metin hazır.
6. **Send** / **Gönder**

> Türkçe yazmak istersen serbestsin, Apple "Reply to this message in your preferred language"
> diyor. Ama İngilizce gönderirsen daha hızlı ve daha az yanlış anlaşılmayla ilerler.

### A2. Play Console kanıtını yanına hazırla

İtirazdaki tablo Google'ın kayıtlarına dayanıyor. Apple isterse hemen gönderebilmek için
ekran görüntülerini şimdiden al:

1. Aç: **https://play.google.com/console**
2. **Flux Core: Arcade Survival** → sol menü **Test edin ve yayınlayın** → **Test etme** → **Kapalı test**
3. **Sürüm geçmişi** tablosunun ekran görüntüsünü al (0.2.4 / 31 Mart → 0.2.9 / 5 Haziran)
4. **Üretim** sekmesinden 0.2.12 / 22 Temmuz ekran görüntüsünü al

Bu iki görüntü, "repackaged template / purchased template / asset flip" iddialarını çürüten
tarihli kanıt. Apple isterse doğrudan yapıştır.

### A3. App Store Connect metadata'nı gözden geçir

Bu **kodda değil**, ben göremiyorum, ve reddin "metadata" ayağı burada.

1. **https://appstoreconnect.apple.com/apps** → FluxCore → **App Information** ve
   **1.0 Prepare for Submission**
2. Şunları jenerik olmaktan çıkar:
   - **App Name / Subtitle** — Play'de "Flux Core: Arcade Survival". "Arcade Survival"
     çok jenerik. Oyunun kendine has yanını öne çıkar (100 seviyelik kampanya, gemi dükkânı,
     zaman yavaşlatma) .
   - **Keywords** — "arcade, survival, hexagon, dodge, reflex" gibi klon kümesiyle
     örtüşen jenerik kelimeler varsa azalt.
   - **Description** — şablon listelemesi gibi durmasın; oyuna özel sistemleri anlat.
   - **Screenshots** — stok görünümlü/jenerik olmasın, gerçek oyun ekranları olsun,
     iPad görüntüleri de dahil (inceleme iPad Air M3'te yapılıyor).
3. **App Review Information → Notes** alanına şunu ekle:

   ```
   All music and sound effects in this build are original work synthesised in-house.
   All in-game graphics (ships, icons, effects) are generated procedurally at runtime.
   The only third-party asset is the Noto Sans font (SIL OFL).
   Development history is available on Google Play closed testing from 31 March 2026.
   ```

---

## AŞAMA B — Apple cevap verince ya da 2-3 gün sonra

### B1. Build 17'nin TestFlight'ta işlendiğini doğrula

1. **https://appstoreconnect.apple.com/apps** → FluxCore → **TestFlight** sekmesi
2. **1.0 (17)** görünene kadar bekle. Yükleme sonrası işlenme 10–60 dakika sürebilir.
3. Durum **"Ready to Submit"** olmalı. "Missing Compliance" çıkarsa şifreleme sorusunu
   yanıtla (bu oyunda standart HTTPS dışında şifreleme yok → genelde "No").

### B2. Sürüme yeni build'i bağla ve gönder

1. Sol menüden **iOS App 1.0** (Rejected) sürümünü aç
2. **Build** bölümünde **+** / **Düzenle** ile **1.0 (17)**'yi seç
3. Sayfanın üstünden **"Add for Review"** → **"Submit for Review"**
4. Gönderim sırasında sorulursa **"Yes"** deyip yaptığın değişiklikleri özetle
   (aynı metnin kısa hali yeter)

---

## Benim yaptıklarım (tamamlandı)

| # | İş | Durum |
|---|---|---|
| 1 | Kevin MacLeod müzikleri paketten çıkarıldı | ✅ |
| 2 | Yerine kurum içi sentezlenmiş 2 orijinal döngü eklendi | ✅ |
| 3 | Twemoji ikonları çıkarıldı, HUD vektör çizime geçti | ✅ |
| 4 | UIverse `touch_tap.png` çıkarıldı | ✅ |
| 5 | Kullanılmayan 556 KB `Button.png` silindi | ✅ |
| 6 | `HexagonGame` → `FluxCoreGame`, `com/orbitflux/**` dizinleri taşındı | ✅ |
| 7 | Uygulama içi lisans ekranı + `THIRD_PARTY_NOTICES.md` güncellendi | ✅ |
| 8 | Premium satın alma kilitlenmeleri düzeltildi (watchdog + deferred) | ✅ |
| 9 | iOS build numarası 17'ye yükseltildi | ✅ |
| 10 | Eksik `IOS_KEYCHAIN_PASSWORD` secret'ı için fallback | ✅ |
| 11 | Yerel doğrulama: test + derleme + lint + gerçek oyun koşusu | ✅ |
| 12 | Yeni repo'ya (`thbyr0610/Fluxcore`) push + build tetiklendi | ✅ |

**Build'i izle:** https://github.com/thbyr0610/Fluxcore/actions

---

## Yararlı linkler

| Ne | Link |
|---|---|
| App Store Connect | https://appstoreconnect.apple.com/apps |
| Apple developer iletişim | https://developer.apple.com/contact/app-store/ |
| Guideline 4.3 (Spam) | https://developer.apple.com/app-store/review/guidelines/#spam |
| Google Play Console | https://play.google.com/console |
| GitHub Actions (build) | https://github.com/thbyr0610/Fluxcore/actions |
| GitHub repo secrets | https://github.com/thbyr0610/Fluxcore/settings/secrets/actions |

---

## Yapma

- **Play'deki "public first" iddiasını kullanma.** Mart-Haziran sürümleri kapalı testti,
  production 22 Temmuz'da açıldı, Apple ilk reddi 9 Temmuz'da verdi. Kontrol edilir ve çöker.
- **Arka arkaya körlemesine yeniden gönderme.** Cevap gelmeden ikinci kez göndermek yerine
  önce Resolution Center'dan eşleşen uygulamayı sor.
- **Android `applicationId`'yi (`com.orbitflux`) değiştirme.** Canlı Play listelemeni ve
  imzalamayı kırar, iOS binary'sine zaten girmiyor.

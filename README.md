# 🦆 GeminiDuck — Zoptymalizowany Klient Webowy Gemini na Androida

GeminiDuck to lekki wrapper Android WebView dla serwisu **gemini.google.com**, wzorowany na projektach modyfikujących klientów webowych (takich jak Spotiduck).

Działa w 100% niezależnie od systemowej aplikacji Google (`com.google.android.googlequicksearchbox`), omija blokadę logowania Google OAuth oraz oferuje ulepszenia interfejsu i dodatkowe funkcje.

📥 **Pobierz najnowszą wersję (.apk):** [GitHub Releases](https://github.com/4zjata/geminiduck/releases/latest)

---

## ✨ Kluczowe Funkcje

1. **Obejście blokady Google OAuth (Błąd 403: disallowed_useragent):**
   * Wyczyszczony User-Agent bez znaczników `wv` oraz `Version/4.0`.
   * Pozwala na bezproblemowe logowanie kontem Google bez instalacji aplikacji Google.
2. **Czysty czarny motyw AMOLED (True Black):**
   * Podmiana domyślnego szarego motywu Google na głęboką czerń (`#000000`), idealną na ekrany OLED.
3. **Wbudowana blokada trackerów i telemetrii:**
   * Przechwytywanie i odrzucanie zapytań do Google Analytics, GTM, DoubleClick oraz loggerów Play na poziomie WebView (`shouldInterceptRequest`).
   * Neutralizacja obiektów śledzących w warstwie JavaScript.
4. **Integracja z menu Udostępnij (Android Share Sheet):**
   * Możliwość udostępniania zaznaczonego tekstu, linków i zdjęć z innych aplikacji bezpośrednio do GeminiDuck.
5. **Natywna obsługa sprzętowa:**
   * Obsługa mikrofonu (WebRTC / dyktowanie głosowe).
   * Pełna obsługa aparatu (zarówno przez WebRTC jak i natywną intencję aparatu `ACTION_IMAGE_CAPTURE`).
   * Całkowicie wyłączony gest pull-to-refresh (brak przypadkowego odświeżania czatu podczas przewijania).
   * Trwały certyfikat podpisu (`geminiduck.keystore`) gwarantujący bezproblemowe aktualizacje bez wylogowywania.

---

## 📁 Struktura Projektu

```text
geminiduck/
├── .github/workflows/
│   └── build-apk.yml          # Automatyczna kompilacja APK w chmurze
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml # Uprawnienia i konfiguracja aplikacji
│   │   ├── assets/
│   │   │   ├── gemini_style.css # Modyfikacje wyglądu (AMOLED)
│   │   │   └── gemini_tweaks.js # Skrypty użytkownika (Export, Focus)
│   │   ├── java/com/geminiduck/app/
│   │   │   └── MainActivity.kt  # Główny kod natywny i silnik WebView
│   │   └── res/                 # Layouty, kolory i motywy
│   └── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Jak skompilować plik .APK?

### Opcja 1: GitHub Actions (Najprostsza — bez instalowania SDK)
1. Utwórz nowe repozytorium na [GitHubie](https://github.com).
2. W katalogu projektu zainicjuj gita i wyślij kod:
   ```bash
   git init
   git add .
   git commit -m "Initial commit GeminiDuck"
   git branch -M main
   git remote add origin https://github.com/TWOJ_USER/TWOJE_REPO.git
   git push -u origin main
   ```
3. Przejdź do zakładki **Actions** w swoim repozytorium na GitHubie.
4. Po zakończeniu zadania (*Build GeminiDuck APK*) pobierz gotowy plik z sekcji **Artifacts** (*GeminiDuck-Debug-APK*).

### Opcja 2: Android Studio (Komputer)
1. Otwórz folder `geminiduck` w Android Studio.
2. Poczekaj na synchronizację Gradle.
3. Kliknij `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`.
4. Gotowy plik `.apk` znajdziesz w `app/build/outputs/apk/debug/`.

---

## 🎨 Jak dostosować własne modyfikacje?

* **Zmiana wyglądu:** Edytuj plik `app/src/main/assets/gemini_style.css`.
* **Nowe skrypty / skróty:** Edytuj plik `app/src/main/assets/gemini_tweaks.js`.

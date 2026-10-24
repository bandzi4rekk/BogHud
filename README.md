# Bog HUD (Minecraft 1.21.4, Fabric)

HUD z: FPS, ping, pancerz (z wytrzymałością), koordynaty, własny tekst.
Ustawienia (włącz/wyłącz, kolory RGB tekstu i akcentu, tło, cień, skala, pozycja)
otwierasz klawiszem **Prawy Shift** (zmienisz w Opcje > Sterowanie).
Ustawienia zapisują się w `config/boghud.json`.

## Jak dostać plik .jar bez instalowania czegokolwiek (GitHub)
1. Załóż konto na github.com i stwórz nowe repozytorium.
2. Wgraj do niego całą zawartość tego folderu (przycisk "Add file" > "Upload files").
   Folder `.github` też musi się wgrać.
3. Wejdź w zakładkę **Actions**, poczekaj aż build się zakończy (zielony ptaszek).
4. Otwórz ukończony build i pobierz artefakt `boghud-jar`. W środku jest plik `boghud-1.0.0.jar`.

## Jak go uruchomić
Potrzebujesz Minecrafta Java 1.21.4 z Fabric Loaderem oraz Fabric API (1.21.4).
Wrzuć `boghud-1.0.0.jar` i plik Fabric API do folderu `.minecraft/mods`.

## Budowanie lokalnie (opcjonalnie)
JDK 21 + Gradle 8.12.1, potem `gradle build`. Jar pojawi się w `build/libs`.

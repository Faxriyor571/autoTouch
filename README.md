# AutoTouch / UZEX Auto Clicker

Bu loyiha maqsadli vaqt kirganda bir yoki bir nechta koordinataga avtomatik bosish qiladi. Hozirgi versiya UZEX `spot.uzex.uz` vaqtiga sinxron ishlashga moslangan.

## Kerakli narsalar

- Windows
- JDK 17 yoki undan yuqori
- Google Chrome yoki Microsoft Edge
- `jnativehook-2.2.2.jar` loyiha ichidagi `lib/` papkasida bo‘lishi kerak

## Loyihada nimalar bor

- `src/app/Main.java` - dastur kirish nuqtasi
- `src/ui/MainWindow.java` - asosiy oynasi
- `src/core/TimerService.java` - countdown va precision trigger
- `src/core/ClickService.java` - sichqoncha bosish logikasi
- `src/time/UzexTimeSyncService.java` - UZEX vaqt sinxi
- `src/result/ResultObserverService.java` - brauzer extension uchun lokal bridge
- `browser-extension/` - UZEX sahifasidagi natija/response kuzatuv extensioni

## Qanday ishga tushiriladi

1. Loyihani IntelliJ IDEA yoki boshqa Java IDE’da oching.
2. Project SDK ni JDK 17 ga qo‘ying.
3. `lib/jnativehook-2.2.2.jar` project classpath’ida turganini tekshiring.
4. `src/app/Main.java` ni run qiling.

## Dasturdan foydalanish

1. Dasturni oching.
2. `F1` tugmasini bosib joriy sichqoncha koordinatasini ro‘yxatga qo‘shing.
3. Kerak bo‘lsa bir nechta nuqta qo‘shing.
4. `UZEX MAQSAD VAQTI` maydoniga vaqt kiriting, masalan:
   `12:33:00.000`
5. `START` tugmasini bosing.
6. Dastur UZEX server vaqti bilan hisoblab, belgilangan vaqtda bosishni boshlaydi.

## Hotkey

- `F1` - joriy mouse koordinatasini qo‘shadi

## Vaqt qanday ishlaydi

- Lokal kompyuter vaqti alohida ko‘rsatiladi
- UZEX server vaqti alohida sinxronlanadi
- Target vaqt UZEX vaqti sifatida qabul qilinadi
- Tarmoq kechikishi `min RTT` va adaptive model bilan hisobga olinadi

## Browser extension nima uchun kerak

Bu extension login qilingan sahifadagi network/DOM natijalarini kuzatadi va lokal bridge’ga sanitized metadata yuboradi.

Bu quyidagilarni beradi:

- `TIME TOPILDI` / `TIME YO'Q` statusi
- natija kelgan endpoint haqida minimal diagnostika
- adaptive kechikish model uchun real sample

### Extension ni o‘rnatish

1. Chrome yoki Edge’da `chrome://extensions` yoki `edge://extensions` ni oching.
2. `Developer mode` ni yoqing.
3. `Load unpacked` ni bosing.
4. `browser-extension/` papkasini tanlang.
5. UZEX sahifasini qayta oching yoki reload qiling.
6. Dastur oynasida `BROWSER EXTENSION: ONLINE` chiqishini tekshiring.

## To‘g‘ri ishlashi uchun tartib

1. Dastur ochilgan bo‘lsin.
2. UZEX vaqti sinxron bo‘lsin.
3. Browser extension `ONLINE` bo‘lsin.
4. Koordinatalar oldindan qo‘shilgan bo‘lsin.
5. Target vaqt kelishidan kamida 300 ms oldin `START` bosilsin.

## Eslatma

- `12:33:00.000` ga qo‘ysangiz, bosish shu vaqtga maksimal yaqin bajariladi.
- Real aniqlik kompyuter tezligi, internet, brauzer response formati va sahifadagi login holatiga bog‘liq.
- Agar `TIME YO'Q` chiqsa, demak extension hali kerakli natijani ushlamagan.
- Agar UZEX sahifa tuzilmasi o‘zgarsa, result parserni moslashtirish kerak bo‘lishi mumkin.

## Tekshiruv

Loyiha Java compile’dan muvaffaqiyatli o‘tdi. Interactive GUI smoke test esa real user session’da qo‘lda tekshiriladi.


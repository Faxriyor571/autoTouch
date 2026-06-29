# AutoTouch / UZEX Auto Clicker

Bu loyiha maqsadli vaqt kirganda bir yoki bir nechta koordinataga avtomatik bosish qiladi. Hozirgi versiya UZEX `spot.uzex.uz` vaqtiga sinxron ishlashga moslangan.

## Kerakli narsalar

- Windows
- JDK 17 yoki undan yuqori
- Google Chrome yoki Microsoft Edge
- `jnativehook-2.2.2.jar` loyiha ichidagi `lib/` papkasida boâ€˜lishi kerak

## Loyihada nimalar bor

- `src/app/Main.java` - dastur kirish nuqtasi
- `src/ui/MainWindow.java` - asosiy oynasi
- `src/core/TimerService.java` - countdown va precision trigger
- `src/core/ClickService.java` - sichqoncha bosish logikasi
- `src/time/UzexTimeSyncService.java` - UZEX vaqt sinxi
- `src/result/ResultObserverService.java` - brauzer extension uchun lokal bridge
- `browser-extension/` - UZEX sahifasidagi natija/response kuzatuv extensioni

## Qanday ishga tushiriladi

1. Loyihani IntelliJ IDEA yoki boshqa Java IDEâ€™da oching.
2. Project SDK ni JDK 17 ga qoâ€˜ying.
3. `lib/jnativehook-2.2.2.jar` project classpathâ€™ida turganini tekshiring.
4. `src/app/Main.java` ni run qiling.

## Dasturdan foydalanish

1. Dasturni oching.
2. `F2` tugmasini bosib joriy sichqoncha koordinatasini roâ€˜yxatga qoâ€˜shing.
3. Kerak boâ€˜lsa bir nechta nuqta qoâ€˜shing.
4. `UZEX MAQSAD VAQTI` maydoniga vaqt kiriting, masalan:
   `12:33:00.000`
5. `START` tugmasini bosing.
6. Dastur UZEX server vaqti bilan hisoblab, belgilangan vaqtda bosishni boshlaydi.

## Hotkey

- `F2` - joriy mouse koordinatasini qoâ€˜shadi

## Vaqt qanday ishlaydi

- Lokal kompyuter vaqti alohida koâ€˜rsatiladi
- UZEX server vaqti alohida sinxronlanadi
- Target vaqt UZEX vaqti sifatida qabul qilinadi
- Tarmoq kechikishi `min RTT` va adaptive model bilan hisobga olinadi

## Browser extension nima uchun kerak

Bu extension login qilingan sahifadagi network/DOM natijalarini kuzatadi va lokal bridgeâ€™ga sanitized metadata yuboradi.

Bu quyidagilarni beradi:

- `TIME TOPILDI` / `TIME YO'Q` statusi
- natija kelgan endpoint haqida minimal diagnostika
- adaptive kechikish model uchun real sample

### Extension ni oâ€˜rnatish

1. Chrome yoki Edgeâ€™da `chrome://extensions` yoki `edge://extensions` ni oching.
2. `Developer mode` ni yoqing.
3. `Load unpacked` ni bosing.
4. `browser-extension/` papkasini tanlang.
5. UZEX sahifasini qayta oching yoki reload qiling.
6. Dastur oynasida `BROWSER EXTENSION: ONLINE` chiqishini tekshiring.

## Toâ€˜gâ€˜ri ishlashi uchun tartib

1. Dastur ochilgan boâ€˜lsin.
2. UZEX vaqti sinxron boâ€˜lsin.
3. Browser extension `ONLINE` boâ€˜lsin.
4. Koordinatalar oldindan qoâ€˜shilgan boâ€˜lsin.
5. Target vaqt kelishidan kamida 300 ms oldin `START` bosilsin.

## Eslatma

- `12:33:00.000` ga qoâ€˜ysangiz, bosish shu vaqtga maksimal yaqin bajariladi.
- Real aniqlik kompyuter tezligi, internet, brauzer response formati va sahifadagi login holatiga bogâ€˜liq.
- Agar `TIME YO'Q` chiqsa, demak extension hali kerakli natijani ushlamagan.
- Agar UZEX sahifa tuzilmasi oâ€˜zgarsa, result parserni moslashtirish kerak boâ€˜lishi mumkin.

## Tekshiruv

Loyiha Java compileâ€™dan muvaffaqiyatli oâ€˜tdi. Interactive GUI smoke test esa real user sessionâ€™da qoâ€˜lda tekshiriladi.


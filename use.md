# AutoTouch / UZEX Auto Clicker

Bu dastur ma'lum vaqt kelganda sichqoncha bosishlarni avtomatik qiladi. Hozirgi sozlama UZEX `spot.uzex.uz` vaqtiga qarab ishlaydi.

## 1. Nima kerak bo'ladi

Ishlash uchun quyidagilar kerak:

- Windows kompyuter
- JDK 17 yoki undan yuqori
- Google Chrome yoki Microsoft Edge
- `jnativehook-2.2.2.jar` fayli loyiha ichidagi `lib/` papkasida

## 2. Loyiha qismlari

Loyihada quyidagi asosiy qismlar bor:

- `src/app/Main.java` - dasturni ishga tushiradigan joy
- `src/ui/MainWindow.java` - asosiy oynasi
- `src/core/TimerService.java` - vaqtni sanash va aniq trigger
- `src/core/ClickService.java` - koordinataga bosish logikasi
- `src/time/UzexTimeSyncService.java` - UZEX server vaqti bilan sinxronlash
- `src/result/ResultObserverService.java` - browser extension bilan lokal aloqa
- `browser-extension/` - UZEX sahifasidagi natijani kuzatadigan extension

## 3. Dasturni ishga tushirish

Boshlashdan oldin:

1. IntelliJ IDEA yoki boshqa Java IDE'ni oching.
2. Project SDK ni JDK 17 ga qo'ying.
3. `lib/jnativehook-2.2.2.jar` project classpath'da borligini tekshiring.
4. `src/app/Main.java` faylini run qiling.

Ishga tushgandan keyin asosiy oyna ochiladi.

## 4. Browser extension nimaga kerak

Extension UZEX sahifasidagi natija, response, DOM o'zgarishlarini kuzatadi. Bu orqali:

- `TIME TOPILDI` yoki `TIME YO'Q` holati ko'rinadi
- real natija kelganini bilasiz
- adaptive kechikish modeli uchun sample yig'iladi

Extension bosish uchun emas. U faqat sahifadagi natijani ushlash va aniqlikni oshirish uchun kerak.

## 5. Extensionni qachon ishlatish kerak

Extensionni UZEX saytiga kirishdan oldin yoki kirgan zahoti yoqib qo'yish kerak.

To'g'ri ketma-ketlik:

1. Dasturni ochasiz.
2. Browser extensionni yoqasiz.
3. UZEX saytini ochasiz.
4. Saytga login qilasiz.
5. Sahifani reload qilasiz.
6. Dastur oynasida `BROWSER EXTENSION: ONLINE` chiqishini kutasiz.

Shundan keyingina extension ishga tayyor hisoblanadi.

## 6. Extensionni o'rnatish

Chrome:

1. `chrome://extensions` ga kiring.
2. `Developer mode` ni yoqing.
3. `Load unpacked` ni bosing.
4. `browser-extension/` papkasini tanlang.
5. UZEX sahifasini qayta oching yoki reload qiling.

Edge:

1. `edge://extensions` ga kiring.
2. `Developer mode` ni yoqing.
3. `Load unpacked` ni bosing.
4. `browser-extension/` papkasini tanlang.
5. UZEX sahifasini qayta oching yoki reload qiling.

Extension to'g'ri ulangan bo'lsa, dastur ichida `BROWSER EXTENSION: ONLINE` ko'rinadi.

## 7. Koordinatalarni qachon qo'shish kerak

Koordinatalarni `F1` bilan qo'shasiz.

To'g'ri tartib:

1. Sichqonchani bosiladigan tugma yoki maydon ustiga olib boring.
2. `F1` tugmasini bosing.
3. Agar boshqa nuqtalar ham kerak bo'lsa, ularni ham birma-bir qo'shing.

Muhim:

- `F1` bosilganda joriy mouse joyi saqlanadi
- qancha nuqta qo'shsangiz, shuncha bosish ketma-ket bajariladi
- bosish tartibi ro'yxatga qo'shilgan ketma-ketlik bo'yicha bo'ladi

## 8. Target vaqtni qachon kiritish kerak

Koordinatalar tayyor bo'lgandan keyin target vaqt kiriting.

Masalan:

`12:33:00.000`

Target vaqt kirishdan oldin quyidagilarni tekshiring:

- UZEX vaqti sinxron ko'rinayotgan bo'lsin
- extension `ONLINE` bo'lsin
- ko'rsatgan vaqt o'tib ketmagan bo'lsin
- ishni boshlashdan oldin kamida 300 ms zaxira vaqt bo'lsin

## 9. `START` ni qachon bosish kerak

`START` tugmasini target vaqtdan oldin bosasiz.

Tartib:

1. Koordinatalar qo'shilgan bo'ladi.
2. Extension ishlayotgan bo'ladi.
3. UZEX vaqti ko'rinib turadi.
4. Target vaqt kiritilgan bo'ladi.
5. `START` bosiladi.

Shundan keyin dastur:

- vaqtni kuzatadi
- oxirgi soniyalarda aniqroq rejimga o'tadi
- target kelganda bosishni bajaradi

## 10. Qaysi vaqtda nima bo'ladi

Bu oqimni bir joyda ko'ring:

1. Dastur ochiladi.
2. Extension yoqiladi.
3. UZEX saytida login qilinadi.
4. Sahifa reload qilinadi.
5. `BROWSER EXTENSION: ONLINE` tekshiriladi.
6. Kerakli koordinatalar `F1` bilan qo'shiladi.
7. Target vaqt kiritiladi.
8. `START` bosiladi.
9. Dastur UZEX vaqtiga qarab kutadi.
10. Target yaqinlashganda bosish bajariladi.
11. Natija oynada chiqadi.

## 11. Natija nimani bildiradi

Dastur bosgandan keyin quyidagilar ko'rinishi mumkin:

- lokal bosish vaqti
- UZEX vaqti
- taxminiy serverga yetib borish farqi
- bir nechta bosishning davomiyligi

Extension ishlagan bo'lsa:

- `TIME TOPILDI` chiqishi mumkin
- yoki hali natija topilmagan bo'lsa `TIME YO'Q` chiqadi

## 12. To'g'ri ishlashi uchun qisqa tartib

Eng to'g'ri amaliy tartib:

1. Dasturni oching.
2. Extensionni yoqing.
3. UZEX saytiga kiring.
4. Login qiling.
5. Sahifani reload qiling.
6. `BROWSER EXTENSION: ONLINE` chiqishini kuting.
7. Bosiladigan koordinatalarni `F1` bilan qo'shing.
8. Target vaqtni kiriting.
9. `START` bosib, dasturni kutishga qo'ying.

## 13. Muhim eslatmalar

- Target vaqt UZEX server vaqtiga qarab yoziladi.
- Lokal kompyuter soatiga emas, UZEX vaqtiga tayangan ma'qul.
- Extension ochiq bo'lmasa, natijani ushlash qiyinlashadi.
- Agar `TIME YO'Q` bo'lsa, extension kerakli response yoki DOM o'zgarishini hali topmagan bo'ladi.
- Agar UZEX sahifa tuzilishi o'zgarsa, extension parserini yangilash kerak bo'lishi mumkin.

## 14. Qisqa misol

Masalan, siz `12:33:00.000` ga bosishni xohlaysiz:

1. Dasturni ochasiz.
2. Extensionni yoqasiz.
3. UZEX saytiga kirasiz.
4. Koordinatani `F1` bilan qo'shasiz.
5. `12:33:00.000` ni kiritasiz.
6. `START` bosasiz.
7. Dastur 12:33 ga yaqinlashganda o'zi bosadi.


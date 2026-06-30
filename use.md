# AutoTouch Foydalanish Qo'llanmasi

Bu versiya faqat qo'lda berilgan server/lokal vaqt farqi bilan ishlaydi. Browser extension, UZEX diagnostika va network compensation olib tashlangan.

## 1. Kerakli narsalar

- Windows kompyuter
- JDK 17 yoki undan yuqori
- `jnativehook-2.2.2.jar` fayli `lib/` papkasida

## 2. Asosiy oqim

1. Dasturni oching.
2. Koordinatalarni `F2` bilan qo'shing.
3. Server maqsad vaqtini `HH:mm:ss.SSS` formatida kiriting.
4. Server va lokal vaqt farqini kiriting.
5. Checkbox bilan `Server vaqti oldinda` yoki `Lokal vaqt oldinda` ni belgilang.
6. `START` tugmasini target vaqtdan kamida 300 ms oldin bosing.

## 3. Qanday hisoblaydi

- Lokal vaqt oynada alohida ko'rinadi.
- Server vaqt qo'lda kiritilgan farq asosida hisoblanadi.
- Tugma shu hisoblangan lokal trigger vaqtiga bog'lanib bosiladi.
- Agar target vaqt juda yaqin bo'lsa, dastur startni rad etadi.

## 4. Qisqa misol

Agar server maqsad vaqt `12:33:00.000` bo'lsa va server lokal vaqtdan 16 ms oldinda bo'lsa, farqni kiriting, checkbox ni to'g'ri belgilang va `START` ni oldindan bosing. Dastur server vaqtiga mos lokal trigger vaqtini hisoblab, shu vaqtda bosadi.

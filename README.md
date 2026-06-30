# AutoTouch

Bu loyiha tanlangan koordinatalarga server vaqti bo'yicha avtomatik bosish qiladi. Lokal vaqt alohida ko'rsatiladi, server va lokal farqi qo'lda kiritiladi va tugma shu farq hisobga olingan holda aniq target vaqtga bog'lanadi.

## Qanday ishlaydi

1. Dasturni oching.
2. Bosiladigan koordinatalarni `F2` bilan qo'shing.
3. Server maqsad vaqtini `HH:mm:ss.SSS` formatida kiriting.
4. Server va lokal vaqt farqini `HH:mm:ss.SSS` formatida kiriting.
5. `Server vaqti oldinda` yoki `Lokal vaqt oldinda` ni belgilang.
6. `START` ni target vaqtdan kamida 300 ms oldin bosing.

## Eslatma

- `12:33:00.000` kabi vaqt kiritsangiz, bosish shu server vaqtiga maksimal yaqin bajariladi.
- Aniqlik kompyuter yuklamasi va Java/Windows scheduling'iga bog'liq.
- Browser extension yoki UZEX diagnostika endi ishlatilmaydi.

## Portable build

O'rnatmasiz ishlatish uchun repo ildizida `build-portable.cmd` ni ishga tushiring.

Natija:

- `dist/AutoTouch-portable.zip`

Arxivni oching va ichidagi `AutoTouch\AutoTouch.exe` ni ishga tushiring.

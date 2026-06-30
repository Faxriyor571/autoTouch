# AutoTouch / UZEX Auto Clicker

Bu loyiha maqsadli vaqt kirganda bir yoki bir nechta koordinataga avtomatik bosish qiladi. Hozirgi versiyada lokal vaqt alohida ko'rsatiladi, server va lokal farqi qo'lda kiritiladi va bosish server vaqtiga moslab hisoblanadi.

## To'g'ri ishlashi uchun tartib

1. Dastur ochilgan bo'lsin.
2. Koordinatalar oldindan qo'shilgan bo'lsin.
3. Server va lokal farqini to'g'ri kiriting.
4. Server yoki lokal qaysi biri oldinda ekanini checkbox bilan belgilang.
5. `SERVER MAQSAD VAQTI` ni kiriting.
6. Kerak bo'lsa Browser extension `ONLINE` bo'lsin.
7. Target vaqt kelishidan kamida 300 ms oldin `START` bosilsin.

## Eslatma

- `12:33:00.000` ga qo'ysangiz, bosish shu server vaqtiga maksimal yaqin bajariladi.
- Real aniqlik kompyuter tezligi, internet, brauzer response formati va sahifadagi login holatiga bog'liq.
- Agar `TIME YO'Q` chiqsa, demak extension hali kerakli natijani ushlamagan.
- Agar UZEX sahifa tuzilmasi o'zgarsa, result parserni moslashtirish kerak bo'lishi mumkin.

## Tekshiruv

Loyiha Java compileÃ¢â‚¬â„¢dan muvaffaqiyatli oÃ¢â‚¬Ëœtdi. Interactive GUI smoke test esa real user sessionÃ¢â‚¬â„¢da qoÃ¢â‚¬Ëœlda tekshiriladi.
## Portable build

O'rnatmasiz ishlatish uchun repo ildizida `build-portable.cmd` ni ishga tushiring.

Natija:

- `dist/AutoTouch-portable.zip`

Arxivni oching va ichidagi `AutoTouch\AutoTouch.exe` ni ishga tushiring.




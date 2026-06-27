# AutoTouch UZEX Result Observer

Bu Chrome/Edge extension faqat `https://spot.uzex.uz/*` sahifalarida ishlaydi.
U POST/PUT/PATCH/DELETE response ichidan vaqt kandidatini lokal
`127.0.0.1:17321` AutoTouch servisiga yuboradi.

Extension quyidagilarni yubormaydi:

- Cookie yoki Authorization header;
- login/parol/token;
- request body;
- URL query parametrlari;
- to'liq response body.

## O'rnatish

1. AutoTouch dasturini ishga tushiring.
2. Chrome yoki Edge'da `chrome://extensions` sahifasini oching.
3. `Developer mode` ni yoqing.
4. `Load unpacked` ni bosing.
5. Shu `browser-extension` papkasini tanlang.
6. UZEX sahifasini qayta yuklang.


# פרומפט ל-ChatGPT: לוגו, אייקונים ואנימציות לאפליקציה "PresentSimple"

**מה זה:** פרומפט אחד, בשלבים, שמדביקים כמו שהוא בשיחה חדשה ב-ChatGPT (עדיף עם יצירת תמונות / Code Interpreter מופעלים). הוא מבקש לוגו שמכיל את השם **PresentSimple**, אייקון אנדרואיד, איורים למסכי האפליקציה ואנימציות קצרות. כל שלב נעצר וממתין ל"continue", כך שאפשר לבחור כיוון לפני שממשיכים.

**איך משתמשים:** מדביקים את הבלוק שבתוך ``` (הוא באנגלית כי מודלי תמונה מדייקים יותר ככה; השם נשאר באנגלית גם בגרסה העברית; הכותרת העברית אופציונלית). בסוף כל שלב בודקים בעין **את האיות של PresentSimple** (P ו-S גדולות, בלי רווח, בלי אותיות מיותרות) ואת העברית, ואומרים "continue 2" או מבקשים תיקון.

**מה לשלוח לי אחרי זה:** את ה-ZIP הסופי (או את התיקיות) - אני ממיר את ה-SVG לאייקון האפליקציה (vector drawable), למסך הפתיחה, למצבי "ריק", ולחנות האפליקציות, ומחליף את השם באפליקציה אם תחליט על כך (כרגע השם בתוך האפליקציה נשאר "אירועים מיוחדים").

```
You are the brand designer, illustrator and motion designer for an Android app called "PresentSimple". The app helps one person (and family and friends) never miss birthdays, anniversaries, memorials and other special events: it shows both the Hebrew and the Gregorian date, sends reminders, and makes it one tap to send a warm greeting by WhatsApp, e-mail or SMS. The name plays on two meanings of "present": a GIFT, and the PRESENT moment - and "Simple": effortless, one tap. Personality in 3 words: warm, simple, celebratory (never childish, never corporate). The app is Hebrew-first (right-to-left) with English equal; audience is mostly non-technical adults on an Android phone.

CURRENT LOOK (keep the family, you may refine): the launcher icon is a white calendar page with an amber header band and a pink heart, on an indigo-to-magenta 135-degree gradient, with a few faint confetti dots. PALETTE (use only these): indigo #4527A0, magenta #C2185B, amber #FFB300, white #FFFFFF, ink #1B1B1F, soft background #FCF9FF; dark background #14121A with the same accents slightly lightened (indigo #7E57C2, magenta #F06292, amber #FFCA28). Gradient allowed ONLY indigo -> magenta at 135 degrees. Style: flat vector, soft rounded geometry, round caps and joins, generous empty space, no photo-realism, no tiny details, no outlines thinner than 6% of the artboard, original work only (no copied logos, mascots, brands, fonts-as-art or living artists' styles), free to use.

NAME RULE (very important): the exact wordmark text is  PresentSimple  - seven letters then six letters, no space, capital P and capital S, everything else lowercase: P-r-e-s-e-n-t-S-i-m-p-l-e. Image models often misspell lettering, so draw the letters as custom outlined shapes, then CHECK the spelling letter by letter and tell me you did. Letters: rounded, geometric, friendly sans-serif look, highly legible at 120 px wide. Give the "P" a small personal touch (for example a tiny ribbon, bow or heart-shaped counter) but keep it readable. The app keeps the SAME English name and the SAME logo in the Hebrew version: never translate or transliterate "PresentSimple" and never draw it in Hebrew letters. A Hebrew tagline is OPTIONAL and separate art only (right-to-left, correct final letters), it must read exactly:  אירועים מיוחדים

STAGE 1 - CONCEPTS. One sheet with 3 distinct directions, each shown on a light and a dark background, numbered 1-3, one-line rationale in the chat (not inside the art):
 1) A gift box whose lid ribbon forms a tiny calendar tab (or a calendar page folded like a gift), amber ribbon, symbol left + wordmark right.
 2) The letter "P" built from a ribbon bow / gift tag with a small star or heart; stacked: symbol above the wordmark.
 3) Your own idea that merges "gift" and "moment" (for example a gift whose bow is a clock hand or a candle flame, or a spark above a simple box) - still very simple.
Wait for me to choose ("continue 2").

STAGE 2 - LOGO SET for the chosen direction (SVG and transparent PNG each):
 a) logo-horizontal-light / logo-horizontal-dark: symbol + PresentSimple on one line, viewBox 640x160 (light = ink text on light; dark = white text on dark).
 b) logo-stacked-light / logo-stacked-dark: viewBox 480x420.
 c) wordmark-light / wordmark-dark: the name only, viewBox 560x120.
 d) tagline-he-light / tagline-he-dark (optional, separate from every logo): "אירועים מיוחדים", viewBox 480x80.
 e) symbol.svg: the symbol alone, viewBox 512x512, transparent.
 f) ANDROID LAUNCHER ICON, viewBox 108x108 (this is the adaptive-icon canvas; ONLY the central 66x66 circle is guaranteed visible, so keep all essential art inside it):
    - ic_background.svg: the indigo->magenta gradient (plus optional faint confetti dots), full 108x108 square.
    - ic_foreground.svg: the symbol only, transparent background, inside the central 66x66 safe zone.
    - ic_monochrome.svg: the symbol as ONE solid shape set in a single colour (#000000), transparent background, readable as a silhouette (used for Android 13+ themed icons).
    - ic_notification.svg: viewBox 24x24, a simple white (#FFFFFF) silhouette of the symbol, no gradient, thick strokes (status-bar icon).
    - ic_launcher_preview.png 512x512 on a rounded square mask.
 g) share-card.png 1200x630: gradient background, stacked logo centred, nothing else (English name only).
 h) splash.svg viewBox 360x640 (portrait): the symbol centred on the gradient with calm space; this is also the first frame of the splash animation.
Contrast: wordmark vs its background at least 4.5:1 in both light and dark; the logo must read at 120 px wide and the symbol at 36 px.

STAGE 3 - IN-APP ILLUSTRATIONS (SVG, no text inside, light + dark variants where marked):
 1) empty-events (240x200, light+dark): a calm gift box with a small empty calendar - "no events yet" (friendly, inviting, not sad).
 2) empty-search (240x200, light+dark): a gift box and a magnifier, nothing found.
 3) all-done (240x200, light+dark): a gift with a check mark.
 4) onboarding-1/2/3 (320x240 each): (1) a calendar with Hebrew and Gregorian dates side by side (use abstract shapes, no real numbers), (2) a bell ringing softly before a birthday cake candle, (3) a hand tapping a send button with a greeting bubble.
 5) event-type badges, 64x64, round, duotone, consistent weight: birthday (cake), anniversary (two rings), memorial (a gentle memorial candle - respectful, calm), bar/bat mitzvah (scroll and star), family (house with a heart), work (briefcase), custom (star).
 6) greeting-card frames (A5 portrait, 148x210, no text): three decorative borders - classic (double line with corner flourishes), festive (confetti and ribbon corners), minimal (two thin rules). Light backgrounds only, printable.

STAGE 4 - ANIMATIONS. Deliver each as (A) a LAYERED SVG whose moving parts are separate <g> groups with clear ids (e.g. id="lid", id="ribbon-left", id="spark-1"), so a developer can animate them natively, plus (B) a short written timing table (element, property: from -> to, start ms, duration ms, easing), plus (C) if you can produce it with code, a Lottie JSON (bodymovin) of the same animation; if not, say so and do not fake one. All loops must be calm, no flashing faster than 3 times per second, and a still frame must also look good (for "reduce motion" users). Animations:
 1) logo-reveal (1200 ms, once): the gift lid lifts slightly, the ribbon unfurls into the symbol and the wordmark fades in from below.
 2) confetti-burst (1500 ms, once): 20-30 small shapes in palette colours burst upward from a point and drift down, fading out (birthday).
 3) heart-pulse (1000 ms, loop): the heart scales 100% -> 112% -> 100% with a gentle ease.
 4) candle-glow (3000 ms, loop): a very slow soft flicker of a memorial candle flame (opacity and tiny sway only, respectful).
 5) bell-ring (800 ms, once): the bell swings 12 degrees left and right three times decaying.
 6) send-check (600 ms, once): a paper plane flies off and a check mark draws itself.
 7) page-flip (500 ms, once): a calendar page folds up and the next day appears.

TECHNICAL CONTRACT (the files are converted to Android vector drawables and checked by a script): SVG 1.1 using only <svg>, <g>, <path>, <rect>, <circle>, <ellipse>, <line>, <polygon>, <polyline>, <linearGradient>, <stop>, <clipPath>, <defs>. NO <text>, <tspan>, <image>, <filter>, <style>, <mask>, <pattern>, blend modes, scripts or external references; ALL lettering outlined into <path>. Always give viewBox; unique ids prefixed with the file name; coordinates with at most 2 decimals; each SVG under 20 KB (lockups with outlined letters may use up to 30 KB). Gradients only linear, only indigo->magenta. If you cannot outline the lettering into paths, say so and deliver 2x transparent PNGs and name the typeface you used.
Accessibility: nothing may rely on colour alone; every illustration must still make sense in greyscale. RTL: nothing in any symbol may depend on left-to-right reading.

DELIVERY: work in the stages above and stop after each ("wait for continue"). At the end of each stage list: file names, sizes in KB, colours used, and confirm that no <text>/<image>/<filter> appears in any SVG and that "PresentSimple" is spelled correctly in every file that contains it. At the end of Stage 4 put everything into ONE ZIP with folders: brand/ (Stage 2 a-e, g), android/ (Stage 2 f, h), illustrations/ (Stage 3), animations/ (Stage 4: svg, timing tables, lottie), previews/ (contact sheets on light and dark backgrounds).
Reply "ready" and start with Stage 1.
```

## מה אני עושה עם התוצרים (לתיעוד)
1. אייקון האפליקציה: `ic_background.svg`/`ic_foreground.svg`/`ic_monochrome.svg` -> `res/drawable/ic_launcher_*.xml` (קיימים היום), `ic_notification.svg` -> `ic_notification.xml`.
2. לוגו + מסך פתיחה + מסך "ברוכים הבאים": `splash.svg`, `logo-*`, `onboarding-*`.
3. מצבי "ריק" במסכים (אירועים קרובים, חיפוש, סיימת הכול) ואייקוני סוגי האירועים.
4. אנימציות: מימוש ב-Compose מקבוצות ה-SVG לפי טבלאות התזמון (בלי ספרייה חיצונית); כיבוד "הפחתת תנועה" של המערכת.
5. חנות האפליקציות (`ohadsam/os_app_store`): אייקון, צילום מסך ותיאור לפי ה-checklist שלה.
6. שם התצוגה של האפליקציה כבר שונה ל-PresentSimple (גם בגרסה העברית; השורה העברית "אירועים מיוחדים" נשארת רק כתת-כותרת אופציונלית).

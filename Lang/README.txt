MAD Launcher language overlays

These packs are copied into the APK at build (`assets/lang`, all ISO 639-2/T files except `eng.json`).
English UI is bundled `strings.xml`. `eng.json` is the translator master and is not installed as an overlay.

Get more languages still lists GitHub `lang/` when a distributor token is present.
Public APKs use the bundled packs only.

File format: one JSON object — string resource name -> translated text.
Master (English): eng.json — regenerate with: python tools/regen_en_json.py
Generate/update translations: python tools/translate_lang_packs.py [code ...]

Language codes (filename.json, ISO 639-2/T):
  eng English (master — not downloaded; built-in)
  heb Hebrew
  ara Arabic
  spa Spanish
  fra French
  deu German
  rus Russian / Русский
  por Portuguese
  ita Italian
  tur Turkish
  pol Polish
  ukr Ukrainian
  hin Hindi
  ind Indonesian
  vie Vietnamese
  zho Chinese (Simplified)
  jpn Japanese
  kor Korean
  nld Dutch
  swe Swedish
  dan Danish
  nor Norwegian
  fin Finnish
  ces Czech
  ron Romanian
  hun Hungarian
  ell Greek
  slk Slovak
  bul Bulgarian
  hrv Croatian
  srp Serbian
  lit Lithuanian
  lav Latvian
  slv Slovenian
  est Estonian
  cat Catalan
  eus Basque
  glg Galician
  fas Persian
  urd Urdu
  ben Bengali
  tam Tamil
  tel Telugu
  mar Marathi
  guj Gujarati
  kan Kannada
  mal Malayalam
  pan Punjabi
  nep Nepali
  sin Sinhala
  tha Thai
  msa Malay
  fil Filipino
  mya Burmese
  khm Khmer
  lao Lao
  swa Swahili
  afr Afrikaans
  zul Zulu
  amh Amharic
  hau Hausa
  yor Yoruba
  ibo Igbo

GitHub folder: https://github.com/madx131/MadLauncher/tree/main/lang

Machine-translated packs are a starting point — native speakers welcome to improve wording.

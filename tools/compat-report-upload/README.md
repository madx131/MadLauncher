# MAD compatibility report → Drive upload

Reports land in:
https://drive.google.com/drive/folders/15PZAr5Rof_bIIjQb2tkyb7Ey_4lOE1Id

Android cannot write to a Drive folder without Google OAuth or a small bridge.
This Apps Script is that bridge (one-time deploy, ~2 minutes).

## Deploy

1. Open https://script.google.com (same Google account that owns the Drive folder).
2. **New project** → paste [`Code.gs`](Code.gs).
3. **Deploy → New deployment → Web app**
   - Execute as: **Me**
   - Who has access: **Anyone**
4. Copy the `/exec` URL.
5. Put it in either:
   - `app/src/main/assets/compat_drive_upload_url.txt` (single line), or
   - `local.properties`: `compat.upload.url=https://script.google.com/macros/s/.../exec`
6. Rebuild MAD.

## Behavior in the app

**Re-check** (Setup self-test + Settings → Not working) re-probes features, writes a
local report, then POSTs it to this web app so the file appears in the Drive folder.

If the upload URL is missing or the POST fails, MAD falls back to a Drive share sheet
and opens the folder link.

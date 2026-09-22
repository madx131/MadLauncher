/**
 * MAD Launcher — compatibility report → Google Drive uploader
 *
 * Target folder: https://drive.google.com/drive/folders/15PZAr5Rof_bIIjQb2tkyb7Ey_4lOE1Id
 *
 * Deploy once (Google account that owns that folder):
 * 1. https://script.google.com → New project
 * 2. Paste this file as Code.gs
 * 3. Deploy → New deployment → Type: Web app
 *    - Execute as: Me
 *    - Who has access: Anyone
 * 4. Copy the Web app URL
 * 5. Paste it into:
 *    - S:/Madl/app/src/main/assets/compat_drive_upload_url.txt  (one line, no quotes)
 *    - or local.properties: compat.upload.url=https://script.google.com/macros/s/.../exec
 * 6. Rebuild / reinstall MAD
 *
 * Test: POST JSON {"filename":"test.txt","content":"hello","folderId":"15PZAr5Rof_bIIjQb2tkyb7Ey_4lOE1Id"}
 */
var FOLDER_ID = '15PZAr5Rof_bIIjQb2tkyb7Ey_4lOE1Id';

function doPost(e) {
  try {
    var raw = (e && e.postData && e.postData.contents) ? e.postData.contents : '{}';
    var data = JSON.parse(raw);
    var folderId = data.folderId || FOLDER_ID;
    var folder = DriveApp.getFolderById(folderId);
    var filename = sanitizeName_(data.filename || ('mad-compat-' + Date.now() + '.txt'));
    var content = data.content != null ? String(data.content) : '';
    var mime = data.mime || MimeType.PLAIN_TEXT;
    var file = folder.createFile(filename, content, mime);
    var out = {
      ok: true,
      id: file.getId(),
      name: file.getName(),
      url: file.getUrl(),
    };
    return ContentService
      .createTextOutput(JSON.stringify(out))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ ok: false, error: String(err) }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet() {
  return ContentService
    .createTextOutput(JSON.stringify({
      ok: true,
      service: 'mad-compat-drive-upload',
      folderId: FOLDER_ID,
    }))
    .setMimeType(ContentService.MimeType.JSON);
}

function sanitizeName_(name) {
  return String(name).replace(/[\\/:*?"<>|]/g, '_').substring(0, 180);
}

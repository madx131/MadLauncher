# Build a public (no-token) MAD APK and copy it into releases/ for git push.
# Distributor builds keep the local token and SHOW_APK_SHARE=true.
param(
    [string]$MadlRoot = "S:\projects\MadLauncher",
    [switch]$Push
)

$ErrorActionPreference = "Stop"
Set-Location $MadlRoot

Write-Host "Building public APK (MAD_PUBLIC_APK=true)..."
& .\gradlew.bat :app:assembleLauncherDebug -PMAD_PUBLIC_APK=true
if ($LASTEXITCODE -ne 0) { throw "public assemble failed" }

$src = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $src)) { $src = "app\build\outputs\apk\launcher\debug\app-launcher-debug.apk" }
if (-not (Test-Path $src)) { throw "APK not found" }

$gradle = Get-Content "app\build.gradle.kts" -Raw
$vn = if ($gradle -match 'versionName\s*=\s*"([^"]+)"') { $Matches[1] } else { "unknown" }
New-Item -ItemType Directory -Force -Path "releases" | Out-Null
New-Item -ItemType Directory -Force -Path "versions" | Out-Null
$latest = "releases\MAD-launcher-latest.apk"
Copy-Item $src $latest -Force
Get-ChildItem "versions" -Filter "MAD-*-latest.apk" -ErrorAction SilentlyContinue | ForEach-Object {
    $demoted = Join-Path "versions" ($_.Name -replace "-latest\.apk$", ".apk")
    if (Test-Path $demoted) { Remove-Item $demoted -Force }
    Move-Item $_.FullName $demoted -Force
}
Copy-Item $src "versions\MAD-$vn-latest.apk" -Force

@"
# MAD public APKs

- ``MAD-launcher-latest.apk`` — always the newest public build (no distributor token / no Share MAD UI)

Download (repo collaborators, GitHub login required):
https://github.com/madx131/MadLauncher/raw/main/releases/MAD-launcher-latest.apk

To share with people who are not collaborators, use Settings → Extras → Share MAD → Send APK
(the distributor build downloads this public APK with the token and shares the file).

Built with ``-PMAD_PUBLIC_APK=true`` so ``SHOW_APK_SHARE=false``.
Local archive: ``versions\MAD-$vn-latest.apk``.
"@ | Set-Content "releases\README.md" -Encoding UTF8

Write-Host "Wrote $latest and versions\MAD-$vn-latest.apk ($((Get-Item $latest).Length) bytes)"

if ($Push) {
    git add releases/MAD-launcher-latest.apk releases/README.md
    git status --short releases/ versions/
    git commit -m "release: public MAD $vn APK"
    git push origin HEAD
}

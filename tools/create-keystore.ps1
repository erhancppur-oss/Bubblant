# Creates the upload key used to sign Bubblant for Google Play.
# Run once:  powershell -ExecutionPolicy Bypass -File tools\create-keystore.ps1
# BACK UP upload.jks and keystore.properties somewhere safe. They are git-ignored.

$root = Split-Path -Parent $PSScriptRoot
$keytool = "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
$jks = Join-Path $root "upload.jks"
if (Test-Path $jks) { Write-Host "upload.jks already exists - not overwriting."; exit 1 }

$secure = Read-Host "Choose a keystore password (min 6 chars)" -AsSecureString
$pass = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
& $keytool -genkeypair -v -keystore $jks -alias upload -keyalg RSA -keysize 2048 -validity 10000 `
    -storepass $pass -keypass $pass -dname "CN=Bubblant, OU=Games, O=Bubblant, C=TR"

@"
storeFile=upload.jks
storePassword=$pass
keyAlias=upload
keyPassword=$pass
"@ | Out-File -Encoding ascii (Join-Path $root "keystore.properties")

Write-Host "Done. Release builds are now signed with upload.jks."

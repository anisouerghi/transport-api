# data/ — stockage partagé

```text
data/
├── attachments/   ← APP_UPLOAD_PATH
└── qr-codes/      ← APP_QR_STORAGE_PATH
```

Résolu par `SharedStoragePaths` vers la racine multi-module (pas le CWD du module).

```powershell
$env:APP_UPLOAD_PATH = "...\transport-api\data\attachments"
$env:APP_QR_STORAGE_PATH = "...\transport-api\data\qr-codes"
# ou scripts/run-public-api.ps1 + run-admin-api.ps1
```

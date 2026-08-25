# Étape 10 — Checklist non-régression (§7)

> Date : 2026-08-25  
> APIs testées : `public-api:8081` + `admin-api:8082`  
> Front voyageur détecté sur **4200** ; admin front **4300** non démarré pendant les tests

---

## Synthèse

| Domaine | API automatisée | UI navigateur |
|---------|-----------------|---------------|
| Voyageur | **OK** (sauf e-mail SMTP réel / i18n UI) | **À valider manuellement** |
| Admin | **OK** (QR GET OK ; regen 422 si déjà présent) | **À valider manuellement** |
| PJ cross-JVM | **Point d’attention** → chemins partagés corrigés | Redémarrer les 2 API avec scripts |

Verdict étape 10 API : **GO conditionnel** — redémarrer les JVM avec `scripts/run-*.ps1` pour valider view/download PJ, puis cocher les parcours UI.

---

## 1. Voyageur (checklist §7)

| Item | Statut API | Détail |
|------|------------|--------|
| Scan QR / UUID support | **OK** | `GET /supports`, `GET /supports/{uuid}` → 200 |
| Signalement anonyme | **OK** | `POST /signalements` → **201** |
| Signalement authentifié | **OK** | register/login/me/mine + create → **201** |
| Pièces jointes (upload) | **OK** | create avec `files` → **201** ; liste admin voit la PJ |
| Confirmation + e-mail | **Partiel** | create OK ; envoi SMTP réel non vérifié ici |
| Suivi UUID | **OK** | `GET .../follow-up` → **200** |
| Réponses publiques | **OK** | `GET /reponses` → 200 |
| i18n FR/AR/EN | **UI only** | Front 4200 — à cocher manuellement |

---

## 2. Administration (checklist §7)

| Item | Statut API | Détail |
|------|------------|--------|
| Login JWT + `/me` | **OK** | 200 |
| Permissions / rôles | **OK** | 200 |
| Liste / filtres / pagination | **OK** | GET + `POST .../search` → 200 |
| Priorité | **OK** | `PATCH .../priority` → 200 |
| Nature | **OK** | `PATCH .../nature` → 200 |
| Réponses (+ publish) | **OK** | `POST .../replies` → **201** (`sendEmail:false` en test) |
| Supports + QR | **OK*** | `GET .../qr` → **200** ; `POST .../generate-qr` → 422 si déjà généré |
| Stats | **OK** | `/statistics/overview` → 200 |
| Audit logs | **OK** | `POST .../audit-logs/search` → 200 |
| Upload/view PJ admin | **Partiel** | liste 200 ; view/download **422 fichier introuvable** (cwd différents) → **corrigé en config** |

\* Utiliser `transportSupportId` (pas `id`).

---

## 3. Correction stockage partagé (définitive)

**Problème :** `./data` ou `../data` suivaient le *working directory* Maven →  
`public-api/.../data` ≠ `admin-api/.../data` → view/download PJ **422**.

**Solution :** classe `SharedStoragePaths` (module `common`) :

1. Si `APP_UPLOAD_PATH` / `APP_QR_STORAGE_PATH` sont **absolus** → utilisés tels quels  
2. Sinon chemins relatifs (`data/attachments`, `data/qr-codes`) résolus sous :
   - `APP_STORAGE_ROOT` si défini, sinon  
   - racine multi-module détectée (dossier contenant `public-api` + `admin-api` + `common`)

Au démarrage, chaque JVM logue :
`Shared storage root resolved — upload=... | qr=...`  
→ les deux doivent afficher **le même chemin absolu**.

**Variables d'environnement :**

| Variable | Rôle |
|----------|------|
| `APP_UPLOAD_PATH` | PJ (absolu recommandé) |
| `APP_QR_STORAGE_PATH` | images QR |
| `APP_STORAGE_ROOT` | racine repo (optionnel si détection auto) |

**Scripts :** `scripts/run-public-api.ps1` / `run-admin-api.ps1` exportent les 3 variables vers `<repo>/data/...`.

**Action :** redémarrer les 2 API, vérifier les logs, retester upload public → view admin.

---

## 4. Parcours UI manuels restants

À faire dans le navigateur (ne bloque pas le code migration, mais ferme l’étape 10 « totale ») :

### Voyageur (`http://localhost:4200` → proxy 8081)
- [ ] Ouvrir `/report/{uuid}` (QR)
- [ ] Créer signalement + PJ via UI
- [ ] Voir confirmation / référence
- [ ] Suivi + réponses
- [ ] Changer langue FR / AR / EN

### Admin (`http://localhost:4300` → API 8082)
- [ ] Login `admin` / `admin123`
- [ ] Liste / filtres signalements
- [ ] Priorité, nature, réponse + option e-mail
- [ ] Prévisualiser / télécharger PJ (après redémarrage chemins partagés)
- [ ] Régénérer / afficher QR
- [ ] Stats + audit

---

## 5. Prochaine étape (11)

Après validation UI + PJ cross-JVM :
- Livraison `public-api.jar` + `admin-api.jar`
- Conservation Git de `transport-api` (rollback) — suppression physique optionnelle plus tard

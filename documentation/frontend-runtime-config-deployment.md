# Configuration runtime Angular — build et déploiement

> Fronts concernés :
> - **Signalement** : `transport-signalement-frontend`
> - **Admin** : `transport-admin-frontend`

Les deux applications chargent leur configuration **au démarrage** depuis :

```
assets/config/config.json
```

Le fichier est copié dans le build final :

```
dist/<projet>/browser/assets/config/config.json
```

Il peut être **modifié après déploiement** sans rebuild (URL API, locale, etc.).

---

## 1. Contenu de `config.json`

### Signalement

```json
{
  "apiBaseUrl": "http://192.168.1.55:8081",
  "googleClientId": "805628985152-kkg4l131p8jmpi7bek764icsp5ikmso7.apps.googleusercontent.com"
}
```

- **DEV** (`src/assets/config/development/config.json`) : `apiBaseUrl` → `http://localhost:8081` (requis pour OAuth Google en redirection pleine page)
- **PROD** (build) : `src/assets/config/production/config.json` → embarqué dans `dist/.../assets/config/config.json`
- `googleClientId` : public uniquement — voir `documentation/google-oauth-passenger.md`

### Admin

```json
{
  "apiBaseUrl": "http://192.168.1.55:8082",
  "locale": "en",
  "rtl": false
}
```

- **DEV** : `"apiBaseUrl": ""` → proxy vers `:8082`
- **PROD** : valeurs production dans `src/assets/config/production/config.json`

**Aucun secret** ne doit figurer dans ces fichiers (pas de JWT, mot de passe DB, etc.).

**JSON strict** : pas de commentaires (`//` ou `/* */`) — un `config.json` commenté provoque une erreur au chargement (`Unexpected token`).

---

## 2. Chargement côté Angular

| Élément | Fichier |
|---------|---------|
| Service | `src/app/core/config/config.service.ts` |
| Endpoints API | `src/app/core/config/api.config.ts` (inchangé, alimenté par `initializeApiConfig()`) |
| Initialisation | `APP_INITIALIZER` dans `src/app/app.config.ts` |

Le chemin de chargement est **relatif au `base href`** :

```typescript
const CONFIG_ASSET_PATH = 'assets/config/config.json';

function resolveConfigUrl(): string {
  return new URL(CONFIG_ASSET_PATH, document.baseURI).href;
}
```

Cela évite les chemins absolus `/assets/...` qui pointent vers la racine du domaine et cassent un déploiement en sous-répertoire.

---

## 3. Pourquoi `/assets/config/config.json` échoue sous `/sig/`

| Contexte | Requête générée | Résultat |
|----------|-----------------|----------|
| Ancien code (`/assets/...`) + déploiement `/sig/` | `http://serveur/assets/config/config.json` | **404** |
| Fichier statique réel | `http://serveur/sig/assets/config/config.json` | **200** |
| Code corrigé + `--base-href /sig/` | `http://serveur/sig/assets/config/config.json` | **200** |

En **développement** (`base href="/"`, `localhost:4200` ou `4500`), les deux approches fonctionnent.  
En **production sous sous-répertoire**, le `base href` et le chemin relatif sont **obligatoires**.

---

## 4. Commandes de build

### Signalement — déployé sous `/sig/`

```bash
cd transport-signalement-frontend
npm install
npm run build -- --base-href /sig/
```

Artefact : `dist/transport-signalement-frontend/browser/`

### Admin — exemple sous `/admin/`

```bash
cd transport-admin-frontend
npm install
npm run build -- --base-href /admin/
```

Artefact : `dist/transport-admin-frontend/browser/`

### Racine du domaine (`/`)

```bash
npm run build
# ou explicitement :
npm run build -- --base-href /
```

### Développement local

```bash
npm start
```

Proxy DEV inchangé :
- Signalement → `http://127.0.0.1:8081` (`proxy.conf.json`)
- Admin → `http://127.0.0.1:8082` (`proxy.conf.json`)

---

## 5. Procédure de déploiement

1. **Build** avec le `--base-href` correspondant au contexte HTTP du serveur (nginx, Apache, IIS).
2. **Copier** le contenu de `dist/.../browser/` dans le répertoire cible :
   - Signalement test : `/sig/` sur le serveur web
   - Admin test : `/admin/` (ou autre, selon infra)
3. **Vérifier** dans `index.html` déployé :
   ```html
   <base href="/sig/">
   ```
4. **Ajuster** si besoin `assets/config/config.json` sur le serveur (URL API, locale…) **sans rebuild**.
5. **Routing Angular (Signalement)** : Path Location — les QR ouvrent `/sig/report/{uuid}` (fallback Apache `.htaccess`).

#### Fallback SPA Apache / nginx

Requis pour le front voyageur sous `/sig/` (routes `/report/{uuid}`, etc.).

---

## 6. Tests après déploiement

### Signalement (`http://192.168.1.55/sig/`)

- [ ] L'application démarre sans erreur « Impossible de charger config.json »
- [ ] Fichier statique OK : `http://192.168.1.55/sig/assets/config/config.json`
- [ ] Onglet **Network** : la requête config pointe vers `/sig/assets/...`, **pas** `/assets/...` à la racine
- [ ] Appels API OK (`apiBaseUrl` dans `config.json`)
- [ ] Navigation / formulaire signalement OK

### Admin

- [ ] Même checklist avec l'URL et le `--base-href` réels (ex. `/admin/`)
- [ ] Login JWT + navigation back-office OK

---

## 7. Environnement de test `192.168.1.55`

| Composant | URL / port |
|-----------|------------|
| Signalement (front) | `http://192.168.1.55/sig/` |
| Admin (front) | `http://192.168.1.55/admin/` (à adapter) |
| Public API | `http://192.168.1.55:8081` |
| Admin API | `http://192.168.1.55:8082` |
| `APP_QR_BASE_URL` | `http://192.168.1.55/sig/` → QR = `/sig/report/{uuid}` |
| `APP_FRONTEND_PUBLIC_BASE_URL` | `http://192.168.1.55/sig/` → e-mails `/sig/report-followup/{uuid}` |

### Routing Signalement (Path Location)

Le front voyageur utilise le routing Angular classique. Apache sert `index.html` via `.htaccess` pour `/sig/report/{uuid}`.

Aucun rewrite Apache n’est nécessaire pour les QR Codes.

Exemple `config.json` Signalement post-déploiement :

```json
{
  "apiBaseUrl": "http://192.168.1.55:8081"
}
```

Exemple `config.json` Admin :

```json
{
  "apiBaseUrl": "http://192.168.1.55:8082",
  "locale": "en",
  "rtl": false
}
```

Démarrage APIs prod (scripts) :

```powershell
.\scripts\run-public-prod.ps1
.\scripts\run-admin-prod.ps1
```

---

## 8. Point d'attention — i18n (Signalement)

Le chargeur ngx-translate utilise encore un préfixe absolu `/assets/i18n/` dans `app.config.ts`.  
Sous un sous-répertoire (`/sig/`), les traductions peuvent échouer si ce chemin n'est pas corrigé de la même manière que `config.json`.  
Voir `transport-signalement-frontend/documentation/i18n-public.md`.

---

## 9. Fichiers sources (référence)

| Rôle | Signalement | Admin |
|------|-------------|-------|
| Config DEV | `src/assets/config/development/config.json` | idem |
| Config PROD (build) | `src/assets/config/production/config.json` | idem |
| Copie assets | `angular.json` → `assets/config/` | idem |
| Service | `src/app/core/config/config.service.ts` | idem |
| API endpoints | `src/app/core/config/api.config.ts` | idem |

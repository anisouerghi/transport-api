# Étape 8 — Validation Frontend + JWT (non-régression)

> Date : 2026-08-25  
> APIs : `public-api:8081` · `admin-api:8082`  
> `transport-api` : **conservé** (rollback), non utilisé par les fronts après correction

---

## Synthèse

| Domaine | Statut |
|---------|--------|
| Frontend voyageur (config → 8081) | **OK** |
| Frontend Admin (config → 8082) | **OK** |
| JWT voyageur | **OK** |
| JWT Admin | **OK** |
| Isolation croisée 401 | **OK** |
| Endpoints publics smoke | **OK** |
| Endpoints admin smoke | **OK** (audit via `POST /search`) |

**Ne pas enchaîner l’étape 9** tant que ce rapport n’est pas validé.

---

## 1. Frontend voyageur

**Projet :** `C:\Users\ThInKpAd11\Desktop\Vm\Back\transport-signalement-frontend`

| Élément | Avant | Après / constat |
|---------|--------|-----------------|
| `proxy.conf.json` | `localhost:8080` | **`localhost:8081`** |
| `environment.apiBaseUrl` | `''` (relatif + proxy) | inchangé — correct |
| `API_CONFIG.public.*` | `/api/public/...` | inchangé — correct |
| Interceptor JWT | Bearer + localStorage `transtu_passenger_session` | OK |
| Expiration / logout / restore `/me` | présents | OK |
| Secrets | aucun | OK |

### Fonctionnalités (câblage front → API)

| Fonction | Endpoint front | Smoke API |
|----------|----------------|-----------|
| Types de signalement | `GET /api/public/report-types` | **200** |
| Supports / scan UUID | `GET /api/public/supports`, `.../{uuid}` | **200** (liste) |
| Réponses accueil | `GET /api/public/reponses` | **200** |
| Auth register / login / me | `/api/public/auth/*` | **200** register + me |
| Mes signalements | `GET .../signalements/mine` | **401** sans JWT · **200** avec JWT voyageur |
| Suivi UUID | `.../signalements/{uuid}/follow-up` | route OK (**404** UUID inexistant) |
| Création signalement + PJ | `POST /api/public/signalements` (multipart) | câblé front ; smoke multipart non exécuté ici |
| QR | URL front `app.qr.base-url` → page voyageur | config API inchangée |

**Verdict voyageur : OK** (proxy corrigé ; pas de changement métier).

---

## 2. Frontend Admin

**Projet :** `C:\Users\ThInKpAd11\Desktop\Vm\Back\transport-admin-frontend`

| Élément | Avant | Après / constat |
|---------|--------|-----------------|
| `Config.API_LINK` | `http://localhost:8080` | **`http://localhost:8082`** |
| `proxy.conf.json` | `localhost:8080` | **`localhost:8082`** |
| `API_CONFIG.admin.*` | `/api/admin/...` via `API_LINK` | OK |
| Interceptor JWT | Bearer + localStorage `transport_admin_auth` | OK |
| 401 → logout | `error.interceptor` | OK |
| Secrets | aucun | OK |

### Fonctionnalités (câblage + smoke)

| Fonction | Endpoint | Smoke |
|----------|----------|-------|
| Login JWT | `POST /api/auth/login` | **200** |
| `/me` | `GET /api/auth/me` | **200** |
| Rôles / permissions | `/api/admin/roles`, `/permissions` | **200** |
| Status | `/api/admin/status` | **200** |
| Signalements (pagination) | `/api/admin/signalements?page&size` | **200** |
| Natures | `/api/admin/natures` | **200** |
| Supports | `/api/admin/transport-supports` | **200** |
| Statistiques | `/api/admin/statistics/overview` | **200** |
| Dashboard API | `/api/admin/dashboard` | **200** (page Angular dashboard = UI statique) |
| Audit | `POST /api/admin/audit-logs/search` | **200** |
| Réponses / priorité / nature / PJ | services front → mêmes bases URL 8082 | câblage OK ; parcours UI manuel recommandé |

**Verdict Admin : OK** (URL + proxy corrigés).

---

## 3. JWT — séparation stricte

### Configuration API

| JAR | `app.security.accepted-token-types` |
|-----|-------------------------------------|
| `public-api` | `PASSENGER` |
| `admin-api` | `ADMIN` |

Filtre : `JwtAuthenticationFilter` (common) — refuse le type non accepté (contexte non authentifié → **401**).

### Tests croisés

| Cas | Résultat |
|-----|----------|
| JWT Admin → `GET 8081/api/public/signalements/mine` | **401** |
| JWT Admin → `GET 8081/api/admin/status` | **401** |
| JWT Voyageur → `GET 8082/api/admin/status` | **401** |
| JWT Voyageur → `GET 8082/api/auth/me` | **401** |
| Sans JWT → `mine` / admin | **401** |
| `8081` ↛ `/api/admin/**`, `/api/auth/**` | **401** |
| `8082` ↛ `/api/public/**` | **401** |

**Verdict JWT : OK**

---

## 4. Corrections réalisées

1. **Voyageur** — `proxy.conf.json` : 8080 → **8081**
2. **Admin** — `Config.API_LINK` + `proxy.conf.json` : 8080 → **8082**
3. **Sécurité** — `app.security.accepted-token-types` par JAR + filtre JWT
4. **Ports** — `PUBLIC_SERVER_PORT` / `ADMIN_SERVER_PORT` (évite collision si `SERVER_PORT=8080` dans l’environnement Windows)

Aucune modification métier Angular / services hors URL & sécurité.

---

## 5. Problèmes rencontrés

| Problème | Impact | Résolution |
|----------|--------|------------|
| Fronts encore sur 8080 | KO dual-run | Proxy / `API_LINK` |
| `SERVER_PORT=8080` env → admin écoutait 8080 | collision | Ports dédiés + `--server.port` |
| `GET /api/admin/audit-logs` | 500 (pas de GET liste) | Front utilise déjà `POST .../search` — OK |

---

## 6. Actions restantes (hors étape 8 code)

- [ ] Parcours UI manuel voyageur (création signalement + PJ + e-mail) sur 4200 → proxy 8081
- [ ] Parcours UI manuel admin (réponse, priorité, nature, QR, PJ) sur front admin → 8082
- [ ] Vérifier ports Angular : voyageur **4200**, admin idéalement **4300** si les deux tournent (scripts actuels admin aussi sur 4200 par défaut)
- [ ] Étape 9 : validation finale / dépréciation progressive `transport-api` (**après votre GO**) — garder le module Git pour rollback

---

## 7. Commandes de référence

```bash
# APIs
mvn -pl admin-api -am spring-boot:run
mvn -pl public-api -am spring-boot:run

# Fronts
cd transport-signalement-frontend && npm start          # :4200 → proxy :8081
cd transport-admin-frontend && npm start                # pointer API_LINK :8082
# si conflit de port front : ng serve --port 4300
```

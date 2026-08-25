# Étape 9 — Dépréciation progressive de `transport-api`

> Date : 2026-08-25  
> Prérequis : étapes 5–8 validées (2 JAR + fronts + JWT)

---

## Objectif

```text
transport-api :8080
        ↓
validation : plus aucune utilisation
        ↓
désactivation démarrage (skip Boot)
        ↓
module CONSERVÉ dans Git (rollback)
```

**Pas de suppression** du dossier / module Maven (étape 11 ultérieure).

---

## Vérifications « usage zéro » de 8080

| Contrôle | Résultat |
|----------|----------|
| Front voyageur `proxy.conf.json` | → **8081** |
| Front admin `API_LINK` + proxy | → **8082** |
| `GET localhost:8080` | **DOWN** (attendu) |
| `GET localhost:8081/api/public/report-types` | **200** |
| `POST localhost:8082/api/auth/login` | **200** |

---

## Actions réalisées

1. `transport-api/pom.xml` : `spring-boot-maven-plugin` → **`skip=true`** (plus de `spring-boot:run` accidentel)
2. Description Maven + `transport-api/README.md` : procédure de rollback documentée
3. READMEs voyageur / public-api / admin front alignés sur 8081 / 8082
4. Admin front : `npm start` sur port **4300** (évite conflit avec voyageur 4200)
5. Module **toujours** listé dans le parent Maven (compilable pour rollback)

---

## Rollback (si besoin)

1. Remettre `<skip>false</skip>` dans `transport-api/pom.xml`
2. Restaurer le code métier depuis Git si la coquille actuelle ne suffit pas
3. `mvn -pl transport-api -am spring-boot:run` → :8080  
4. Re-pointer temporairement les fronts vers 8080 **uniquement** pour diagnostic

---

## Hors scope (étape 10 / 11)

- [ ] Parcours UI manuel complets (création signalement, réponses admin, e-mail, QR)
- [ ] Recette / prod avec les 2 JAR
- [ ] Suppression physique du module `transport-api` du reactor Maven

---

## Architecture cible (état actuel)

```text
public-api  :8081  ──► common ──► MySQL
admin-api   :8082  ──► common ──► MySQL
transport-api      ──► présent en Git, non démarré
```

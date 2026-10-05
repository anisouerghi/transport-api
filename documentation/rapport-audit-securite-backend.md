# Rapport d'audit de sécurité backend

Date : 25 septembre 2026

## 1. Périmètre

L'audit a porté sur les modules backend suivants :

- `common` : sécurité JWT, stockage des fichiers et services partagés ;
- `public-api` : API voyageur, CORS et configuration SMTP ;
- `admin-api` : API d'administration, bootstrap du compte administrateur, CORS et SMTP ;
- `transport-api` : module monolithique historique, notamment JWT et SMTP ;
- scripts et documentation de démarrage.

L'objectif était d'identifier les secrets exposés, les contrôles d'accès insuffisants et les risques de lecture de fichiers hors des répertoires autorisés.

## 2. Synthèse des corrections

| Risque | Gravité | État |
|---|---:|---|
| Secret JWT connu fourni par défaut | Critique | Corrigé |
| Identifiants SMTP présents dans les propriétés | Critique | Corrigé dans les sources |
| Mot de passe administrateur connu `admin123` | Critique | Corrigé |
| Lecture de QR potentiellement hors du répertoire de stockage | Élevée | Corrigé |
| CORS avec jokers de ports localhost et credentials | Élevée | Corrigé |
| Debug SMTP actif dans les logs | Moyenne | Corrigé |
| Documentation et exemple de secrets obsolètes | Moyenne | Corrigé |

## 3. Détail des vulnérabilités corrigées

### 3.1 Secret JWT par défaut

**Problème**

Les applications utilisaient une valeur de secours connue pour `app.security.jwt.secret`. Toute instance démarrée sans variable d'environnement pouvait donc signer et accepter des tokens générés avec un secret public.

**Correction**

Les propriétés utilisent maintenant une valeur vide par défaut :

```properties
app.security.jwt.secret=${JWT_SECRET:}
```

`JwtService` refuse également une valeur absente ou vide et impose une clé d'au moins 256 bits, soit 32 octets ou un équivalent Base64.

**Fichiers concernés**

- `common/src/main/java/com/transport/reporting/security/JwtService.java`
- `admin-api/src/main/resources/application.properties`
- `public-api/src/main/resources/application.properties`
- `transport-api/src/main/resources/application.properties`

**Effet**

Une instance sans `JWT_SECRET` ne démarre pas avec une sécurité JWT dégradée.

### 3.2 Identifiants SMTP versionnés

**Problème**

Des mots de passe SMTP apparaissaient en clair dans les fichiers `application.properties` et pouvaient être récupérés dans l'historique Git ou les artefacts de déploiement.

**Correction**

Les identifiants sont désormais injectés par variables d'environnement :

```properties
spring.mail.username=${SMTP_USERNAME:}
spring.mail.password=${SMTP_PASSWORD:}
```

Les propriétés ont été corrigées dans `admin-api`, `public-api` et `transport-api`, y compris le profil historique `application-transtu-mail.properties`.

**Effet**

Aucun mot de passe SMTP n'est requis dans le dépôt. Le service conserve son contrôle existant et signale l'absence de configuration lorsqu'un envoi est demandé.

### 3.3 Compte administrateur initial prévisible

**Problème**

Le seed créait automatiquement le compte `admin` avec le mot de passe connu `admin123`.

**Correction**

`SecurityDataInitializer` exige maintenant `INITIAL_ADMIN_PASSWORD` uniquement lors de la création initiale du compte. Si le compte existe déjà, son mot de passe n'est pas remplacé automatiquement.

```properties
app.security.initial-admin-password=${INITIAL_ADMIN_PASSWORD:}
```

Si aucun mot de passe initial n'est fourni lors d'une création, le démarrage échoue explicitement.

**Fichier concerné**

- `admin-api/src/main/java/com/transport/reporting/config/SecurityDataInitializer.java`

**Effet**

Il n'existe plus de compte administrateur avec un mot de passe universel connu.

### 3.4 Lecture de QR hors de la racine autorisée

**Problème**

`QrCodeService.readQrImage` utilisait directement le chemin enregistré en base. Le contrôle de la racine de stockage n'était appliqué qu'au chemin de repli, ce qui pouvait permettre une lecture arbitraire si une valeur de chemin non fiable était introduite en base.

**Correction**

Le chemin est maintenant :

1. converti en chemin absolu ;
2. normalisé ;
3. vérifié comme descendant de `sharedStoragePaths.qrRoot()` ;
4. vérifié comme fichier régulier ;
5. remplacé par un chemin de repli dans la même racine si nécessaire.

**Fichier concerné**

- `common/src/main/java/com/transport/reporting/service/QrCodeService.java`

**Effet**

Les séquences de traversal et les chemins extérieurs au répertoire QR partagé sont refusés.

### 3.5 CORS trop permissif

**Problème**

Les configurations ajoutaient automatiquement les motifs `http://localhost:*` et `http://127.0.0.1:*` tout en autorisant les credentials. Une application web locale exécutée sur un port contrôlé par un tiers pouvait alors effectuer des requêtes avec le contexte d'authentification du navigateur.

**Correction**

Les origines sont désormais comparées exactement aux valeurs de `CORS_ALLOWED_ORIGINS`. Les jokers de port automatiques ont été supprimés dans les deux chaînes Spring Security.

**Fichiers concernés**

- `admin-api/src/main/java/com/transport/reporting/security/AdminSecurityConfig.java`
- `public-api/src/main/java/com/transport/reporting/security/PublicSecurityConfig.java`

**Effet**

Avec `allowCredentials=true`, seules les origines explicitement déclarées peuvent accéder aux API depuis un navigateur.

### 3.6 Debug SMTP

**Problème**

`spring.mail.properties.mail.debug=true` pouvait produire des logs SMTP trop détaillés et exposer des informations opérationnelles sensibles.

**Correction**

Le debug SMTP est désactivé dans les modules concernés :

```properties
spring.mail.properties.mail.debug=false
```

## 4. Documentation et scripts mis à jour

Les éléments suivants ont été alignés avec la nouvelle politique :

- `documentation/security.md` : prérequis de secrets, CORS et stockage ;
- `documentation/DOCUMENTATION_PRODUCTION.md` : recommandations SMTP/JWT ;
- `documentation/init-database-prod.sql` : suppression de la référence à `admin123` ;
- `documentation/etape10-non-regression-checklist.md` : connexion avec le mot de passe initial injecté ;
- `scripts/secrets.local.ps1.example` : exemple sans secret réel et avec les nouvelles variables ;
- `scripts/runtime-config-display.ps1` : JWT indiqué comme obligatoire.

## 5. Configuration requise au démarrage

Avant de démarrer le backend, configurer au minimum :

```powershell
$env:JWT_SECRET = "<secret-aleatoire-d-au-moins-32-octets>"
$env:INITIAL_ADMIN_PASSWORD = "<mot-de-passe-fort>"
$env:SMTP_USERNAME = "<compte-smtp>"
$env:SMTP_PASSWORD = "<mot-de-passe-smtp>"
```

Selon les fonctionnalités utilisées, configurer aussi :

- `GOOGLE_CLIENT_SECRET` pour Google OAuth ;
- `CLOUDFLARE_SECRET_KEY` pour Turnstile ;
- `CORS_ALLOWED_ORIGINS` avec une liste exacte d'origines autorisées.

Les secrets doivent être fournis par le gestionnaire de secrets de l'environnement de production, et non par Git.

## 6. Validation technique

La compilation et le packaging des modules corrigés ont réussi avec :

```text
mvn -pl common,admin-api,public-api -am package -DskipTests
BUILD SUCCESS
```

Les vérifications statiques des fichiers Java touchés ne signalent aucune erreur.

L'exécution complète des tests a été tentée, mais Mockito/Byte Buddy échoue avec la JVM Java 26 utilisée dans l'environnement. La version embarquée de Byte Buddy supporte officiellement jusqu'à Java 23. Cet échec est un problème de compatibilité de l'environnement de test, pas une erreur de compilation des corrections.

## 7. Actions obligatoires après l'audit

1. Révoquer et régénérer le secret SMTP exposé précédemment.
2. Révoquer et régénérer le secret Google OAuth exposé précédemment.
3. Générer un nouveau `JWT_SECRET` aléatoire pour chaque environnement sensible.
4. Définir un `INITIAL_ADMIN_PASSWORD` fort puis changer le mot de passe après le premier accès si la politique interne l'exige.
5. Vérifier l'historique Git et supprimer les secrets exposés de l'historique selon la procédure de l'hébergeur.
6. Utiliser Java 17 à 23, ou mettre à jour Mockito/Byte Buddy, pour réactiver la suite de tests complète.

## 8. Limites et suites recommandées

Cette passe a corrigé les vulnérabilités identifiées dans les chemins de configuration, d'authentification JWT, de CORS, de bootstrap admin et de stockage de fichiers. Une seconde passe de sécurité devrait compléter le travail avec :

- un scan des dépendances OWASP et des CVE Maven ;
- des tests d'intégration d'autorisation pour chaque endpoint admin ;
- des tests de traversal sur QR et pièces jointes ;
- des tests de limitation OTP par IP, e-mail et appareil ;
- un test DAST sur les API déployées ;
- une vérification de la restriction Swagger/Actuator en production.

# Sécurité Administration — RBAC dynamique (matrice)

## Principe

## Configuration obligatoire des secrets

Les secrets ne doivent jamais avoir de valeur de secours dans les fichiers de configuration versionnes.
Avant un demarrage, fournir au minimum :

- `JWT_SECRET` : secret aleatoire d'au moins 32 octets (ou equivalent Base64) ;
- `INITIAL_ADMIN_PASSWORD` : utilise uniquement lors de la creation initiale du compte `admin` ;
- `SMTP_USERNAME` et `SMTP_PASSWORD` si l'envoi d'e-mails est active ;
- `GOOGLE_CLIENT_SECRET` et `CLOUDFLARE_SECRET_KEY` si les fonctions correspondantes sont activees.

Le compte initial n'est plus cree avec un mot de passe connu par defaut. Les secrets qui ont ete
commits ou places dans un fichier local expose doivent etre immediatement revoques et remplaces.

Les origines CORS sont comparees exactement aux valeurs de `CORS_ALLOWED_ORIGINS`. Les jokers de
ports localhost ne sont pas autorises par defaut, y compris avec `allowCredentials=true`.

Les fichiers QR et les pieces jointes sont lus uniquement sous leurs repertoires de stockage dedies.
Un chemin en base qui sort de cette racine est refuse.

```
AppUser ──< user_role >── Role ──< role_permission >── Permission
```

- Un utilisateur a **N rôles**
- Un rôle a **N permissions**
- Une permission = **module × action** (ex. `REPORT` × `REPLY` → code `REPORT_REPLY`)

Aucune liste de permissions n’est figée dans le code métier : elles vivent en base et sont affectées via la matrice rôles.

## Modèle Permission

| Champ | Exemple |
|-------|---------|
| `code` | `REPORT_VIEW` |
| `module_code` | `REPORT` |
| `module_label` | Signalements |
| `action_code` | `VIEW` |
| `label` | Consulter les signalements |
| `description` | … |
| `active` | true |

Actions supportées (selon le module) :  
`VIEW`, `ADD`, `EDIT`, `DELETE`, `SEARCH`, `EXPORT`, `PRINT`, `REPLY`, `ASSIGN`, `CLOSE`, `ACTIVATE`, `DEACTIVATE`, `UPDATE_PRIORITY`, `ASSIGN_NATURE`

## Modules récents

| Module | Codes | Menu |
|--------|-------|------|
| `PASSENGER` | `PASSENGER_VIEW`, `PASSENGER_SEARCH`, `PASSENGER_ACTIVATE`, `PASSENGER_DEACTIVATE` | Voyageurs → `/passengers` |
| `REPORT_STATISTICS` | `REPORT_STATISTICS_VIEW` | Rapports & Statistiques → `/statistics` |
| `REPORT` | … + `REPORT_UPDATE_PRIORITY`, `REPORT_ASSIGN_NATURE` | Priorité / nature des signalements |
| `NATURE` | `NATURE_VIEW`, `ADD`, `EDIT`, `DELETE`, `SEARCH`, `ACTIVATE`, `DEACTIVATE` | Natures → `/report-natures` |

Rôles seed (création initiale) :

- **ADMIN** : toutes les permissions (resynchronisé au démarrage)
- **AGENT** : signalements (+ `REPORT_ASSIGN_NATURE`, `NATURE_VIEW`) / supports / voyageurs
- **RESPONSABLE** : natures (sauf DELETE) + `REPORT_ASSIGN_NATURE` + voyageurs + stats

## Contrôle API (centralisé)

Bean Spring `@perm` (`PermissionChecker`) :

```java
@PreAuthorize("@perm.has('USER', 'ADD')")
@PreAuthorize("@perm.hasAny('REPORT', 'SEARCH', 'VIEW')")
@PreAuthorize("@perm.has('PASSENGER', 'ACTIVATE')")
@PreAuthorize("@perm.has('REPORT_STATISTICS', 'VIEW')")
```

Les authorities JWT / SecurityContext sont les **codes** chargés dynamiquement depuis la base à chaque requête.

## Matrice rôles

`GET /api/admin/permissions/matrix` → modules × actions + `permissionId` pour les checkboxes.

UI admin : écran **Rôles** → modal avec grille (une ligne = module, une colonne = action).

## Menus dynamiques

Table `app_menu` : chaque entrée a un `permission_code` (`PASSENGER_VIEW`, `REPORT_STATISTICS_VIEW`, …).  
Au login, seuls les menus dont la permission est présente sont renvoyés.  
Le seed ajoute les menus manquants sans écraser les existants.

## Frontend

- Login retourne `permissions[]` + `menus[]` (filtrés DB)
- Routes : `data.permission` = `*_VIEW`
- Boutons : `*appHasPermission="'PASSENGER_ACTIVATE'"`
- Helper : `auth.can('USER', 'ADD')`

## Compte seed

Le compte `admin` initial reçoit le mot de passe fourni par `INITIAL_ADMIN_PASSWORD`.

## Extension

1. Insérer une ligne dans `permission` (module + action + code) — ou laisser le seed l’assurer
2. Affecter via la matrice rôle (ou `role_permission`)
3. Protéger l’API avec `@perm.has('MODULE', 'ACTION')`
4. Masquer le bouton FE avec `*appHasPermission="'MODULE_ACTION'"`
5. Optionnel : entrée `app_menu` liée à `MODULE_VIEW`

Pas de redéploiement de listes hardcodées de permissions.

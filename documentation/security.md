# Sécurité Administration — RBAC dynamique (matrice)

## Principe

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
`VIEW`, `ADD`, `EDIT`, `DELETE`, `SEARCH`, `EXPORT`, `PRINT`, `REPLY`, `ASSIGN`, `CLOSE`, `ACTIVATE`, `DEACTIVATE`, `UPDATE_PRIORITY`

## Modules récents

| Module | Codes | Menu |
|--------|-------|------|
| `PASSENGER` | `PASSENGER_VIEW`, `PASSENGER_SEARCH`, `PASSENGER_ACTIVATE`, `PASSENGER_DEACTIVATE` | Voyageurs → `/passengers` |
| `REPORT_STATISTICS` | `REPORT_STATISTICS_VIEW` | Rapports & Statistiques → `/statistics` |
| `REPORT` | … + `REPORT_UPDATE_PRIORITY` | Priorité interne des signalements |

Rôles seed (création initiale) :

- **ADMIN** : toutes les permissions (resynchronisé au démarrage)
- **AGENT** : consultation voyageurs (+ signalements / supports)
- **RESPONSABLE** : voyageurs (activer/désactiver) + `REPORT_STATISTICS_VIEW`

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

`admin` / `admin123` — rôle `ADMIN` (toutes les permissions de la matrice)

## Extension

1. Insérer une ligne dans `permission` (module + action + code) — ou laisser le seed l’assurer
2. Affecter via la matrice rôle (ou `role_permission`)
3. Protéger l’API avec `@perm.has('MODULE', 'ACTION')`
4. Masquer le bouton FE avec `*appHasPermission="'MODULE_ACTION'"`
5. Optionnel : entrée `app_menu` liée à `MODULE_VIEW`

Pas de redéploiement de listes hardcodées de permissions.

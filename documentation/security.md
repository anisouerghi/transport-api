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
`VIEW`, `ADD`, `EDIT`, `DELETE`, `SEARCH`, `EXPORT`, `PRINT`, `REPLY`, `ASSIGN`, `CLOSE`, `ACTIVATE`, `DEACTIVATE`

## Contrôle API (centralisé)

Bean Spring `@perm` (`PermissionChecker`) :

```java
@PreAuthorize("@perm.has('USER', 'ADD')")
@PreAuthorize("@perm.hasAny('REPORT', 'SEARCH', 'VIEW')")
```

Les authorities JWT / SecurityContext sont les **codes** chargés dynamiquement depuis la base à chaque requête.

## Matrice rôles

`GET /api/admin/permissions/matrix` → modules × actions + `permissionId` pour les checkboxes.

UI admin : écran **Rôles** → modal avec grille (une ligne = module, une colonne = action).

## Frontend

- Login retourne `permissions[]` + `menus[]` (filtrés DB)
- Routes : `data.permission` = `*_VIEW`
- Boutons : `*appHasPermission="'USER_ADD'"`
- Helper : `auth.can('USER', 'ADD')`

## Compte seed

`admin` / `admin123` — rôle `ADMIN` (toutes les permissions de la matrice)

## Extension

1. Insérer une ligne dans `permission` (module + action + code)
2. Affecter via la matrice rôle (ou `role_permission`)
3. Protéger l’API avec `@perm.has('MODULE', 'ACTION')`
4. Masquer le bouton FE avec `*appHasPermission="'MODULE_ACTION'"`

Pas de redéploiement de listes hardcodées de permissions.

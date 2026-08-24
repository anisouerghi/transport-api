# Natures des signalements

## Objectif

Classifier chaque signalement selon une **nature métier** configurable (propreté, sécurité, etc.) afin de préparer des statistiques fiables. La nature est une **référence** (`report_nature`), pas une chaîne libre.

## Modèle

### Table `report_nature`

| Colonne | Description |
|---------|-------------|
| `report_nature_id` | PK |
| `code` | Unique (ex. `PROPRETE`) |
| `label` | Libellé affiché |
| `description` | Texte optionnel |
| `active` | Actif / inactif |
| `created_at` / `updated_at` | Horodatage |

### Relation `report.nature_id` → `report_nature`

- Nullable = **Non classé**
- FK pour intégrité et agrégations futures

## API admin

Base : `/api/admin/natures`

| Méthode | Chemin | Permission |
|---------|--------|------------|
| GET | `/` | `NATURE_VIEW` |
| GET | `/active` | `NATURE_VIEW` ou `REPORT_ASSIGN_NATURE` ou `REPORT_VIEW` |
| GET | `/{id}` | `NATURE_VIEW` |
| POST | `/search` | `NATURE_SEARCH` / `NATURE_VIEW` |
| POST | `/` | `NATURE_ADD` |
| PUT | `/{id}` | `NATURE_EDIT` |
| PATCH | `/{id}/activate` | `NATURE_ACTIVATE` |
| PATCH | `/{id}/deactivate` | `NATURE_DEACTIVATE` |
| DELETE | `/{id}` | `NATURE_DELETE` (refus si signalements rattachés) |

### Affectation sur un signalement

`PATCH /api/admin/signalements/{id}/nature`  
Body : `{ "reportNatureId": 3 }` (null pour déclasser)  
Permission : `REPORT_ASSIGN_NATURE`

Audit : `AuditAction.NATURE_CHANGE` + entrée `report_history`.

### Filtre recherche signalements

Dans `POST /api/admin/signalements/search` :

- `natureId` : filtre par nature
- `uncategorized: true` : nature absente (Non classé)

Réponse signalement enrichie : `natureId`, `natureCode`, `natureLabel`.

## Permissions

| Code | Usage |
|------|--------|
| `NATURE_VIEW` | Menu / CRUD lecture |
| `NATURE_ADD` / `EDIT` / `DELETE` | CRUD |
| `NATURE_ACTIVATE` / `DEACTIVATE` | Activation |
| `NATURE_SEARCH` | Recherche paginée |
| `REPORT_ASSIGN_NATURE` | Affecter une nature |

Menu admin : **Natures des signalements** → `/report-natures` (`NATURE_VIEW`).

## Seed

Natures créées au démarrage si absentes : AGRESSION, PROPRETE, SECURITE, MAINTENANCE, INFORMATION, COMPORTEMENT, RETARD, ACCESSIBILITE, AUTRE.

## Frontend admin

- CRUD : `/report-natures`
- Liste signalements `/reports` : colonne Nature, filtre (dont Non classé), action **Nature** (modal)

## Statistiques (préparation)

Le modèle FK + codes stables permettent ensuite : volumes par nature, évolution temporelle, croisement support / unité / statut / réponses — sans module stats dédié dans cette livraison.

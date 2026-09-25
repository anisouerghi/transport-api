# I18n paramètres métier FR / AR / EN (V1)

## Objectif

Afficher les libellés de référentiels (`support_type`, `report_type`, `report_nature`, `report_status`) et les priorités selon la langue voyageur (`Accept-Language`), sans casser le contrat JSON (`label`).

## Migration SQL (obligatoire avant déploiement JAR PROD)

Fichier : [`migration-i18n-param-labels.sql`](./migration-i18n-param-labels.sql)

```bash
# Backup puis :
mysql -u <USER> -p signalement < documentation/migration-i18n-param-labels.sql
```

Colonnes ajoutées (par table) : `label_fr`, `label_ar`, `label_en`.  
La colonne historique `label` est **conservée** (miroir FR).

## API

| Header | Effet |
|--------|--------|
| `Accept-Language: fr` | `label` = français |
| `Accept-Language: ar` | `label` = arabe (fallback FR si vide) |
| `Accept-Language: en` | `label` = anglais (fallback FR si vide) |

Réponses catalogue (admin) exposent aussi `labelFr`, `labelAr`, `labelEn` pour l’édition.

Priorités : `GET /api/admin/signalements/priorities` localise le champ `label`.

## Fronts

- **Voyageur** : interceptor `Accept-Language` = langue `LanguageService` ; re-fetch au changement de langue.
- **Admin** : interceptor fixe `Accept-Language: fr` ; formulaires types support / report-type : Label FR + AR + EN.

## Hors V1

`district`, menus/rôles/permissions, `transport_support.label` (instance), textes saisis voyageur/agent.

## Rollback soft

Voir commentaires en fin de `migration-i18n-param-labels.sql` (resync `label = label_fr` ; DROP colonnes uniquement après validation).

## Checklist tests

- [ ] FR / AR / EN : types, supports, statuts, natures (API)
- [ ] Changement dynamique langue voyageur
- [ ] Anciens signalements (FK inchangées)
- [ ] Fallback `label_ar` vide → FR
- [ ] Admin CRUD labels 3 langues
- [ ] Auth / OTP / Google inchangés

# Avis voyageur après clôture

Un signalement clôturé (`RESOLVED` ou `CLOSED`) peut recevoir un seul avis sur la réponse TRANSTU : `SATISFAIT`, `MOYENNEMENT_SATISFAIT` ou `NON_SATISFAIT`, avec un commentaire facultatif de 1 000 caractères au plus. Le texte est stocké sans balises. L'avis n'est pas une note de l'agent.

## Accès

- Compte avec suivi (mot de passe ou Google) : le jeton du propriétaire est obligatoire. L'UUID du lien ne suffit pas.
- Dossier sans compte : le lien de suivi UUID, déjà utilisé pour la consultation, autorise un seul envoi. La recherche par référence ne propose l'avis qu'au propriétaire connecté.
- Un second envoi, y compris simultané, est refusé (`SATISFACTION_ALREADY_EXISTS`).
- Un signalement clôturé ne peut plus être modifié (réponse, statut, type, priorité, nature). L'avis déjà enregistré est conservé.

## API

- `POST /api/public/signalements/{uuid}/satisfaction`
- Le suivi `GET .../follow-up` ajoute `satisfaction`, `canSubmitSatisfaction` et `satisfactionLoginRequired`.
- Admin, permission `REPORT_STATISTICS_VIEW` :
  - `GET /api/admin/statistics/satisfaction`
  - `GET /api/admin/statistics/satisfaction/comments`
- Filtres : `from`, `to` (dates UTC de clôture), `reportTypeId`, `transportSupportId`.
- Les pourcentages de niveau portent sur les avis reçus. Le taux de participation a pour dénominateur les signalements clôturés du filtre. Il n'existe pas de dimension station distincte du support.

La table `report_satisfaction` est créée au démarrage par `DatabaseSchemaPatcher`, et figure dans `schema.sql` ainsi que `documentation/init-database-prod.sql`.

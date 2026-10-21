# Notifications plateforme (cloche voyageur)

## Objectif

Lorsqu’un agent change le statut public d’un signalement (ou demande un complément),
le voyageur **authentifié** reçoit une notification dans la cloche du header, en plus
de l’e-mail éventuel. Les voyageurs anonymes ne reçoivent pas de notification de compte.

## Déclenchement

Point unique : `ReplyService.create` (après enregistrement de la réponse agent).

| Condition | Effet |
|-----------|--------|
| Transition de statut **public** (`notifyStatusTransition`) | Notif `STATUS_PUBLIC_CHANGED` |
| Demande de complément (`notifyComplementMessage`) | Notif `COMPLEMENT_REQUEST` |
| Compte sans `password_hash` / `google_subject` | Aucune notif |
| `notify_platform = false` | Aucune notif (e-mail inchangé) |

Anti-doublon : clé `dedup_key` unique par passager
(`STATUS:{reportId}:{statutPublic}` ou `COMPLEMENT:reply:{replyId}`).

Le libellé affiché vient uniquement de `report_status.statut_public`.

## API (JWT voyageur obligatoire)

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `GET` | `/api/public/notifications/inbox` | `{ unreadCount, items[≤20] }` |
| `POST` | `/api/public/notifications/read` | `{ markAll, ids }` — scoped au JWT |

Le `passengerId` est lu depuis le token, jamais depuis le client.

## Frontend

- Service : `InboxNotificationService` (polling unique, RxJS)
- Config runtime (`assets/config/config.json`) :
  - `notificationsPollingEnabled` (défaut true)
  - `notificationsPollingIntervalMs` (défaut 30000)
- Pause si onglet masqué / hors ligne ; arrêt à la déconnexion
- Clic → `/report-followup/{uuid}`

## Schéma

Table `passenger_notification` — voir `migration-passenger-notification.sql`
et `DatabaseSchemaPatcher.ensurePassengerNotificationTable`.

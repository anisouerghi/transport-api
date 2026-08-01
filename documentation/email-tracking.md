# E-mails & suivi public sécurisé

## Propriétés

| Propriété | Rôle | Exemple |
|-----------|------|---------|
| `app.frontend.public-base-url` | Base URL frontend voyageur (liens e-mail) | `http://localhost:4200` |
| `spring.mail.*` | SMTP | host, port, username, password, from |

Lien de suivi construit : `{public-base-url}/suivi/{reportUuid}`

## Envoi après réponse agent

`ReplyService.create` :

1. Persiste la réponse (`public_response`, `email_sent`)
2. Si `sendEmail=true` **et** e-mail voyageur valide → `EmailService` + HTML `ReplyEmailComposer`
3. Audit `EMAIL_SEND` (succès / échec, destinataire, erreur)

Réponse API (`ApiResponse`) :

| Champ | Signification |
|-------|----------------|
| `success` | `true` si réponse OK **et** e-mail OK (si demandé) |
| `message` | Message utilisateur (succès ou échec e-mail explicite) |
| `errorCode` | Ex. `EMAIL_SMTP_AUTH`, `EMAIL_SMTP_TIMEOUT`, `EMAIL_CONFIG_INCOMPLETE` |
| `data` | `ReplyResponse` (toujours présente si la réponse a été enregistrée) |

Les exceptions SMTP ne remontent jamais brutes au client.

Objet : `Réponse à votre signalement – TRANSTU`

Logo : embarqué en inline CID (`classpath:email/transtu_logo.png` → `cid:transtu-logo`),
pas via une URL `localhost` (sinon invisible dans les clients mail).

## Suivi public

- `GET /api/public/suivi/{uuid}` → `PublicReportTrackingResponse`
- Accès par **UUID** uniquement (pas la référence métier dans l’URL)
- Réponses filtrées : `public_response = true` uniquement
- Pas d’IDs internes, agents, priorité

## Frontend

- Route : `/suivi/:uuid`
- Confirmation : lien vers `/suivi/{uuid}`

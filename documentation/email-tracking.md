# E-mails & suivi public sécurisé

## Flux

```text
Création signalement → E-mail de confirmation avec lien UUID (si e-mail voyageur)
        ↓
Traitement Admin → Réponse visible à l'auteur ?
        ↓ (oui + e-mail voyageur)
    E-mail de réponse avec le même lien → /report-followup/{uuid}
        ↓
Suivi public (réponses visibles uniquement)
```

## Propriétés

| Propriété | Rôle | Exemple |
|-----------|------|---------|
| `app.frontend.public-base-url` | Base URL frontend voyageur (liens e-mail) | `http://localhost:4200` |
| `SMTP_PASSWORD` | Mot de passe SMTP (variable d'environnement) | — |
| `spring.mail.*` | SMTP | host, port, username, from |

Lien de suivi : `{public-base-url}/report-followup/{reportUuid}`

## Envoi après réponse agent

`ReplyService.create` :

1. Persiste la réponse (`public_response`, `email_sent`)
2. Si `publicResponse=true` **et** `sendEmail=true` **et** e-mail voyageur valide → envoi SMTP
3. Si `publicResponse=false` → pas d'e-mail (réponse interne)
4. La réponse est **toujours** enregistrée même si l'e-mail échoue
5. Audit `EMAIL_SEND` (succès / échec)

`mailSender.send(...)` réussi = le serveur SMTP TRANSTU a **accepté** le message.
Cela n'est pas une garantie d'arrivée en boîte (indésirables, SPF/DKIM, ou destinataire différent).
Le destinataire est **l'e-mail du voyageur** sur le signalement, pas celui de l'agent.

## Dépannage : e-mail non reçu (ni boîte ni indésirables)

1. **Mot de passe SMTP tronqué** — ne jamais écrire `${SMTP_PASSWORD:secret:avec:deux-points}` : Spring coupe au premier `:`. Utiliser une valeur directe ou la variable d'environnement `SMTP_PASSWORD` seule.
2. **Vérifier le destinataire** — l'e-mail part vers l'adresse du **voyageur** du signalement (ex. `anis.benezzin@gmail.com`), pas vers l'agent connecté.
3. **Logs API** — au démarrage : `passwordLength=16` pour `f6SZGbF:4xxFQ` (si `11`, le mot de passe est tronqué). Après envoi : `Message-ID: <...>` pour tracer côté serveur mail.
4. **File d'attente serveur** — demander à l'admin `mail.transtu.tn` de vérifier la queue Postfix et les bounces pour le Message-ID ou le destinataire Gmail.
5. **SPF / DKIM / DMARC** — Gmail peut rejeter silencieusement si le domaine `transtu.tn` n'authentifie pas correctement les envois depuis `mail.transtu.tn`.
6. **Profil dev** — `application-dev.properties` utilise le port **587** + STARTTLS + `anis.ouerghi@transtu.tn` comme expéditeur de test.

## API suivi sécurisé

| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/public/signalements/{uuid}/follow-up` | Suivi par UUID (lien e-mail) |
| GET | `/api/public/suivi/{uuid}` | Alias déprécié (compatibilité) |

- Accès par **UUID** uniquement — la référence métier (`SIG-…`) ne permet pas le suivi
- Réponses filtrées : `public_response = true` uniquement
- Pas d'IDs internes, agents, priorité

## Réponse publique de création

`POST /api/public/signalements` retourne la **référence** mais masque l'**UUID** (réservé au lien e-mail).

## Frontend public

| Route | Rôle |
|-------|------|
| `/confirmation` | Référence copiable, pas de lien de suivi direct |
| `/report-followup/:uuid` | Suivi sécurisé (lien e-mail) |
| `/suivi/:uuid` | Redirection vers `/report-followup/:uuid` |

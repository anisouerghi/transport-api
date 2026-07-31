# Pièces jointes des signalements

## Objectif

Permettre au voyageur d'ajouter **0 à 5** fichiers optionnels lors de la création d'un signalement, et à l'agent de les consulter / télécharger depuis l'administration.

L'entité JPA existante `Attachment` est réutilisée sans rupture du modèle.

## Contraintes

| Règle | Valeur |
|-------|--------|
| Formats | JPG, JPEG, PNG, WEBP, PDF |
| Nombre max | 5 fichiers / signalement |
| Taille unitaire | 10 Mo |
| Taille totale | 25 Mo |
| Contrôles | extension + MIME + taille |
| Nom physique | UUID + extension (jamais le nom client) |

## Configuration

```properties
app.upload.path=./data/attachments
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=30MB
```

Profils :

- **dev** : `./data/attachments`
- **prod** : `${APP_UPLOAD_PATH:/var/data/transport/attachments}`

## Création publique (multipart)

`POST /api/public/signalements`  
`Content-Type: multipart/form-data`

| Part | Type | Obligatoire | Description |
|------|------|-------------|-------------|
| `report` | `application/json` | oui | Corps `ReportRequest` |
| `files` | fichiers binaires | non | 0..N fichiers (max 5) |

Exemple côté Angular :

```typescript
const formData = new FormData();
formData.append('report', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
files.forEach((f) => formData.append('files', f, f.name));
```

Flux métier :

1. `FileStorageService.validateBatch(files)`
2. Persistence du `Report`
3. Pour chaque fichier : stockage disque + insert `attachment`
4. Réponse `ReportResponse` enrichie avec `attachments`

## API administration

| Méthode | Endpoint | Rôle |
|---------|----------|------|
| GET | `/api/admin/signalements/{id}` | Détail (+ `attachments` éventuels) |
| GET | `/api/admin/signalements/{id}/attachments` | Liste métadonnées |
| GET | `/api/admin/attachments/{id}/view` | Affichage inline |
| GET | `/api/admin/attachments/{id}/download` | Téléchargement |

## Erreurs

| Situation | HTTP | Origine |
|-----------|------|---------|
| Format / taille métier | 422 | `BusinessException` |
| Multipart trop volumineux (Tomcat) | 413 | `MaxUploadSizeExceededException` |
| Pièce jointe inconnue | 404 | `ResourceNotFoundException` |

## Évolutions futures possibles

- Stockage objet (S3 / MinIO)
- Scan antivirus
- Compression / redimensionnement d'images
- Quota par voyageur

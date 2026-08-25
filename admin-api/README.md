# Admin API (agents / back-office)

Module Spring Boot exécutable — port **8082**.

```bash
# Démarrage normal (pas de DROP schéma)
mvn -pl admin-api -am spring-boot:run

# Première install / reset volontaire (DROP + CREATE) — arrêter public-api avant
mvn -pl admin-api -am spring-boot:run -Dspring-boot.run.profiles=dev,dev-reset
```

- Routes : `/api/auth/**`, `/api/admin/**`
- Dépend uniquement de `common` (pas de `public-api` ni `transport-api`)
- Seeders Java (permissions, menus, admin) : toujours au démarrage, idempotents
- `schema.sql` : uniquement avec le profil `dev-reset`

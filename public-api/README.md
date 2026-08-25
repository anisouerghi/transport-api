# Placeholder — module public-api

Spring Boot exécutable (voyageur).

```bash
mvn -pl public-api -am spring-boot:run
```

Port par défaut : **8081**  
URLs : `/api/public/**`

Ne lance pas `schema.sql` (réservé à `admin-api`, profil `dev-reset`).

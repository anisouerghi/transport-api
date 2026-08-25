# transport-api (DEPRECATED — rollback uniquement)

Coquille du monolithe historique. **Ne plus démarrer en développement ni en production.**

## Utiliser à la place

| API | Port | Commande |
|-----|------|----------|
| Public | 8081 | `mvn -pl public-api -am spring-boot:run` |
| Admin | 8082 | `mvn -pl admin-api -am spring-boot:run` |

## Pourquoi le module reste dans Git

- Rollback rapide si régression majeure sur les 2 JAR
- Historique / comparaison
- `spring-boot:run` est **désactivé** (`skip=true`) pour éviter un démarrage accidentel sur 8080

## Réactiver en rollback (exceptionnel)

1. Dans `transport-api/pom.xml` : `spring-boot-maven-plugin` → `<skip>false</skip>`
2. Restaurer controllers / sécurité depuis l’historique Git si la coquille ne suffit pas
3. `mvn -pl transport-api -am spring-boot:run` (port 8080)

La suppression définitive du module = **étape 11**, après validation prod/recette.

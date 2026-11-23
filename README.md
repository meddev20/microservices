# 🏥 Med-RDV — Plateforme Complète Intégrée (Jalons V0 à V7)

Architecture unifiée de prise de rendez-vous médicaux multi-praticiens avec **Spring Boot 3**, **Spring Cloud**, **Apache Kafka**, **RabbitMQ**, **Resilience4j** et **PostgreSQL**.

Toutes les exigences des **Jalons V0 à V7** (Dr. Salim KHALIL - Fiche de suivi d'évaluation) sont intégrées directement au cœur des microservices du projet.

> 📖 **Dossier de Conception Officiel V0 & V1 :** Voir [CONCEPTION_V0_V1.md](file:///c:/Users/Abdessamad/Desktop/ai_story/rendez-vous%20medcin/CONCEPTION_V0_V1.md) pour les diagrammes de classes Mermaid, l'architecture globale cohérente et les résultats détaillés des tests CRUD.
>
> 🧪 **Lancer les tests V1 CRUD :** `powershell -ExecutionPolicy Bypass -File test-v1-crud.ps1`

---

## 🏛️ Cartographie des Services & Ports

| Service | Port | Rôle & Intégrations Pédagogiques | Jalon Associé |
| :--- | :--- | :--- | :--- |
| **`eureka-server`** | **8761** | Registre de découverte Netflix Eureka (`@EnableEurekaServer`) | **Jalon V2** |
| **`config-server`** | **8888** | Serveur de configuration centralisé (`config-repo/`) | **Jalon V3** |
| **`api-gateway`** | **8080** | Passerelle réactive avec routage dynamique/statique & Rate Limiter Redis | **Jalon V2, V6** |
| **`service-praticiens`** | **8081** | Gestion des praticiens et réservation atomique des créneaux (Optimistic lock) | **Jalon V0, V1, V4, V7** |
| **`service-patients`** | **8082** | Gestion des patients et dossier médical (mise à jour chronologique sans écrasement) | **Jalon V0, V1, V7** |
| **`service-rendezvous`** ⭐ | **8083** | Cœur métier, OpenFeign, Resilience4j (`ResilientRequests`, Cache Caffeine, RateLimiter), Saga Kafka | **Jalon V0, V4, V5, V6, V7** |
| **`service-notification`** | **8084** | Consommateur Kafka & RabbitMQ, envoi asynchrone d'emails via SMTP (MailHog) | **Jalon V5, V7** |
| **`service-batch-analytics`** | **8085** | Jobs `@Scheduled` : rappels J-1 de consultation et export CSV mensuel par spécialité | **Bonus** |

---

## 🧩 Détail des Compétences Jalons Intégrées

0. **Jalon V0 (Conception & Documentation) :**
   - **Architecture générale cohérente :** Découplage microservices, passerelle Spring Cloud Gateway, registre Eureka, Config Server.
   - **Diagramme de classes métier :** Modélisation complète dans `CONCEPTION_V0_V1.md` (`Doctor`, `Availability`, `Patient`, `MedicalRecord`, `Appointment`, enums).
   - **Git et documentation à jour :** Dépôt GitHub maintenu, documentation détaillée avec instructions de test.
1. **Jalon V1 (Implémentation Métier & BDD) :**
   - **Services, couches et BDD isolées :** Architecture 3-tiers (Controller, Service, Repository, Entity, DTO) avec bases séparées.
   - **CRUD et validations métier :** `@Valid`, `@NotBlank`, `@Email`, réservations atomiques, contrôles d'unicité.
   - **Tests API initiaux :** Suite automatisée `test-v1-crud.ps1` validée à 100%.
2. **Jalon V2 (Eureka & Gateway) :**
   - Tous les services sont annotés Eureka Client et s'enregistrent sur `http://localhost:8761/`.
   - `api-gateway` (Port 8080) gère le routage dynamique (`/service-id/**`) et les routes directes (`/api/v1/**`).
3. **Jalon V3 (Config Server) :**
   - Dossier centralisé `config-repo/` contenant les `.properties` servis sur `http://localhost:8888/`.
   - `spring.config.import=optional:configserver:http://localhost:8888/` activé sur tous les services.
4. **Jalon V4 (OpenFeign & Consultation Enrichie) :**
   - `service-rendezvous` valide le patient et le médecin/créneau via `PatientsClient` et `PraticiensClient`.
   - Réservation atomique du créneau (`reserveSlot`).
   - Endpoint de consultation enrichie : `GET /api/v1/appointments/{id}/enriched`.
5. **Jalon V5 (RabbitMQ) :**
   - `service-notification` consomme sur la file `notifications.email`.
   - Déclenchement automatique de notifications email lors des événements de réservation.
6. **Jalon V6 (Résilience Resilience4j) :**
   - Classe `ResilientRequests` dans `service-rendezvous` encapsulant `@Retry`, `@CircuitBreaker`.
   - Cache Caffeine pour la consultation enrichie en mode dégradé (champs mis en cache ou null lors d'une panne).
   - Repli strict HTTP 503 lors de la création si un service distant est en panne.
   - `@RateLimiter(name = "consultation")` pour protéger l'endpoint enrichi contre les surcharges.
7. **Jalon V7 (Transactions Événementielles avec Kafka) :**
   - Orchestration Saga via topics Kafka :
     - `appointment-requested` : Demande de réservation du créneau.
     - `slot-reserved` / `slot-rejected` : Confirmation ou rejet automatique du RDV.
     - `appointment-booked` : Confirmation envoyée par notification.
     - `consultation-completed` : Mise à jour sans écrasement du `MedicalRecord`.
     - `appointment-cancelled` : Libération du créneau et notification d'annulation.

---

## 🚀 Ordre de Démarrage Recommandé

### Étape 1 : Démarrer l'Infrastructure Docker
```bash
docker compose up -d
```
Services disponibles :
- **PostgreSQL** : `localhost:5432`
- **Apache Kafka** : `localhost:9092` (Kafka UI : `http://localhost:8085`)
- **RabbitMQ** : `localhost:5672` (Management UI : `http://localhost:15672`, user: `guest`, pass: `guest`)
- **Redis** : `localhost:6379`
- **MailHog** : `localhost:1025` (Web UI : `http://localhost:8025`)

---

### Étape 2 : Démarrer les Serveurs d'Infrastructure Spring Cloud
Dans des terminaux séparés ou sous IntelliJ :
```bash
# 1. Registre Eureka (Port 8761)
cd eureka-server && mvn spring-boot:run

# 2. Config Server (Port 8888)
cd ../config-server && mvn spring-boot:run

# 3. API Gateway (Port 8080)
cd ../api-gateway && mvn spring-boot:run
```

---

### Étape 3 : Démarrer les Microservices Métier
```bash
# 4. Praticiens (Port 8081)
cd service-praticiens && mvn spring-boot:run

# 5. Patients (Port 8082)
cd service-patients && mvn spring-boot:run

# 6. Rendez-vous (Port 8083) ⭐
cd service-rendezvous && mvn spring-boot:run

# 7. Notifications (Port 8084)
cd service-notification && mvn spring-boot:run

# 8. Batch Analytics (Port 8085)
cd service-batch-analytics && mvn spring-boot:run
```

---

## 🧪 Test End-to-End Automatisé

Exécutez le script PowerShell complet pour valider toutes les briques de l'architecture :
```powershell
.\test-e2e.ps1
```

# 🎓 Guide de Préparation à l'Examen & Soutenance — Med-RDV

Ce document est conçu pour votre **révision d'examen et soutenance orale** (J2EE Avancée — Dr. Salim KHALIL). Il synthétise l'architecture, les choix techniques, les configurations critiques et les questions types posées par les examinateurs.

---

## 1. Fiche d'Identité du Projet (Le Pitch de 1 Minute)

> **"Med-RDV est une plateforme microservices d'agenda médical multi-praticiens.**  
> Son unité de traitement est le **Rendez-vous**, dont le cycle de vie passe par `DEMANDE` → `CONFIRME` → `REALISE` ou `ANNULE`.  
> L'architecture est hybride :
> - **Lectures synchrones (REST / OpenFeign)** protégées par **Resilience4j** pour la consultation enrichie du dossier patient et praticien.
> - **Transactions distribuées asynchrones (Saga chorégraphiée avec Apache Kafka & RabbitMQ)** pour garantir la réservation atomique du créneau sans risque de surbooking, la mise à jour chronologique du dossier médical et les notifications par email."

---

## 2. Topologie des Services & Ports (À connaître par cœur)

| Service | Port | Rôle Clé | Technologies & Annotations |
| :--- | :---: | :--- | :--- |
| **`eureka-server`** | **8761** | Registre de découverte des services | `@EnableEurekaServer`, Netflix Eureka |
| **`config-server`** | **8888** | Centralisation des configurations Git/Native | `@EnableConfigServer`, Spring Cloud Config |
| **`api-gateway`** | **8080** | Point d'entrée unique, routage dynamique & statique, Rate Limiting | Spring Cloud Gateway (WebFlux réactif), Redis |
| **`service-praticiens`** | **8081** | Gestion des docteurs & réservation atomique des créneaux | Spring Data JPA, H2 / PostgreSQL, `@Version` lock |
| **`service-patients`** | **8082** | Annuaire des patients & synthèses médicales (allergies/antécédents) | Spring Data JPA, H2 / PostgreSQL |
| **`service-rendezvous`** ⭐ | **8083** | Orchestrateur de la Saga, vue enrichie, cycle de vie du RDV | `@EnableFeignClients`, `@EnableCaching`, Resilience4j |
| **`service-notification`** | **8084** | Envoi asynchrone d'emails et journal d'audit | `@RabbitListener`, `@KafkaListener`, JavaMailSender, MailHog |
| **`service-batch-analytics`** | **8085** | Jobs de rappels $J-1$ et export CSV mensuel | `@EnableScheduling`, `@Scheduled(cron = "...")` |

---

## 3. Les 7 Jalons Expliqués (Pourquoi chaque brique existe)

### V1 — CRUD Fonctionnel
- **Problème résolu** : Modéliser le domaine médical dans des microservices autonomes possédant chacun leur base de données (*Database-per-Service*).
- **Entités** : `Doctor` + `Availability` (Praticiens), `Patient` + `MedicalRecord` (Patients).

### V2 — Eureka & Gateway
- **Problème résolu** : Ne pas coder en dur les adresses IP/ports des services.
- **Eureka (`:8761`)** : Les microservices s'enregistrent comme clients (`eureka.client.service-url.defaultZone`).
- **Gateway (`:8080`)** : 
  - **Routage dynamique** : `discovery.locator.enabled=true` → `/service-praticiens/doctors`.
  - **Routage statique** : Déclaration de routes avec préfixe `lb://` (*LoadBalanced* via Spring Cloud LoadBalancer).

### V3 — Spring Cloud Config Server
- **Problème résolu** : Éviter la duplication des fichiers `application.properties` dans chaque service.
- **Fonctionnement** : `config-server` (:8888) lit les propriétés dans `config-repo/`.
- Chaque microservice déclare simplement dans son `application.properties` :
  ```properties
  spring.config.import=optional:configserver:http://localhost:8888/
  ```

### V4 — OpenFeign & Consultation Enrichie
- **Problème résolu** : Permettre à `service-rendezvous` d'interroger `service-patients` et `service-praticiens` de manière déclarative (sans `RestTemplate` verbeux).
- **Règles métier** :
  1. Validation de l'existence du patient et du médecin.
  2. Réservation atomique du créneau (`/availabilities/{id}/reserve`).
  3. Construction de l'endpoint enrichi `GET /api/v1/appointments/{id}/enriched` combinant Rendez-vous + Patient + Dossier Médical (allergies) + Praticien + Créneau.

### V5 — Notifications Asynchrones avec RabbitMQ
- **Problème résolu** : Découpler l'envoi d'email du flux HTTP de réservation (l'utilisateur n'attend pas que le serveur SMTP réponde).
- **Composants** :
  - `service-rendezvous` publie sur la file `notifications.email` via `RabbitTemplate`.
  - `service-notification` consomme le message avec `@RabbitListener(queues = "notifications.email")` et contacte MailHog (SMTP :1025).

### V6 — Résilience avec Resilience4j
- **Problème résolu** : Éviter les pannes en cascade si `service-patients` ou `service-praticiens` tombe.
- **Mécanismes implémentés dans `ResilientRequests`** :
  1. **`@Retry`** : Réessaie jusqu'à 3 fois avec un intervalle de 500 ms sur les erreurs transitoires.
  2. **`@CircuitBreaker`** : Ouvre le circuit (Open) si 50% des 6 derniers appels échouent → bascule immédiate en mode dégradé pendant 5 secondes sans inonder le service en panne.
  3. **Cache Caffeine** : Si `service-patients` est en panne lors d'un `GET /enriched`, la consultation renvoie les données depuis le cache mémoire local (mode dégradé) au lieu d'échouer.
  4. **Repli strict HTTP 503** : Si un service distant est indisponible lors d'un `POST /appointments` (création), on refuse immédiatement avec une `ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)` car on ne peut pas créer un RDV sur des données non validées.
  5. **`@RateLimiter`** : Limite le débit de consultations enrichies à 5 requêtes par seconde pour prévenir les dénis de service (renvoie HTTP 429 *Too Many Requests*).

### V7 — Transactions Métier Événementielles avec Apache Kafka
- **Problème résolu** : Cohérence éventuelle et transactions distribuées (Pattern Saga chorégraphiée) sans blocage bidirectionnel.
- **Topics Kafka** :
  - `appointment-requested` : service-rendezvous → service-praticiens (réserver le créneau).
  - `slot-reserved` / `slot-rejected` : service-praticiens → service-rendezvous (CONFIRME ou ANNULE).
  - `appointment-booked` : service-rendezvous → service-notification (confirmation email).
  - `consultation-completed` : service-rendezvous → service-patients (màj sans écraser consultation plus récente).
  - `appointment-cancelled` : service-rendezvous → service-praticiens (libérer créneau) & notification.

---

## 4. Questions Pièges Typiques d'Examen & Réponses Idéales

### Q1 : Pourquoi RabbitMQ en V5 et Kafka en V7 ? Quelle est la différence fondamentale ?
> **Réponse idéale :**  
> - **RabbitMQ** est un *Message Broker traditionnel* orienté files d'attente (AMQP). Dès qu'un message est acquitté (`ACK`), il est détruit de la file. Il n'y a pas d'historique ni de possibilité de rejouer des messages. C'est parfait pour distribuer des tâches discrètes (comme envoyer un email).
> - **Kafka** est un *Distributed Event Streaming Log*. Les messages sont enregistrés dans un journal immuable partitionné et conservés selon une durée de rétention. Plusieurs groupes de consommateurs indépendants peuvent lire le même topic à leur propre rythme et **rejouer l'historique** en cas de panne. C'est le standard pour les architectures orientées événements (EDA) et les sagas transactionnelles.

---

### Q2 : Pourquoi place-t-on le Retry avant le Circuit Breaker (`retry-aspect-order=1`, `circuit-breaker-aspect-order=2`) ?
> **Réponse idéale :**  
> L'aspect `@Retry` (ordre 1) doit envelopper le `@CircuitBreaker` (ordre 2). Ainsi, si un premier appel Feign échoue à cause d'un problème réseau temporaire, le Retry retente l'appel. Si la seconde tentative réussit, le Circuit Breaker ne comptabilise pas d'échec global. À l'inverse, si le circuit s'ouvre (état `OPEN`), le Retry verra l'exception `CallNotPermittedException` et ne gaspille pas de temps à faire des tentatives inutiles.

---

### Q3 : Comment résolvez-vous le problème de double réservation (Anti-Double Booking) ?
> **Réponse idéale :**  
> Nous combinons deux niveaux de protection :
> 1. **Verrouillage au niveau de la base** : Dans `service-praticiens`, l'entité `Availability` comporte une colonne `@Version` pour l'**Optimistic Locking** (ou une requête `@Lock(LockModeType.PESSIMISTIC_WRITE)`). Si deux requêtes tentent de réserver le même créneau en même temps, la seconde échoue avec `OptimisticLockException` ou attend la libération du verrou.
> 2. **Saga asynchrone** : Si la réservation échoue, `service-praticiens` émet `slot-rejected`, ce qui fait passer automatiquement le rendez-vous à `ANNULE` dans `service-rendezvous`.

---

### Q4 : Quelle est la différence entre le mode dégradé en consultation et le comportement en création dans Jalon V6 ?
> **Réponse idéale :**  
> - **En consultation (`GET /enriched`)** : La disponibilité prime. Si `service-patients` est en panne, on renvoie le rendez-vous avec les informations du patient lues depuis le cache local Caffeine (ou avec des champs partiels à null) grâce à la méthode de repli `patientDeRepli`. L'utilisateur a toujours une réponse.
> - **En création (`POST /appointments`)** : La cohérence prime. Si `service-patients` ou `service-praticiens` est indisponible, il est dangereux de créer un rendez-vous médical avec un patient ou médecin non vérifié. La méthode `patientIndisponible` lève alors strictement une exception `HTTP 503 Service Unavailable`.

---

### Q5 : Comment garantissez-vous que la fin de consultation n'écrase pas une consultation plus récente dans `service-patients` ?
> **Réponse idéale :**  
> Lors de la réception de l'événement `consultation-completed`, `service-patients` compare la date de consultation de l'événement avec `MedicalRecord.getDerniereConsultation()`.  
> La mise à jour du motif et de la date n'est effectuée que si `event.getDate().isAfter(existing.getDerniereConsultation())`. Cela gère les cas de rejeu d'événements Kafka ou de saisies tardives désordonnées.

---

### Q6 : Que signifie le préfixe `lb://` dans la Gateway et comment fonctionne la découverte dynamique ?
> **Réponse idéale :**  
> Le préfixe `lb://` indique à Spring Cloud Gateway d'utiliser **Spring Cloud LoadBalancer** pour résoudre le nom du service (`lb://service-rendezvous`) auprès d'Eureka.  
> La Gateway n'a pas besoin de connaître l'adresse IP ni le port physique de l'instance ; Eureka lui fournit la liste des instances disponibles et LoadBalancer distribue la charge (Round Robin par défaut).

---

## 5. Cheat Sheet des URLs Utiles pour la Démo

```bash
# Eureka Dashboard
http://localhost:8761/

# Config Server
http://localhost:8888/service-praticiens/default

# Gateway Routes
http://localhost:8080/api/v1/doctors
http://localhost:8080/api/v1/patients
http://localhost:8080/api/v1/appointments/1/enriched

# MailHog Web UI (Emails reçus)
http://localhost:8025/

# Kafka UI (Événements et topics)
http://localhost:8085/

# RabbitMQ Management (guest / guest)
http://localhost:15672/
```

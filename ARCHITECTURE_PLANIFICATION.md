# 🏥 Med-RDV — Spécifications d'Architecture & Plan de Développement

Plateforme Microservices Spring Boot 3 / Spring Cloud / Apache Kafka / PostgreSQL

---

## 1. Vue d'Ensemble & Rationalisation du Projet

Le cahier des charges initial proposait une évolution séquentielle (V1 CRUD synchrone feign → V5 RabbitMQ → V7 Kafka).  
Pour garantir une **architecture industrielle, maintenable et scalable**, nous unifions les choix technologiques :
- **Requêtes de lecture / Agrégation** : Synchrones via **OpenFeign** ou Pattern **BFF / API Composition** sécurisé avec **Resilience4j** (Circuit Breakers).
- **Transactions distribuées & mutations d'état** : Asynchrones via **Apache Kafka** (Event-Driven Architecture) couplé au **Transactional Outbox Pattern** pour éviter le problème de double écriture (Dual-Write).
- **Anti-Double Booking** : Verrouillage optimiste (`@Version`) ou pessimiste (`SELECT ... FOR UPDATE`) sur le créneau dans `service-praticiens`.
- **Sécurité & Conformité Données de Santé (HDS / RGPD)** : Authentification centralisée OAuth2 / JWT (Keycloak / Spring Authorization Server), chiffrement des données médicales au repos.

---

## 2. Décomposition des Microservices

| Service | Rôle Principal | Base de données | Technologies clés |
| :--- | :--- | :--- | :--- |
| `api-gateway` | Point d'entrée unique, rate limiting, routing, validation JWT | Redis (Cache / Rate limiter) | Spring Cloud Gateway, Spring Security Reactive, Redis |
| `service-praticiens` | Gestion des médecins et des disponibilités / créneaux | PostgreSQL (`praticiens_db`) | Spring Boot 3, JPA/Hibernate, Optimistic Locking |
| `service-patients` | Dossiers patients et antécédents médicaux | PostgreSQL (`patients_db`) | Spring Boot 3, JPA/Hibernate, Chiffrement des données |
| `service-rendezvous` ⭐ | Cœur métier : cycle de vie du RDV, orchestrateur de la Saga | PostgreSQL (`rendezvous_db`) | Spring Boot 3, OpenFeign, Kafka Producer/Consumer, Outbox |
| `service-notification` | Envoi d'alertes, emails et notifications temps-réel | PostgreSQL ou MongoDB | Spring Boot 3, Spring Kafka, JavaMailSender, WebSocket STOMP |
| `service-batch-analytics` | Traitements différés : rappels J-1 et exports CSV | - (Lecture via API/Events) | Spring Batch, `@Scheduled`, OpenCSV |

---

## 3. Matrice des Événements Kafka

| Topic | Producteur | Consommateur(s) | Description / Charge utile |
| :--- | :--- | :--- | :--- |
| `appointment-requested` | `service-rendezvous` | `service-praticiens` | Demande de blocage du créneau (`appointmentId`, `slotId`, `doctorId`, `patientId`) |
| `slot-reserved` | `service-praticiens` | `service-rendezvous` | Confirmation de réservation atomique du créneau |
| `slot-rejected` | `service-praticiens` | `service-rendezvous` | Rejet si créneau indisponible ou conflit de concurrence |
| `appointment-booked` | `service-rendezvous` | `service-notification` | RDV confirmé → Envoi d'email/SMS de confirmation |
| `consultation-completed` | `service-rendezvous` | `service-patients` | RDV honoré → Mise à jour de la date et du motif dans `MedicalRecord` |
| `appointment-cancelled` | `service-rendezvous` | `service-praticiens`, `service-notification` | RDV annulé → Libération du créneau et alerte patient |

---

## 4. Modèle de Données & Verrouillage Concurrence

### Anti-Double Réservation (`service-praticiens`)
Pour éliminer les risques de *race conditions* :
```java
@Entity
@Table(name = "availabilities")
public class Availability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private LocalDateTime dateHeureDebut;

    private Integer dureeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus statut; // DISPONIBLE, RESERVE, BLOQUE

    @Version
    private Long version; // Optimistic locking
}
```

---

## 5. Feuille de Route d'Implémentation (Sprints)

```text
[Sprint 1 : Socle Technique]
 ├── docker-compose.yml (PostgreSQL, Kafka KRaft, Redis, MailHog)
 ├── Multi-module Maven / Gradle (Parent POM)
 └── common-library (DTOs d'événements, Exceptions partagées)

[Sprint 2 : Services Praticiens & Patients]
 ├── service-praticiens (CRUD Doctor, Availability, Optimistic Lock)
 └── service-patients (CRUD Patient, MedicalRecord)

[Sprint 3 : Cœur Rendez-vous & Intégration Feign]
 ├── service-rendezvous (CRUD Appointment, Statut initial DEMANDE)
 ├── OpenFeign Clients avec Resilience4j (vérification Patient & Doctor)
 └── Endpoint enrichi : GET /appointments/{id}/enriched

[Sprint 4 : Saga Asynchrone avec Kafka]
 ├── Transactional Outbox Pattern dans service-rendezvous
 ├── Topics Kafka & Consommateurs idempotents
 └── Gestion des flux de compensation (slot-rejected -> ANNULE)

[Sprint 5 : Notifications & Fin de consultation]
 ├── service-notification (Consommation des événements, simulation email)
 └── Gestion de consultation-completed -> MàJ de MedicalRecord

[Sprint 6 : Gateway, Sécurité, Batch & Observabilité]
 ├── API Gateway avec Rate Limiting Redis & Sécurité JWT
 ├── Service Batch (@Scheduled rappel J-1 + stats CSV)
 └── Documentation OpenAPI / Swagger UI centralisée
```


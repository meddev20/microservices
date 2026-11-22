# 🏥 Med-RDV — Récapitulatif Pédagogique des Jalons (V1 à V7)

Ce dossier regroupe l'implémentation complète et autonome de chacun des jalons du projet **Med-RDV** (Agenda médical multi-praticiens), conformément aux sujets de TP de **Dr. Salim KHALIL (EMSI 5ème Année IDSI)** :

---

## 📑 Correspondance des Dossiers et des TPs

| Dossier | Jalon & TP Associé | PDF de Référence | Description & Compétences Clés |
| :--- | :--- | :--- | :--- |
| [`V1-CRUD`](./V1-CRUD) | **Jalon V1 : CRUD Fonctionnel** | *Fiche Projet Med-RDV* | Services autonomes `service-praticiens` (Doctor, Availability) et `service-patients` (Patient, MedicalRecord) avec base H2 et `data.sql`. |
| [`V2-Eureka-Gateway`](./V2-Eureka-Gateway) | **Jalon V2 : Eureka & Gateway** | [`Tp-V2.0-eureka-gateway.pdf`](./Tp-V2.0-eureka-gateway.pdf) | Mise en place de `eureka-server` (8761) et `gateway-service` (8080) avec routage dynamique et statique. Enregistrement des microservices. |
| [`V3-ConfigServer`](./V3-ConfigServer) | **Jalon V3 : Config Server** | [`Tp2-configserver.pdf`](./Tp2-configserver.pdf) | Centralisation des configurations avec `config-server` (8888) et dossier `config-repo/`. Découplage complet des paramètres d'environnement. |
| [`V4-OpenFeign`](./V4-OpenFeign) | **Jalon V4 : OpenFeign & Consultation Enrichie** | [`Tp4-openfeign.pdf`](./Tp4-openfeign.pdf) | Création de `service-rendezvous` (8883/8083), clients déclaratifs Feign vers praticiens et patients, réservation atomique de créneau et endpoint enrichi (`/enriched`). |
| [`V5-RabbitMQ`](./V5-RabbitMQ) | **Jalon V5 : Communication Asynchrone RabbitMQ** | [`Tp-v5-async-RabbitMQ.pdf`](./Tp-v5-async-RabbitMQ.pdf) | Création de `service-notification` (8084) avec écouteur RabbitMQ (`notifications.email`), envoi SMTP (MailHog) et découplage producteur/consommateur. |
| [`V6-Resilience`](./V6-Resilience) | **Jalon V6 : Résilience & Mode Dégradé** | [`Tp-v6-resilience.pdf`](./Tp-v6-resilience.pdf) | Resilience4j dans `service-rendezvous` : `@Retry`, `@CircuitBreaker`, `ResilientRequests`, Cache Caffeine, `@RateLimiter` sur la consultation enrichie, repli strict HTTP 503. |
| [`V7-Kafka`](./V7-Kafka) | **Jalon V7 : Transactions Métier avec Kafka** | [`Tp-v7-migration-Kafka.pdf`](./Tp-v7-migration-Kafka.pdf) | Architecture orientée événements (EDA) et Saga chorégraphiée (`appointment-requested`, `slot-reserved`, `slot-rejected`, `appointment-booked`, `consultation-completed`, `appointment-cancelled`). |

---

## 🚀 Comment Lancer un Jalon Spécifique ?

Chaque dossier est un projet Maven multi-module prêt à l'emploi.

Par exemple pour **Jalon V4** :
```bash
cd jalon/V4-OpenFeign
# 1. Lancer Eureka
cd eureka-server && mvn spring-boot:run
# 2. Lancer la Gateway
cd ../gateway-service && mvn spring-boot:run
# 3. Lancer les services métiers
cd ../service-praticiens && mvn spring-boot:run
cd ../service-patients && mvn spring-boot:run
cd ../service-rendezvous && mvn spring-boot:run
```

Pour les jalons utilisant des brokers de messages (**V5** et **V7**), démarrez d'abord le docker-compose inclus :
```bash
# Pour V5 (RabbitMQ & MailHog)
cd jalon/V5-RabbitMQ
docker compose up -d

# Pour V7 (Apache Kafka & Kafka UI)
cd jalon/V7-Kafka
docker compose up -d
```

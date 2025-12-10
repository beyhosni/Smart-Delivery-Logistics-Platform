# Architecture de la Smart Delivery & Logistics Platform

## Vue d'ensemble

L'architecture de notre plateforme est basée sur des microservices qui communiquent via des événements RabbitMQ. Chaque service a une responsabilité spécifique et peut être développé, déployé et mis à l'échelle indépendamment.

## Diagramme d'architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway                             │
│                    (gateway-service)                        │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────────┐
│                  RabbitMQ Message Broker                     │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼────┐ ┌─────▼─────┐
│  Delivery    │ │Tracking │ │Dispatcher │
│  Service     │ │Service  │ │ Service   │
└──────────────┘ └─────────┘ └───────────┘
        │             │             │
        └─────────────┼─────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼────┐ ┌─────▼─────┐
│ Notification │ │ Route   │ │ Frontend  │
│ Service      │ │Optimizer│ │ (Angular) │
└──────────────┘ └─────────┘ └───────────┘
```

## Services

### 1. Gateway Service (Port: 8080)
- **Responsabilité**: Point d'entrée unique pour toutes les requêtes client
- **Technologies**: Spring Cloud Gateway, Spring Security
- **Fonctionnalités**:
  - Routage des requêtes vers les services appropriés
  - Authentification et autorisation
  - Rate limiting
  - Load balancing

### 2. Delivery Service (Port: 8081)
- **Responsabilité**: Gestion du cycle de vie des livraisons
- **Technologies**: Spring Boot, Spring Data JPA, PostgreSQL
- **Fonctionnalités**:
  - CRUD des livraisons
  - Validation des informations de livraison
  - Publication d'événements de livraison

### 3. Dispatcher Service (Port: 8082)
- **Responsabilité**: Attribution des livraisons aux livreurs
- **Technologies**: Spring Boot, Spring Data JPA, PostgreSQL
- **Fonctionnalités**:
  - Gestion des livreurs
  - Attribution automatique des livraisons
  - Optimisation de la charge de travail

### 4. Tracking Service (Port: 8083)
- **Responsabilité**: Suivi en temps réel des livraisons
- **Technologies**: Spring Boot, MongoDB, WebSocket
- **Fonctionnalités**:
  - Suivi GPS des livraisons
  - Mises à jour de position en temps réel
  - Historique des positions

### 5. Notification Service (Port: 8084)
- **Responsabilité**: Envoi de notifications aux clients
- **Technologies**: Spring Boot, Spring Mail, Twilio
- **Fonctionnalités**:
  - Notifications par email
  - Notifications SMS
  - Notifications push

### 6. Route Optimizer Service (Port: 8085)
- **Responsabilité**: Optimisation des routes de livraison
- **Technologies**: Spring Boot, GraphHopper, OpenStreetMap
- **Fonctionnalités**:
  - Calcul d'itinéraires optimaux
  - Optimisation multi-livraisons
  - Prise en compte du trafic en temps réel

## Événements RabbitMQ

### Exchanges et Queues

| Exchange | Routing Key | Queue | Service Consommateur |
|----------|-------------|-------|----------------------|
| delivery.exchange | delivery.created | delivery.created.queue | dispatcher, tracking, notification |
| delivery.exchange | delivery.dispatched | delivery.dispatched.queue | tracking, notification |
| delivery.exchange | delivery.in_transit | delivery.in_transit.queue | tracking, notification |
| delivery.exchange | delivery.delivered | delivery.delivered.queue | notification |

### Format des messages

#### delivery.created
```json
{
  "eventId": "uuid",
  "timestamp": "2023-10-15T10:30:00Z",
  "eventType": "delivery.created",
  "data": {
    "deliveryId": "uuid",
    "senderId": "uuid",
    "recipientId": "uuid",
    "pickupAddress": {
      "street": "123 Pickup St",
      "city": "City",
      "postalCode": "12345",
      "country": "Country",
      "coordinates": {
        "latitude": 48.8566,
        "longitude": 2.3522
      }
    },
    "deliveryAddress": {
      "street": "456 Delivery Ave",
      "city": "City",
      "postalCode": "54321",
      "country": "Country",
      "coordinates": {
        "latitude": 48.8584,
        "longitude": 2.2945
      }
    },
    "packageDetails": {
      "weight": 2.5,
      "dimensions": {
        "length": 30,
        "width": 20,
        "height": 10
      }
    },
    "priority": "NORMAL",
    "requestedDeliveryTime": "2023-10-16T14:00:00Z"
  }
}
```

#### delivery.dispatched
```json
{
  "eventId": "uuid",
  "timestamp": "2023-10-15T11:00:00Z",
  "eventType": "delivery.dispatched",
  "data": {
    "deliveryId": "uuid",
    "dispatcherId": "uuid",
    "courierId": "uuid",
    "estimatedDeliveryTime": "2023-10-15T15:30:00Z",
    "route": [
      {
        "latitude": 48.8566,
        "longitude": 2.3522,
        "estimatedArrival": "2023-10-15T11:30:00Z"
      },
      {
        "latitude": 48.8584,
        "longitude": 2.2945,
        "estimatedArrival": "2023-10-15T15:30:00Z"
      }
    ]
  }
}
```

#### delivery.in_transit
```json
{
  "eventId": "uuid",
  "timestamp": "2023-10-15T12:00:00Z",
  "eventType": "delivery.in_transit",
  "data": {
    "deliveryId": "uuid",
    "courierId": "uuid",
    "currentLocation": {
      "latitude": 48.8570,
      "longitude": 2.3400
    },
    "estimatedDeliveryTime": "2023-10-15T15:30:00Z",
    "status": "PICKED_UP"
  }
}
```

#### delivery.delivered
```json
{
  "eventId": "uuid",
  "timestamp": "2023-10-15T15:30:00Z",
  "eventType": "delivery.delivered",
  "data": {
    "deliveryId": "uuid",
    "courierId": "uuid",
    "deliveryLocation": {
      "latitude": 48.8584,
      "longitude": 2.2945
    },
    "deliveryTime": "2023-10-15T15:30:00Z",
    "recipientName": "John Doe",
    "signature": "base64-encoded-signature",
    "photo": "base64-encoded-photo"
  }
}
```

## Sécurité

- Authentification via JWT (JSON Web Tokens)
- Autorisation basée sur les rôles (RBAC)
- Communication HTTPS entre services
- Chiffrement des données sensibles

## Scalabilité

- Chaque service peut être scalé horizontalement indépendamment
- Utilisation de partitions RabbitMQ pour gérer la charge
- Mise en cache avec Redis pour les données fréquemment accédées
- Base de données répliquée pour la haute disponibilité

## Observabilité

- Logs structurés avec ELK Stack
- Métriques avec Prometheus et Grafana
- Tracing distribué avec Jaeger
- Health checks pour chaque service

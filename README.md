# Smart Delivery & Logistics Platform

## Description

Smart Delivery & Logistics Platform est une solution complète de gestion de livraisons basée sur une architecture de microservices. Elle permet aux utilisateurs de créer, suivre et gérer des livraisons en temps réel, avec optimisation des itinéraires et notifications automatiques.

## Architecture

L'application est composée des éléments suivants :

- **Frontend** : Application Angular pour l'interface utilisateur
- **Backend** : Services Spring Boot organisés en microservices
- **Gateway** : API Gateway pour router les requêtes vers les services appropriés
- **Communication** : RabbitMQ pour la communication asynchrone entre services
- **Bases de données** : PostgreSQL et MongoDB pour la persistance des données

### Services Backend

1. **Gateway Service** (Port 8080) : Point d'entrée unique pour toutes les requêtes client
2. **Delivery Service** (Port 8081) : Gestion du cycle de vie des livraisons
3. **Dispatcher Service** (Port 8082) : Attribution des livraisons aux livreurs
4. **Tracking Service** (Port 8083) : Suivi en temps réel des livraisons
5. **Notification Service** (Port 8084) : Envoi de notifications aux clients
6. **Route Optimizer Service** (Port 8085) : Optimisation des routes de livraison

## Prérequis

- Docker et Docker Compose
- Node.js 18+ (pour le frontend)
- Java 23 (pour le développement local)
- Maven 3.8+

## Démarrage rapide

### Avec Docker Compose

1. Clonez ce dépôt :
   ```bash
   git clone https://github.com/votre-organisation/Smart-Delivery-Logistics-Platform.git
   cd Smart-Delivery-Logistics-Platform
   ```

2. Démarrez tous les services :
   ```bash
   docker-compose up -d
   ```

3. Accédez à l'application :
   - Frontend : http://localhost:4200
   - API Gateway : http://localhost:8080
   - RabbitMQ Management : http://localhost:15672 (guest/guest)

### Pour le développement local

#### Backend

1. Compilez chaque service :
   ```bash
   cd backend/delivery-service
   mvn clean install
   cd ../dispatcher-service
   mvn clean install
   # ... et ainsi de suite pour chaque service
   ```

2. Démarrez les services nécessaires (PostgreSQL, MongoDB, RabbitMQ) :
   ```bash
   docker-compose up -d postgres mongodb rabbitmq
   ```

3. Lancez chaque service individuellement :
   ```bash
   cd backend/delivery-service
   mvn spring-boot:run
   # ... dans des terminaux séparés pour chaque service
   ```

#### Frontend

1. Installez les dépendances :
   ```bash
   cd frontend
   npm install
   ```

2. Démarrez le serveur de développement :
   ```bash
   ng serve
   ```

## Documentation

- [Architecture détaillée](docs/architecture.md)
- [API Documentation](docs/api.md) (à venir)
- [Guide de développement](docs/development-guide.md) (à venir)

## Contribution

Pour contribuer à ce projet, veuillez suivre les étapes suivantes :

1. Fork ce dépôt
2. Créez une branche pour votre fonctionnalité (`git checkout -b feature/nouvelle-fonctionnalite`)
3. Commitez vos changements (`git commit -am 'Ajout d'une nouvelle fonctionnalité'`)
4. Pushez vers la branche (`git push origin feature/nouvelle-fonctionnalite`)
5. Créez une Pull Request

## Licence

Ce projet est sous licence MIT - voir le fichier [LICENSE](LICENSE) pour plus de détails.

## Contact

- Email : contact@smartdelivery.fr
- Site web : https://smartdelivery.fr


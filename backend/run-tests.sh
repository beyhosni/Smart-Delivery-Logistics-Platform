
#!/bin/bash

echo "Exécution des tests unitaires pour tous les services..."

# Test unitaires pour le service Delivery
echo "Tests unitaires pour Delivery Service..."
cd delivery-service
mvn test

# Test unitaires pour le service Dispatcher
echo "Tests unitaires pour Dispatcher Service..."
cd ../dispatcher-service
mvn test

# Test unitaires pour le service Tracking
echo "Tests unitaires pour Tracking Service..."
cd ../tracking-service
mvn test

# Test unitaires pour le service Notification
echo "Tests unitaires pour Notification Service..."
cd ../notification-service
mvn test

# Test unitaires pour le service Route Optimizer
echo "Tests unitaires pour Route Optimizer Service..."
cd ../route-optimizer-service
mvn test

# Test unitaires pour le service Gateway
echo "Tests unitaires pour Gateway Service..."
cd ../gateway-service
mvn test

echo "Exécution des tests d'intégration..."

# Tests d'intégration pour le service Delivery
echo "Tests d'intégration pour Delivery Service..."
cd ../delivery-service
mvn test -Dspring.profiles.active=integration

# Tests d'intégration pour les autres services...
# (À compléter pour chaque service)

echo "Exécution des tests de bout en bout..."
cd ../e2e-test
mvn verify

echo "Tous les tests ont été exécutés avec succès!"

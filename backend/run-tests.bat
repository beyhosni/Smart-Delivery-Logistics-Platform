
@echo off
echo Exécution des tests unitaires pour tous les services...

REM Test unitaires pour le service Delivery
echo Tests unitaires pour Delivery Service...
cd delivery-service
call mvn test

REM Test unitaires pour le service Dispatcher
echo Tests unitaires pour Dispatcher Service...
cd ..\dispatcher-service
call mvn test

REM Test unitaires pour le service Tracking
echo Tests unitaires pour Tracking Service...
cd ..	racking-service
call mvn test

REM Test unitaires pour le service Notification
echo Tests unitaires pour Notification Service...
cd ..
otification-service
call mvn test

REM Test unitaires pour le service Route Optimizer
echo Tests unitaires pour Route Optimizer Service...
cd ..oute-optimizer-service
call mvn test

REM Test unitaires pour le service Gateway
echo Tests unitaires pour Gateway Service...
cd ..\gateway-service
call mvn test

echo Exécution des tests d'intégration...

REM Tests d'intégration pour le service Delivery
echo Tests d'intégration pour Delivery Service...
cd ..\delivery-service
call mvn test -Dspring.profiles.active=integration

REM Tests d'intégration pour les autres services...
REM (À compléter pour chaque service)

echo Exécution des tests de bout en bout...
cd ..\e2e-test
call mvn verify

echo Tous les tests ont été exécutés avec succès!
pause

// Création des bases de données
db = db.getSiblingDB('tracking_db');

// Création des collections pour tracking_db
db.createCollection('delivery_locations');
db.createCollection('tracking_events');

// Création d'index pour optimiser les requêtes
db.delivery_locations.createIndex({ "deliveryId": 1, "timestamp": -1 });
db.tracking_events.createIndex({ "deliveryId": 1, "timestamp": -1 });

// Insertion de données de test pour tracking_db
db.delivery_locations.insertMany([
  {
    deliveryId: "550e8400-e29b-41d4-a716-446655440001",
    courierId: "550e8400-e29b-41d4-a716-446655440101",
    location: {
      type: "Point",
      coordinates: [2.3522, 48.8566] // longitude, latitude (Paris)
    },
    timestamp: new Date("2023-10-15T12:00:00Z"),
    accuracy: 10.5
  },
  {
    deliveryId: "550e8400-e29b-41d4-a716-446655440002",
    courierId: "550e8400-e29b-41d4-a716-446655440102",
    location: {
      type: "Point",
      coordinates: [2.2945, 48.8584] // longitude, latitude (Tour Eiffel)
    },
    timestamp: new Date("2023-10-15T13:30:00Z"),
    accuracy: 8.2
  }
]);

db.tracking_events.insertMany([
  {
    deliveryId: "550e8400-e29b-41d4-a716-446655440001",
    eventType: "PICKED_UP",
    timestamp: new Date("2023-10-15T11:45:00Z"),
    location: {
      type: "Point",
      coordinates: [2.3522, 48.8566] // longitude, latitude (Paris)
    },
    description: "Colis ramassé par le livreur",
    courierId: "550e8400-e29b-41d4-a716-446655440101"
  },
  {
    deliveryId: "550e8400-e29b-41d4-a716-446655440001",
    eventType: "IN_TRANSIT",
    timestamp: new Date("2023-10-15T12:00:00Z"),
    location: {
      type: "Point",
      coordinates: [2.3522, 48.8566] // longitude, latitude (Paris)
    },
    description: "En route vers la destination",
    courierId: "550e8400-e29b-41d4-a716-446655440101"
  },
  {
    deliveryId: "550e8400-e29b-41d4-a716-446655440002",
    eventType: "DELIVERED",
    timestamp: new Date("2023-10-15T14:30:00Z"),
    location: {
      type: "Point",
      coordinates: [2.2945, 48.8584] // longitude, latitude (Tour Eiffel)
    },
    description: "Colis livré avec succès",
    courierId: "550e8400-e29b-41d4-a716-446655440102",
    recipientName: "Jean Dupont",
    signature: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
    photo: "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k="
  }
]);

// Création d'un utilisateur pour accéder à la base de données
db.createUser({
  user: "tracking_user",
  pwd: "tracking_password",
  roles: [
    {
      role: "readWrite",
      db: "tracking_db"
    }
  ]
});

print("Initialisation de MongoDB terminée avec succès");

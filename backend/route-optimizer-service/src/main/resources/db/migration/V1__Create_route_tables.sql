-- Création de la table route
CREATE TABLE IF NOT EXISTS route (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id UUID NOT NULL,
    courier_id UUID NOT NULL,
    waypoints JSONB NOT NULL,
    total_distance DECIMAL(10, 2) NOT NULL,
    estimated_time INTERVAL NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ajout d'index pour optimiser les requêtes
CREATE INDEX IF NOT EXISTS idx_route_delivery_id ON route(delivery_id);
CREATE INDEX IF NOT EXISTS idx_route_courier_id ON route(courier_id);
CREATE INDEX IF NOT EXISTS idx_route_waypoints ON route USING GIN (waypoints);

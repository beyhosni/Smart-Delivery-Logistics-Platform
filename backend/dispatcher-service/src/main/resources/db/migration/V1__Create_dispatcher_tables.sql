-- Création de la table courier
CREATE TABLE IF NOT EXISTS courier (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    current_location JSONB,
    vehicle_type VARCHAR(20) NOT NULL,
    max_capacity DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Création de la table delivery_assignment
CREATE TABLE IF NOT EXISTS delivery_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id UUID NOT NULL,
    courier_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estimated_delivery_time TIMESTAMP,
    actual_delivery_time TIMESTAMP,
    route JSONB,
    FOREIGN KEY (courier_id) REFERENCES courier(id)
);

-- Ajout d'index pour optimiser les requêtes
CREATE INDEX IF NOT EXISTS idx_courier_status ON courier(status);
CREATE INDEX IF NOT EXISTS idx_courier_vehicle_type ON courier(vehicle_type);
CREATE INDEX IF NOT EXISTS idx_courier_current_location ON courier USING GIN ((current_location->'coordinates'));

CREATE INDEX IF NOT EXISTS idx_delivery_assignment_delivery_id ON delivery_assignment(delivery_id);
CREATE INDEX IF NOT EXISTS idx_delivery_assignment_courier_id ON delivery_assignment(courier_id);
CREATE INDEX IF NOT EXISTS idx_delivery_assignment_status ON delivery_assignment(status);

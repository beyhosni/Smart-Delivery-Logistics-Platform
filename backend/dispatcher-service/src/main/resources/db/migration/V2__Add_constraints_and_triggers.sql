-- Ajout de contraintes de validation pour la table courier
ALTER TABLE courier 
ADD CONSTRAINT chk_courier_status 
CHECK (status IN ('AVAILABLE', 'BUSY', 'OFFLINE', 'ON_BREAK'));

ALTER TABLE courier 
ADD CONSTRAINT chk_vehicle_type 
CHECK (vehicle_type IN ('BICYCLE', 'MOTORCYCLE', 'CAR', 'VAN', 'TRUCK'));

ALTER TABLE courier 
ADD CONSTRAINT chk_max_capacity 
CHECK (max_capacity > 0);

-- Ajout de contraintes de validation pour la table delivery_assignment
ALTER TABLE delivery_assignment 
ADD CONSTRAINT chk_assignment_status 
CHECK (status IN ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

-- Création d'un trigger pour mettre à jour automatiquement le champ updated_at dans la table courier
CREATE OR REPLACE FUNCTION update_courier_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_courier_updated_at 
BEFORE UPDATE ON courier 
FOR EACH ROW EXECUTE FUNCTION update_courier_updated_at_column();

-- Ajout d'un index composite pour optimiser les requêtes sur les livreurs disponibles par type de véhicule
CREATE INDEX IF NOT EXISTS idx_courier_status_vehicle_type 
ON courier(status, vehicle_type);

-- Ajout d'un index pour optimiser les requêtes sur les livraisons assignées par statut et date
CREATE INDEX IF NOT EXISTS idx_delivery_assignment_status_assigned_at 
ON delivery_assignment(status, assigned_at);

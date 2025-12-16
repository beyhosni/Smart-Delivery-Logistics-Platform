-- Ajout de contraintes de validation pour la table deliveries
ALTER TABLE deliveries 
ADD CONSTRAINT chk_delivery_priority 
CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'));

ALTER TABLE deliveries 
ADD CONSTRAINT chk_delivery_status 
CHECK (status IN ('CREATED', 'DISPATCHED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED'));

-- Création d'un trigger pour mettre à jour automatiquement le champ updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_deliveries_updated_at 
BEFORE UPDATE ON deliveries 
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Ajout d'un index composite pour optimiser les requêtes sur les livraisons par statut et date
CREATE INDEX IF NOT EXISTS idx_deliveries_status_created_at 
ON deliveries(status, created_at);

-- Ajout d'un index sur le champ JSONB pour optimiser les requêtes géospatiales
CREATE INDEX IF NOT EXISTS idx_deliveries_pickup_location 
ON deliveries USING GIN ((pickup_address->'coordinates'));

CREATE INDEX IF NOT EXISTS idx_deliveries_delivery_location 
ON deliveries USING GIN ((delivery_address->'coordinates'));

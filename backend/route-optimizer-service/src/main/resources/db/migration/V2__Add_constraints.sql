-- Ajout de contraintes de validation pour la table route
ALTER TABLE route 
ADD CONSTRAINT chk_total_distance 
CHECK (total_distance >= 0);

-- Ajout d'un index composite pour optimiser les requêtes sur les routes par livreur et date
CREATE INDEX IF NOT EXISTS idx_route_courier_created_at 
ON route(courier_id, created_at);

-- Ajout d'un index pour optimiser les requêtes sur les routes par distance
CREATE INDEX IF NOT EXISTS idx_route_total_distance 
ON route(total_distance);

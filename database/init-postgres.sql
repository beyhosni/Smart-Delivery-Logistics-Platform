-- Création des bases de données
CREATE DATABASE delivery_db;
CREATE DATABASE dispatcher_db;
CREATE DATABASE route_optimizer_db;
CREATE DATABASE notification_db;

-- Création des utilisateurs et attribution des droits
CREATE USER delivery_user WITH PASSWORD 'delivery_password';
GRANT ALL PRIVILEGES ON DATABASE delivery_db TO delivery_user;

CREATE USER dispatcher_user WITH PASSWORD 'dispatcher_password';
GRANT ALL PRIVILEGES ON DATABASE dispatcher_db TO dispatcher_user;

CREATE USER route_optimizer_user WITH PASSWORD 'route_optimizer_password';
GRANT ALL PRIVILEGES ON DATABASE route_optimizer_db TO route_optimizer_user;

CREATE USER notification_user WITH PASSWORD 'notification_password';
GRANT ALL PRIVILEGES ON DATABASE notification_db TO notification_user;

-- Création des tables pour delivery_db
\c delivery_db;

CREATE TABLE IF NOT EXISTS delivery (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    pickup_address JSONB NOT NULL,
    delivery_address JSONB NOT NULL,
    package_details JSONB NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    requested_delivery_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Création des tables pour dispatcher_db
\c dispatcher_db;

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

CREATE TABLE IF NOT EXISTS delivery_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id UUID NOT NULL,
    courier_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estimated_delivery_time TIMESTAMP,
    actual_delivery_time TIMESTAMP,
    route JSONB,
    FOREIGN KEY (delivery_id) REFERENCES delivery(id),
    FOREIGN KEY (courier_id) REFERENCES courier(id)
);

-- Création des tables pour route_optimizer_db
\c route_optimizer_db;

CREATE TABLE IF NOT EXISTS route (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id UUID NOT NULL,
    courier_id UUID NOT NULL,
    waypoints JSONB NOT NULL,
    total_distance DECIMAL(10, 2) NOT NULL,
    estimated_time INTERVAL NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (delivery_id) REFERENCES delivery(id),
    FOREIGN KEY (courier_id) REFERENCES courier(id)
);

-- Création des tables pour notification_db
\c notification_db;

CREATE TABLE IF NOT EXISTS notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (delivery_id) REFERENCES delivery(id)
);

-- Check if SUPER_ADMIN role exists and insert it if it doesn't
-- This script can be run directly in pgAdmin or any PostgreSQL client

-- First, let's see what roles currently exist
SELECT * FROM roles;

-- Check if SUPER_ADMIN role exists
SELECT * FROM roles WHERE name = 'SUPER_ADMIN';

-- If SUPER_ADMIN doesn't exist, insert it
INSERT INTO roles (id, name, description, scope, created_at, updated_at, version)
SELECT
    '00000000-0000-0000-0000-000000000201' as id,
    'SUPER_ADMIN' as name,
    'Super Administrator with full access' as description,
    'PLATFORM' as scope,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at,
    0 as version
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'SUPER_ADMIN'
);

-- Also insert USER role if it doesn't exist
INSERT INTO roles (id, name, description, scope, created_at, updated_at, version)
SELECT
    '00000000-0000-0000-0000-000000000202' as id,
    'USER' as name,
    'Regular user with basic access' as description,
    'PLATFORM' as scope,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at,
    0 as version
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'USER'
);

-- Verify the roles were inserted
SELECT * FROM roles;
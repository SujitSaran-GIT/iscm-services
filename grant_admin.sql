-- Script to assign SUPER_ADMIN role to admin@example.com user
-- This script finds the user ID for admin@example.com and assigns SUPER_ADMIN role

-- Get user ID for admin@example.com
SELECT id INTO @admin_user_id FROM users WHERE email = 'admin@example.com';

-- Insert SUPER_ADMIN role if not exists (safety check)
INSERT INTO roles (id, name, description, scope, created_at, updated_at, version, tenant_id)
SELECT gen_random_uuid() as id, 'SUPER_ADMIN' as name, 'Super Administrator with full access' as description, 'PLATFORM' as scope, NOW(), NOW(), 0, '00000000-0000-0000-0000-000000000000'
ON CONFLICT (name) DO NOTHING;

-- Assign SUPER_ADMIN role to the user
INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by, tenant_id)
SELECT
    @admin_user_id as user_id,
    (SELECT id FROM roles WHERE name = 'SUPER_ADMIN') as role_id,
    NOW() as assigned_at,
    @admin_user_id as assigned_by,
    (SELECT tenant_id FROM users WHERE email = 'admin@example.com') as tenant_id;

-- Verify the assignment
SELECT
    u.email,
    u.first_name,
    u.last_name,
    r.name as role_name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'admin@example.com' AND r.name = 'SUPER_ADMIN';
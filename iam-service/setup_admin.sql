-- Insert SUPER_ADMIN role if not exists
INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'SUPER_ADMIN',
    'Super Administrator with full access',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Insert ADMIN role if not exists
INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'ADMIN',
    'Administrator with limited access',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Get the user ID for admin@example.com
SELECT id FROM users WHERE email = 'admin@example.com';

-- After getting the user ID, run this to assign SUPER_ADMIN role:
-- INSERT INTO user_roles (user_id, role_id, assigned_at)
-- VALUES ('user-id-here', (SELECT id FROM roles WHERE name = 'SUPER_ADMIN'), NOW());
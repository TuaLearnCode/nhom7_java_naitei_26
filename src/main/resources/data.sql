-- Local/demo seed data.
-- All seeded accounts use the plain-text password: Password@123

-- Roles
INSERT INTO role (name) VALUES ('USER')      ON CONFLICT (name) DO NOTHING;
INSERT INTO role (name) VALUES ('HOST')      ON CONFLICT (name) DO NOTHING;
INSERT INTO role (name) VALUES ('MODERATOR') ON CONFLICT (name) DO NOTHING;
INSERT INTO role (name) VALUES ('ADMIN')     ON CONFLICT (name) DO NOTHING;

-- Users (BCrypt cost 10 hash for Password@123)
INSERT INTO users (
    id, name, email, password, phone, status,
    is_identity_verified, is_business_verified, language, password_changed_at
) VALUES
    (1, 'System Admin', 'admin@coworking.local',
     '$2a$10$WQYFCw/ehpYp0JCBGCESLu41vy4HoBTygvRX.B3kJ.92EcAgVKbxG',
     '0900000001', 'ACTIVE', TRUE, TRUE, 'vi', CURRENT_TIMESTAMP),
    (2, 'Content Moderator', 'moderator@coworking.local',
     '$2a$10$WQYFCw/ehpYp0JCBGCESLu41vy4HoBTygvRX.B3kJ.92EcAgVKbxG',
     '0900000002', 'ACTIVE', TRUE, TRUE, 'vi', CURRENT_TIMESTAMP),
    (3, 'Demo Host', 'host@coworking.local',
     '$2a$10$WQYFCw/ehpYp0JCBGCESLu41vy4HoBTygvRX.B3kJ.92EcAgVKbxG',
     '0900000003', 'ACTIVE', TRUE, TRUE, 'vi', CURRENT_TIMESTAMP),
    (4, 'Demo User', 'user@coworking.local',
     '$2a$10$WQYFCw/ehpYp0JCBGCESLu41vy4HoBTygvRX.B3kJ.92EcAgVKbxG',
     '0900000004', 'ACTIVE', TRUE, FALSE, 'vi', CURRENT_TIMESTAMP)
ON CONFLICT (email) DO UPDATE SET
    name = EXCLUDED.name,
    password = EXCLUDED.password,
    phone = EXCLUDED.phone,
    status = EXCLUDED.status,
    is_identity_verified = EXCLUDED.is_identity_verified,
    is_business_verified = EXCLUDED.is_business_verified,
    language = EXCLUDED.language,
    password_changed_at = EXCLUDED.password_changed_at;

-- Role assignments
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN role r
WHERE u.email = 'admin@coworking.local' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN role r
WHERE u.email = 'moderator@coworking.local' AND r.name = 'MODERATOR'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN role r
WHERE u.email = 'host@coworking.local' AND r.name = 'HOST'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN role r
WHERE u.email = 'user@coworking.local' AND r.name = 'USER'
ON CONFLICT DO NOTHING;

-- Amenities
INSERT INTO amenity (id, name) VALUES
    (1, 'Wi-Fi tốc độ cao'),
    (2, 'Máy lạnh'),
    (3, 'Máy chiếu'),
    (4, 'Bãi đỗ xe'),
    (5, 'Cà phê miễn phí')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

-- Venues
INSERT INTO venue (
    id, owner_id, name, description, address, city, street,
    latitude, longitude, status, block_reason, deleted
) VALUES
    (1, 3, 'Saigon Central Workspace',
     'Không gian làm việc hiện đại tại trung tâm Thành phố Hồ Chí Minh.',
     '123 Nguyễn Huệ, Phường Sài Gòn, Thành phố Hồ Chí Minh',
     'Thành phố Hồ Chí Minh', 'Nguyễn Huệ', 10.77312000, 106.70328000,
     'APPROVE', NULL, FALSE),
    (2, 3, 'Riverside Coworking',
     'Không gian yên tĩnh, phù hợp làm việc nhóm và tổ chức họp.',
     '45 Tôn Đức Thắng, Phường Sài Gòn, Thành phố Hồ Chí Minh',
     'Thành phố Hồ Chí Minh', 'Tôn Đức Thắng', 10.78115000, 106.70687000,
     'PENDING', NULL, FALSE)
ON CONFLICT (id) DO UPDATE SET
    owner_id = EXCLUDED.owner_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    address = EXCLUDED.address,
    city = EXCLUDED.city,
    street = EXCLUDED.street,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    status = EXCLUDED.status,
    block_reason = EXCLUDED.block_reason,
    deleted = EXCLUDED.deleted;

INSERT INTO venue_amenities (venue_id, amenity_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
    (2, 1), (2, 2), (2, 4)
ON CONFLICT DO NOTHING;

-- Spaces
INSERT INTO space (
    id, venue_id, name, type, capacity, description,
    price, price_unit, open_time, close_time, status
) VALUES
    (1, 1, 'Bàn làm việc linh hoạt', 'HOT_DESK', 20,
     'Chỗ ngồi linh hoạt trong khu vực làm việc chung.',
     60000.00, 'HOUR', '08:00', '22:00', 'ACTIVE'),
    (2, 1, 'Phòng họp Lotus', 'MEETING_ROOM', 8,
     'Phòng họp riêng có màn hình và bảng viết.',
     250000.00, 'HOUR', '08:00', '22:00', 'ACTIVE'),
    (3, 2, 'Văn phòng riêng Riverside', 'PRIVATE_OFFICE', 6,
     'Văn phòng riêng dành cho nhóm nhỏ.',
     1200000.00, 'DAY', '08:00', '20:00', 'INACTIVE')
ON CONFLICT (id) DO UPDATE SET
    venue_id = EXCLUDED.venue_id,
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    capacity = EXCLUDED.capacity,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    price_unit = EXCLUDED.price_unit,
    open_time = EXCLUDED.open_time,
    close_time = EXCLUDED.close_time,
    status = EXCLUDED.status;

INSERT INTO space_host (space_id, user_id) VALUES
    (1, 3), (2, 3), (3, 3)
ON CONFLICT DO NOTHING;

-- Representative bookings for user/moderator/admin screens
INSERT INTO booking (
    id, user_id, space_id, start_time, end_time,
    total_price, status, created_at
) VALUES
    (1, 4, 1,
     CURRENT_DATE + INTERVAL '1 day 09:00',
     CURRENT_DATE + INTERVAL '1 day 12:00',
     180000.00, 'PENDING', CURRENT_TIMESTAMP),
    (2, 4, 2,
     CURRENT_DATE + INTERVAL '2 days 13:00',
     CURRENT_DATE + INTERVAL '2 days 15:00',
     500000.00, 'PAID', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (3, 4, 1,
     CURRENT_DATE - INTERVAL '2 days' + INTERVAL '09:00',
     CURRENT_DATE - INTERVAL '2 days' + INTERVAL '11:00',
     120000.00, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '3 days')
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    space_id = EXCLUDED.space_id,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    total_price = EXCLUDED.total_price,
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at;

INSERT INTO payment (
    id, booking_id, amount, payment_method, status, paid_at, transaction_id
) VALUES
    (1, 2, 500000.00, 'BANK_TRANSFER', 'COMPLETED',
     CURRENT_TIMESTAMP - INTERVAL '12 hours', 'DEMO-TXN-0001'),
    (2, 3, 120000.00, 'CASH', 'COMPLETED',
     CURRENT_TIMESTAMP - INTERVAL '2 days', 'DEMO-TXN-0002')
ON CONFLICT (id) DO UPDATE SET
    booking_id = EXCLUDED.booking_id,
    amount = EXCLUDED.amount,
    payment_method = EXCLUDED.payment_method,
    status = EXCLUDED.status,
    paid_at = EXCLUDED.paid_at,
    transaction_id = EXCLUDED.transaction_id;

-- Keep identity sequences ahead of the fixed seed IDs.
SELECT setval(pg_get_serial_sequence('role', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM role;
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM users;
SELECT setval(pg_get_serial_sequence('amenity', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM amenity;
SELECT setval(pg_get_serial_sequence('venue', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM venue;
SELECT setval(pg_get_serial_sequence('space', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM space;
SELECT setval(pg_get_serial_sequence('booking', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM booking;
SELECT setval(pg_get_serial_sequence('payment', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM payment;

-- ════════════════════════════════════════════════════════════
-- V006: Add ROOM feature ("Quản Lý Phòng")
-- Lodging engine for hotel / motel / homestay shop types: room status board,
-- check-in / check-out, in-room item folio. Depends on ORDER + PRODUCT
-- (enforced in the app's feature dependency graph).
-- ════════════════════════════════════════════════════════════

INSERT INTO features (id, name, display_name, description, active, deleted)
VALUES (202601043, 'ROOM', 'Quản Lý Phòng',
        'Quản lý phòng khách sạn / nhà nghỉ / homestay: sơ đồ phòng, nhận phòng, trả phòng, ghi nợ dịch vụ trong phòng',
        TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;

SELECT setval(pg_get_serial_sequence('features', 'id'),
              GREATEST((SELECT MAX(id) FROM features), 202601043), true);

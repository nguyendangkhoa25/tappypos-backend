-- ============================================================
-- V010 — Optimistic-lock column on room_stay
-- Guards against concurrent checkout / reservation check-in both succeeding
-- (double settlement order, double-booking). JPA @Version on RoomStayEntity.
-- ============================================================

ALTER TABLE room_stay ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

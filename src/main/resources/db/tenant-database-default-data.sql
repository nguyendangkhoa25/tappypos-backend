/*!40101 SET NAMES utf8mb4 */;
/*!40101 SET CHARACTER SET utf8mb4 */;
/*!40101 SET COLLATION_CONNECTION = utf8mb4_0900_ai_ci */;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;


INSERT INTO `roles` VALUES (1,'SHOP_OWNER','Shop Owner - Full access to all features','2025-12-30 10:28:50','2025-12-30 10:28:50',0,NULL),(2,'MANAGER','Manager - Can manage shop, employees, and reports','2025-12-30 10:28:50','2025-12-30 10:28:50',0,NULL),(3,'RECEPTIONIST','Receptionist - Can manage appointments and customers','2025-12-30 10:28:50','2025-12-30 10:28:50',0,NULL),(4,'CLEANER','Cleaner - Can manage cleaning tasks and inventory','2025-12-30 10:28:50','2025-12-30 10:28:50',0,NULL),(5,'TECHNICIAN','Technician/Employee - Can view appointments and customer info','2025-12-30 10:28:50','2025-12-30 10:28:50',0,NULL);

INSERT INTO `users` VALUES (36202600001,'Administrator','nguyendangkhoa25@gmail.com','$2a$10$pyg6ud.T6WmFBtcsyBp2TujecrqKNifJZPmewv2aJDApOVZWxbbi6',NULL,'Jeff Trantow',1,1,1,1,'','2025-12-30 10:32:54','2026-02-25 05:22:15',0,NULL,NULL,'#d357fe','vi');

INSERT INTO `user_roles` VALUES (36202600001,1);

INSERT INTO `shop_info` VALUES (3920260101,'Tiệm tóc của tôi','','CTy THNN Tiệm Tóc',0.00,'s_invoice_user','12345678x@X','2424234234','','','4234234344','VIETTEL','','2025-12-30 10:28:29','2026-01-02 08:49:44',0,NULL,'2000/01','S-INVOICE','CM26KNP');

INSERT IGNORE INTO customers (id, name, phone, email, notes, zalo_id, facebook_id, preferred_services, allergies_or_sensitivities, hair_type, special_requests, deleted)
VALUES (68202600001, 'Khách lẻ', '0000000000', NULL, 'Khách hàng lẻ - không có thông tin liên hệ', NULL, NULL, NULL, NULL, NULL, NULL, FALSE);



INSERT INTO products (name, description, price_before_tax, tax, price, duration_minutes, commission_rate, quantity, active, product_as_service, created_by, updated_by)
VALUES('Cắt tóc nam 50k', '', 50000.00, 0.00, 50000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Cắt tóc nam 60k', '', 60000.00, 0.00, 60000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Lấy ráy tai 60k', '', 60000.00, 0.00, 60000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Đắp mặt 30k', '', 30000.00, 0.00, 30000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Lột mụn 30k', '', 30000.00, 0.00, 30000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Tẩy tế bảo 30k', '', 30000.00, 0.00, 30000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Gội đầu + Massage Mặt 60k', '', 60000.00, 0.00, 60000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Nặn mụn', '', 50000.00, 0.00, 50000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Cắt da tay 60k', '', 60000.00, 0.00, 60000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Cắt da chân 60k', '', 60000.00, 0.00, 60000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Đánh mắt 30k', '', 60000.00, 0.00, 60000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Nhổ tóc bạc', '', 50000.00, 0.00, 50000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Se lỗ ghèn 30k', '', 30000.00, 0.00, 30000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Massage cổ vai gáy 30p - 120k', '', 120000.00, 0.00, 120000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Nhuộm cơ bản', '', 300000.00, 0.00, 300000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Nhuộm đen', '', 200000.00, 0.00, 200000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Nhuộm thời trang', '', 350000.00, 0.00, 350000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Tẩy 1 lần 200k', '', 200000.00, 0.00, 200000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Uốn 300k', '', 300000.00, 0.00, 300000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Gội đầu 40k', '', 40000.00, 0.00, 40000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Nhuộm chuyên sân 600k', '', 600000.00, 0.00, 600000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator"),
      ('Uốn chuyên sâu 600k', '', 600000.00, 0.00, 600000.00, 0, 0.00, 0, 1, 1, "Administrator", "Administrator");


INSERT INTO features (id, name, display_name, description, active, created_at, updated_at, deleted, deleted_at)
VALUES
    (202601001, 'DASHBOARD', 'Bảng Điều Khiển', 'Xem tổng quan và thống kê chính của cửa hàng', 1, '2026-01-04 11:41:46', '2026-01-04 11:41:46', 0, NULL),
    (202601002, 'ORDER', 'Đơn Hàng', 'Quản lý đơn hàng, theo dõi trạng thái và lịch sử đơn hàng', 1, '2026-01-04 11:41:46', '2026-01-04 11:41:46', 0, NULL),
    (202601003, 'MY_WORK', 'Công Việc Của Tôi', 'Xem công việc được giao cho nhân viên hiện tại', 1, '2026-01-04 11:41:46', '2026-01-04 11:41:46', 0, NULL),
    (202601004, 'PRODUCT', 'Sản Phẩm & Dịch Vụ', 'Quản lý danh sách sản phẩm, dịch vụ, giá cả và hoa hồng', 1, '2026-01-04 11:41:46', '2026-01-04 11:41:46', 0, NULL),
    (202601005, 'PROMOTION', 'Khuyến Mãi', 'Tạo và quản lý các chương trình khuyến mãi, giảm giá', 1, '2026-01-04 11:41:46', '2026-01-04 11:41:46', 0, NULL),
    (202601006, 'EMPLOYEE', 'Nhân Viên', 'Quản lý nhân viên, chức vụ, lương cơ bản', 1, '2026-01-04 11:41:46', '2026-01-04 11:41:46', 0, NULL),
    (202601007, 'SALARY', 'Lương Nhân Viên', 'Quản lý bảng lương, tính toán lương, chi trả', 1, '2026-01-04 11:41:46', '2026-01-04 11:41:46', 0, NULL),
    (202601008, 'CUSTOMER', 'Khách Hàng', 'Quản lý thông tin khách hàng, lịch sử mua hàng', 1, '2026-01-04 11:41:46', '2026-01-04 11:41:46', 0, NULL),
    (202601009, 'INVOICE', 'Hóa Đơn', 'Quản lý hóa đơn, xuất hóa đơn điện tử', 1, '2026-01-04 11:41:46', '2026-01-04 11:41:46', 0, NULL),
    (202601010, 'REVENUE', 'Doanh Thu', 'Xem báo cáo doanh thu, lợi nhuận, chi phí', 1, '2026-01-04 11:41:46', '2026-01-04 11:41:46', 0, NULL),
    (202601011, 'USER', 'Người Dùng', 'Quản lý tài khoản người dùng, quyền truy cập', 1, '2026-01-04 11:41:46', '2026-01-04 11:41:46', 0, NULL),
    (202601012, 'SHOP_INFO', 'Thông Tin Cửa Hàng', 'Cập nhật thông tin cửa hàng, cấu hình hệ thống', 1, '2026-01-04 11:41:46', '2026-01-04 11:41:46', 0, NULL);

/*!40000 ALTER TABLE `role_features` DISABLE KEYS */;
INSERT INTO `role_features` VALUES (20260001,1,202601001,'2026-01-04 04:43:14'),(20260002,1,202601002,'2026-01-04 04:43:14'),(20260004,1,202601004,'2026-01-04 04:43:14'),(20260005,1,202601005,'2026-01-04 04:43:14'),(20260006,1,202601006,'2026-01-04 04:43:14'),(20260007,1,202601007,'2026-01-04 04:43:14'),(20260008,1,202601008,'2026-01-04 04:43:14'),(20260009,1,202601009,'2026-01-04 04:43:14'),(20260010,1,202601010,'2026-01-04 04:43:14'),(20260011,1,202601011,'2026-01-04 04:43:14'),(20260012,1,202601012,'2026-01-04 04:43:14'),(20260016,2,202601002,'2026-01-04 04:43:21'),(20260017,2,202601003,'2026-01-04 04:43:21'),(20260018,2,202601004,'2026-01-04 04:43:21'),(20260019,2,202601005,'2026-01-04 04:43:21'),(20260020,2,202601006,'2026-01-04 04:43:21'),(20260021,2,202601008,'2026-01-04 04:43:21'),(20260022,2,202601009,'2026-01-04 04:43:21'),(20260023,2,202601010,'2026-01-04 04:43:21'),(20260024,2,202601011,'2026-01-04 04:43:21'),(20260031,3,202601002,'2026-01-04 04:43:25'),(20260032,3,202601004,'2026-01-04 04:43:25'),(20260033,3,202601005,'2026-01-04 04:43:25'),(20260034,3,202601008,'2026-01-04 04:43:25'),(20260035,3,202601009,'2026-01-04 04:43:25'),(20260038,4,202601003,'2026-01-04 04:43:28'),(20260039,5,202601003,'2026-01-04 04:43:32'),(20260041,5,202601008,'2026-01-04 06:46:07'),(20260042,5,202601002,'2026-01-04 06:46:07');

-- Restore original character set
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
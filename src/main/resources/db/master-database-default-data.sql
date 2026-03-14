
SET NAMES utf8mb4;

INSERT INTO `features` VALUES (202601001,'DASHBOARD','Bảng Điều Khiển','Xem tổng quan và thống kê chính của cửa hàng',1,'2026-01-04 04:44:44','2026-01-04 04:44:44',0,NULL),(202601002,'ORDER','Đơn Hàng','Quản lý đơn hàng, theo dõi trạng thái và lịch sử đơn hàng',1,'2026-01-04 04:44:44','2026-01-04 04:44:44',0,NULL),(202601003,'MY_WORK','Công Việc Của Tôi','Xem công việc được giao cho nhân viên hiện tại',1,'2026-01-04 04:44:44','2026-01-04 04:44:44',0,NULL),(202601004,'PRODUCT','Sản Phẩm & Dịch Vụ','Quản lý danh sách sản phẩm, dịch vụ, giá cả và hoa hồng',1,'2026-01-04 04:44:44','2026-01-04 04:44:44',0,NULL),(202601005,'PROMOTION','Khuyến Mãi','Tạo và quản lý các chương trình khuyến mãi, giảm giá',1,'2026-01-04 04:44:44','2026-01-04 04:44:44',0,NULL),(202601006,'EMPLOYEE','Nhân Viên','Quản lý nhân viên, chức vụ, lương cơ bản',1,'2026-01-04 04:44:44','2026-01-04 04:44:44',0,NULL),(202601007,'SALARY','Lương Nhân Viên','Quản lý bảng lương, tính toán lương, chi trả',1,'2026-01-04 04:44:44','2026-01-04 04:44:44',0,NULL),(202601008,'CUSTOMER','Khách Hàng','Quản lý thông tin khách hàng, lịch sử mua hàng',1,'2026-01-04 04:44:44','2026-01-04 04:44:44',0,NULL),(202601009,'INVOICE','Hóa Đơn','Quản lý hóa đơn, xuất hóa đơn điện tử',1,'2026-01-04 04:44:44','2026-01-04 04:44:44',0,NULL),(202601010,'REVENUE','Doanh Thu','Xem báo cáo doanh thu, lợi nhuận, chi phí',1,'2026-01-04 04:44:44','2026-01-04 04:44:44',0,NULL),(202601011,'USER','Người Dùng','Quản lý tài khoản người dùng, quyền truy cập',1,'2026-01-04 04:44:44','2026-01-04 04:44:44',0,NULL),(202601012,'SHOP_INFO','Thông Tin Cửa Hàng','Cập nhật thông tin cửa hàng, cấu hình hệ thống',1,'2026-01-04 04:44:44','2026-01-04 04:44:44',0,NULL),(202601013,'TENANT_MGMT','Quản Lý Cửa Hàng','Quản lý các chi nhánh, cửa hàng trong hệ thống',1,'2026-01-04 04:47:12','2026-01-04 04:47:12',0,NULL);

-- Dumping data for table `roles`
INSERT INTO `roles` VALUES (202600001,'MASTER_TENANT','Tenant Master - Full access to all features','2026-01-04 02:11:30','2026-01-04 02:11:30',0,NULL);


-- Dumping data for table `role_features`
INSERT INTO `role_features` VALUES (20260001,202600001,202601013,'2026-01-04 04:48:03'),(20260002,202600001,202601011,'2026-01-04 08:44:05');


-- Dumping data for table `users`
INSERT INTO `users` VALUES (79260001,'Administrator','nguyendangkhoa25@gmail.com','$2a$10$pyg6ud.T6WmFBtcsyBp2TujecrqKNifJZPmewv2aJDApOVZWxbbi6',NULL,'Khoa Nguyen',1,1,1,1,NULL,'2026-01-04 02:08:32','2026-01-04 02:08:32',0,NULL,NULL,NULL,'vi');

-- Dumping data for table `user_roles`
INSERT INTO `user_roles` VALUES (79260001,202600001);

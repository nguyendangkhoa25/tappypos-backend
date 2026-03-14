-- Retail Platform Features SQL Insert
-- Generated for all store types: Convenience Store, Pharmacy, Book Store, Hardware Store, Building Materials, Restaurant, Coffee Shop
-- Date: 2026-03-17

SET NAMES utf8mb4;

-- Core Features (Available for All Store Types)
INSERT INTO `features` VALUES
(202601001,'DASHBOARD','Bảng Điều Khiển','Xem tổng quan và thống kê chính của cửa hàng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601002,'ORDER','Đơn Hàng','Quản lý đơn hàng, theo dõi trạng thái và lịch sử đơn hàng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601003,'MY_WORK','Công Việc Của Tôi','Xem công việc được giao cho nhân viên hiện tại',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601004,'PRODUCT','Sản Phẩm & Dịch Vụ','Quản lý danh sách sản phẩm, dịch vụ, giá cả và hoa hồng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601005,'PROMOTION','Khuyến Mãi','Tạo và quản lý các chương trình khuyến mãi, giảm giá',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601006,'EMPLOYEE','Nhân Viên','Quản lý nhân viên, chức vụ, lương cơ bản',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601007,'SALARY','Lương Nhân Viên','Quản lý bảng lương, tính toán lương, chi trả',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601008,'CUSTOMER','Khách Hàng','Quản lý thông tin khách hàng, lịch sử mua hàng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601009,'INVOICE','Hóa Đơn','Quản lý hóa đơn, xuất hóa đơn điện tử',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601010,'REVENUE','Doanh Thu','Xem báo cáo doanh thu, lợi nhuận, chi phí',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601011,'USER','Người Dùng','Quản lý tài khoản người dùng, quyền truy cập',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601012,'SHOP_INFO','Thông Tin Cửa Hàng','Cập nhật thông tin cửa hàng, cấu hình hệ thống',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601013,'TENANT_MGMT','Quản Lý Cửa Hàng','Quản lý các chi nhánh, cửa hàng trong hệ thống',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601014,'POS','Quầy Thanh Toán','Xử lý giao dịch, quản lý thanh toán khách hàng, xuất hóa đơn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601015,'INVENTORY','Quản Lý Kho','Theo dõi mức tồn kho, quản lý cấp phát, lịch sử kho',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601016,'SUPPLIER','Nhà Cung Cấp','Quản lý nhà cung cấp, đơn đặt hàng, mối quan hệ',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601017,'ANALYTICS','Phân Tích & Báo Cáo','Xem báo cáo phân tích, xu hướng, hiệu suất kinh doanh',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601018,'LOYALTY','Chương Trình Loyalty','Quản lý chương trình khách hàng thân thiết, điểm thưởng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601019,'BARCODE','Quản Lý Barcode/QrCode','Quét barcode nhanh, tạo mã QR, theo dõi sản phẩm',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601020,'SETTING','Cấu Hình Hệ Thống','Quản lý cấu hình hệ thống, giao diện, tùy chọn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202601021,'ACCESSIBILITY','Hỗ Trợ Truy Cập','Hỗ trợ WCAG 2.1, điều hướng bàn phím, đầu đọc màn hình',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL);

-- Convenience Store & General Retail Specific Features
INSERT INTO `features` VALUES
(202602001,'CONVN_EXPIRY','Kiểm Soát Hạn Sử Dụng','Giám sát ngày hết hạn sản phẩm và quản lý tươi mới hàng hóa',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202602002,'CONVN_LOWSTOCK','Cảnh Báo Hết Hàng','Thông báo tự động khi tồn kho dưới ngưỡng giới hạn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202602003,'CONVN_MULTILOC','Quản Lý Đa Chi Nhánh','Quản lý nhiều cửa hàng chi nhánh với kho tập trung',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL);

-- Pharmacy / Drug Store Specific Features
INSERT INTO `features` VALUES
(202603001,'PHARM_PRESCRIPTION','Quản Lý Đơn Thuốc','Xử lý và theo dõi đơn thuốc từ khách hàng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202603002,'PHARM_INTERACTION','Kiểm Tra Tương Tác Thuốc','Xác minh không có tương tác hại giữa các loại thuốc',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202603003,'PHARM_CONTROLLED','Theo Dõi Chất Kiểm Soát','Theo dõi hàng tồn kho thuốc kiểm soát và bán hàng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202603004,'PHARM_EXPIRY_ALERT','Cảnh Báo Hạn Sử Dụng','Giám sát hàng ngày hạn sử dụng, cảnh báo tự động hàng hóa hết hạn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202603005,'PHARM_TEMPERATURE','Giám Sát Nhiệt Độ','Theo dõi và ghi lại nhiệt độ bảo quản cho thuốc nhạy cảm',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202603006,'PHARM_BATCH','Theo Dõi Lô Hàng','Theo dõi lô hàng đầy đủ từ nhà cung cấp đến khách hàng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202603007,'PHARM_COMPLIANCE','Tuân Thủ Pháp Luật','Đảm bảo tuân thủ quy định DEA và quy định dược học',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202603008,'PHARM_REFILL','Quản Lý Làm Lại Đơn','Nhắc nhở làm lại tự động cho khách hàng thường xuyên',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202603009,'PHARM_INSURANCE','Tích Hợp Bảo Hiểm','Hỗ trợ thanh toán bảo hiểm và xác minh bảo hiểm',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL);

-- Book Store Specific Features
INSERT INTO `features` VALUES
(202604001,'BOOK_SEARCH','Tìm Kiếm Nâng Cao','Tìm kiếm sách theo tác giả, thể loại, ISBN, ngày xuất bản',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202604002,'BOOK_REVIEW','Đánh Giá & Bình Luận','Xem đánh giá khách hàng và bình luận sách',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202604003,'BOOK_GENRE','Quản Lý Tác Giả & Thể Loại','Sắp xếp sách theo tác giả, thể loại, dòng sách',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202604004,'BOOK_FORMAT','Quản Lý Theo Định Dạng','Phân biệt tồn kho cho bìa cứng, bìa mềm, ebook',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202604005,'BOOK_PREORDER','Quản Lý Đặt Trước','Cho phép khách hàng đặt trước sách sắp ra mắt',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202604006,'BOOK_DIGITAL','Quản Lý Nội Dung Kỹ Thuật Số','Hỗ trợ bán ebook và audiobook',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202604007,'BOOK_RECOMMEND','Đề Xuất Nhân Viên','Đề xuất sách từ nhân viên, sách nổi bật',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202604008,'BOOK_RETURN','Quản Lý Trả Hàng & Trao Đổi','Xử lý trả lại sách và trao đổi hàng dễ dàng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202604009,'BOOK_SUPPLIER','Tích Hợp Nhà Cung Cấp','Đồng bộ hóa kho với các nhà xuất bản/nhà phân phối lớn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL);

-- Hardware Store Specific Features
INSERT INTO `features` VALUES
(202605001,'HARD_RENTAL','Quản Lý Cho Thuê Dụng Cụ','Theo dõi cho thuê dụng cụ, thời hạn, phí, tiền cọc',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202605002,'HARD_BULK','Quản Lý Đơn Hàng Lớn','Xử lý đơn hàng lớn với giá đặc biệt',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202605003,'HARD_VARIANT','Quản Lý Theo Kích Cỡ/Biến Thể','Theo dõi kho theo kích cỡ (ví dụ: đinh dài khác nhau)',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202605004,'HARD_WEIGHT','Theo Dõi Trọng Lượng','Quản lý kho theo trọng lượng cho vật liệu lớn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202605005,'HARD_CONTRACTOR','Quản Lý Tài Khoản Nhà Thầu','Hỗ trợ tài khoản nhà thầu với điều khoản tín dụng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202605006,'HARD_SDS','Bảng An Toàn Dữ Liệu','Lưu trữ và hiển thị SDS cho vật liệu nguy hiểm',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202605007,'HARD_ASSEMBLY','Theo Dõi Dịch Vụ Lắp Ráp','Theo dõi dịch vụ lắp ráp/cài đặt được cung cấp',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202605008,'HARD_WARRANTY','Quản Lý Bảo Hành','Theo dõi bảo hành trên dụng cụ và thiết bị',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202605009,'HARD_EXPERT','Hướng Dẫn Chuyên Gia','Theo dõi lời khuyên từ chuyên gia và tư vấn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL);

-- Building Materials / Construction Supply Specific Features
INSERT INTO `features` VALUES
(202606001,'BUILD_BULK_QTY','Quản Lý Số Lượng Lớn','Theo dõi kho theo nhiều đơn vị (miếng, bó, pallet, tấn)',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202606002,'BUILD_CONTRACTOR','Quản Lý Tài Khoản Nhà Thầu','Giá đặc biệt, điều khoản tín dụng, theo dõi dự án cho nhà thầu',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202606003,'BUILD_PROJECT','Đơn Hàng Theo Dự Án','Tổ chức đơn hàng theo dự án xây dựng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202606004,'BUILD_WEIGHT','Theo Dõi Trọng Lượng & Kích Thước','Quan trọng cho vật liệu nặng (xi măng, thép, gỗ)',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202606005,'BUILD_DELIVERY','Quản Lý Giao Hàng','Lên lịch giao hàng, theo dõi trạng thái giao hàng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202606006,'BUILD_RETURN','Trả Hàng & Trao Đổi','Xử lý dễ dàng trả lại vật liệu bị lỗi hoặc dư thừa',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202606007,'BUILD_QUOTE','Tạo Báo Giá','Tạo báo giá cho các dự án lớn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202606008,'BUILD_CERT','Theo Dõi Chứng Chỉ','Theo dõi chứng chỉ và tuân thủ của vật liệu',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202606009,'BUILD_SUPPLIER','Quản Lý Mối Quan Hệ Nhà Cung Cấp','Theo dõi điều khoản, thời gian chuyên chở, số liệu chất lượng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202606010,'BUILD_SPEC','Tờ Thông Tin Kỹ Thuật','Lưu trữ thông số kỹ thuật và thông tin tương thích',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL);

-- Restaurant / Food Service Specific Features
INSERT INTO `features` VALUES
(202607001,'REST_MENU','Quản Lý Thực Đơn','Quản lý các mục thực đơn, công thức, nguyên liệu, giá cả',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202607002,'REST_RECIPE','Quản Lý Công Thức','Lưu trữ công thức với nguyên liệu và phần ăn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202607003,'REST_KITCHEN_DISPLAY','Hệ Thống Hiển Thị Bếp','Hệ thống bếp kỹ thuật số để quản lý đơn hàng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202607004,'REST_TABLE_MGMT','Quản Lý Bàn Ăn','Đặt chỗ/quản lý bàn ăn, theo dõi trạng thái bàn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202607005,'REST_INGREDIENT_COST','Theo Dõi Chi Phí Nguyên Liệu','Tính toán chi phí thực phẩm cho mỗi mục thực đơn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202607006,'REST_EXPIRY','Kiểm Soát Hạn Sử Dụng Thực Phẩm','Theo dõi ngày hết hạn nguyên liệu (quan trọng cho an toàn thực phẩm)',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202607007,'REST_DELIVERY','Quản Lý Giao Hàng','Theo dõi đơn giao hàng, nhân viên giao hàng, trạng thái',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202607008,'REST_RATING','Hệ Thống Đánh Giá Đánh Xếp','Đánh giá và xếp hạng khách hàng cho các món ăn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202607009,'REST_SHIFT','Quản Lý Ca Làm Việc Nhân Viên','Lên lịch ca làm việc, theo dõi chuyên cần',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202607010,'REST_WASTE','Theo Dõi Lãng Phí Thực Phẩm','Theo dõi lãng phí thực phẩm để phân tích chi phí',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202607011,'REST_ALLERGEN','Quản Lý Chất Gây Dị Ứng','Theo dõi chất gây dị ứng trong các mục thực đơn cho an toàn khách hàng',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202607012,'REST_TEMPERATURE','Giám Sát Nhiệt Độ Bảo Quản','Giám sát nhiệt độ bảo quản (lạnh, nóng)',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL);

-- Coffee Shop / Cafe Specific Features
INSERT INTO `features` VALUES
(202608001,'COFFEE_BEAN','Quản Lý Hạt Cà Phê','Theo dõi kho hạt cà phê, ngày rang, gốc, ghi chú hương vị',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202608002,'COFFEE_CUSTOMIZE','Tùy Chỉnh Đồ Uống','Theo dõi tùy chỉnh (số shots, loại sữa, syrup, toppings)',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202608003,'COFFEE_EQUIPMENT','Quản Lý Thiết Bị','Theo dõi lịch bảo trì máy espresso, máy xay','1','2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202608004,'COFFEE_RECIPE','Quản Lý Công Thức Đồ Uống','Lưu trữ công thức đồ uống với nguyên liệu và tỷ lệ',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202608005,'COFFEE_SIZE_INVENTORY','Quản Lý Tồn Kho Theo Kích Cỡ','Theo dõi cốc và vật dụng theo các kích cỡ khác nhau',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202608006,'COFFEE_MILK_TRACKING','Theo Dõi Sữa & Nguyên Liệu','Theo dõi tiêu thụ sữa, mức syrup, tồn kho bánh nước',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202608007,'COFFEE_QUEUE','Quản Lý Hàng Chờ','Hiển thị hàng chờ cho đơn hàng trực tuyến',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202608008,'COFFEE_PREFERENCES','Lưu Thích Khách Hàng','Lưu tùy chọn đồ uống khách hàng để gọi nhanh hơn',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202608009,'COFFEE_PROMOTION','Theo Dõi Khuyến Mãi','Mã giảm giá, điểm loyalty, chiến dịch quảng cáo',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202608010,'COFFEE_MULTILOC','Hỗ Trợ Đa Chi Nhánh','Quản lý nhiều chi nhánh café với chương trình loyalty chung',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL),
(202608011,'COFFEE_MOBILE_ORDER','Đặt Hàng Di Động','Hỗ trợ đặt hàng qua ứng dụng di động và đặt trước',1,'2026-03-17 00:00:00','2026-03-17 00:00:00',0,NULL);


package com.tappy.pos.model.enums;

import lombok.Getter;

/**
 * FeatureEnum - Enum for all system features
 * Features are stored in the master database and can be assigned to roles
 * Each feature represents a module or page in the application
 *
 * Format: KEY, VIETNAMESE_NAME, VIETNAMESE_DESCRIPTION
 */
@Getter
public enum FeatureEnum {
    DASHBOARD("Bảng Điều Khiển", "Xem tổng quan và thống kê chính của cửa hàng"),
    ORDER("Đơn Hàng", "Quản lý đơn hàng, theo dõi trạng thái và lịch sử đơn hàng"),
    ORDER_VIEW_ALL("Xem Tất Cả Đơn Hàng", "Xem đơn hàng của tất cả nhân viên; nếu không có quyền này, chỉ xem được đơn hàng tự tạo"),
    MY_WORK("Công Việc Của Tôi", "Xem công việc được giao cho nhân viên hiện tại"),
    PRODUCT("Sản Phẩm & Dịch Vụ", "Quản lý danh sách sản phẩm, dịch vụ, giá cả và hoa hồng"),
    PROMOTION("Khuyến Mãi", "Tạo và quản lý các chương trình khuyến mãi, giảm giá"),
    EMPLOYEE("Nhân Viên", "Quản lý nhân viên, chức vụ, lương cơ bản"),
    SALARY("Lương Nhân Viên", "Quản lý bảng lương, tính toán lương, chi trả"),
    SALARY_VIEW_ALL("Xem Tất Cả Bảng Lương", "Xem bảng lương của tất cả nhân viên; nếu không có quyền này, chỉ xem được bảng lương của bản thân"),
    CUSTOMER("Khách Hàng", "Quản lý thông tin khách hàng, lịch sử mua hàng"),
    LOYALTY("Tích Điểm Khách Hàng", "Chương trình tích điểm và phần thưởng khách hàng"),
    CUSTOMER_DEBT("Công Nợ Khách Hàng", "Bán chịu, ghi sổ nợ và thu nợ khách hàng (thường dùng cho vật liệu xây dựng)"),
    INVOICE("Hóa Đơn", "Quản lý hóa đơn, xuất hóa đơn điện tử"),
    REVENUE("Doanh Thu", "Xem báo cáo doanh thu, lợi nhuận"),
    EXPENSE("Chi Phí", "Theo dõi và quản lý chi phí hoạt động cửa hàng"),
    USER("Người Dùng", "Quản lý tài khoản người dùng, quyền truy cập"),
    SHOP_INFO("Thông Tin Cửa Hàng", "Cập nhật thông tin cửa hàng, cấu hình hệ thống"),
    VENDOR("Nhà Cung Cấp", "Quản lý nhà cung cấp, đơn đặt hàng, nhập hàng"),
    INVENTORY("Quản Lý Kho", "Quản lý tồn kho, nhập xuất kho và kiểm kho"),
    STOCK_TAKE("Kiểm Kho", "Kiểm kê tồn kho thực tế bằng cách quét mã, đối chiếu và điều chỉnh chênh lệch so với hệ thống"),
    RECIPE("Định Lượng & Sản Xuất", "Quản lý công thức/định lượng nguyên liệu cho thành phẩm, tính giá vốn thật và sản xuất (làm bánh) trừ kho nguyên liệu, cộng kho thành phẩm"),
    ROOM("Quản Lý Phòng", "Quản lý phòng khách sạn / nhà nghỉ / homestay: sơ đồ phòng, nhận phòng, trả phòng, ghi nợ dịch vụ trong phòng"),
    POS("Điểm Bán Hàng", "Bán hàng tại quầy, thanh toán và in hóa đơn"),
    ACTIVITY_LOG("Nhật Ký Hoạt Động", "Xem nhật ký hoạt động của người dùng trong cửa hàng"),
    PAWN("Cầm Đồ", "Quản lý hợp đồng cầm đồ, lãi suất và thanh lý tài sản"),
    PAWN_VIEW_ALL("Xem Tất Cả Cầm Đồ", "Xem hợp đồng cầm đồ của tất cả nhân viên; nếu không có quyền này, chỉ xem được hợp đồng tự tạo"),
    NOTIFICATION("Thông Báo", "Nhận thông báo và nhắc nhở từ hệ thống"),
    FEEDBACK("Góp Ý", "Gửi phản hồi và đề xuất đến quản trị hệ thống"),
    PRINT_TEMPLATE("Mẫu In", "Quản lý mẫu in biên nhận và hóa đơn"),
    BANK_ACCOUNT("Tài Khoản Ngân Hàng", "Quản lý tài khoản ngân hàng của cửa hàng"),
    ACCOUNTING("Kế Toán", "Xem báo cáo kế toán tổng hợp"),
    GOLD_PRICE("Bảng Giá Vàng", "Quản lý bảng giá vàng theo tuổi, dùng cho tính giá mua/bán và cầm đồ"),
    GOLD_PRICE_CHART("Biểu Đồ Giá Vàng", "Xem biểu đồ giá vàng thế giới (XAU/USD) theo thời gian thực"),
    COMMISSION("Hoa Hồng Nhân Viên", "Gán nhân viên thực hiện và tính hoa hồng cho từng sản phẩm/dịch vụ trong đơn hàng"),
    COMMISSION_VIEW_ALL("Xem Hoa Hồng Toàn Đội", "Xem hoa hồng của tất cả nhân viên và báo cáo tổng; nếu không có quyền này, chỉ xem được hoa hồng của bản thân"),
    GOOGLE_DRIVE("Tích Hợp Google Drive", "Kết nối Google Drive cá nhân để lưu ảnh sản phẩm, hình căn cước khách hàng và ảnh hợp đồng cầm đồ"),
    APPOINTMENT("Lịch Hẹn", "Quản lý lịch hẹn với khách hàng, đặt lịch và xác nhận"),
    TABLE_SERVICE("Quản Lý Bàn", "Theo dõi trạng thái bàn và gọi món theo bàn cho quán ăn / quán nhậu"),
    BOOKING("Đặt Bàn / Đặt Sân", "Quản lý bàn bida, sân thể thao: tính giờ chơi, đặt sân theo giờ và tạo hoá đơn khi kết thúc"),
    UTILITIES("Tiện Ích", "Bộ công cụ tính toán: tính lãi, khoản vay, thuế, ngân sách, đổi tiền, giá vàng thị trường, chia hóa đơn, điểm hòa vốn"),
    REPAIR("Sửa Chữa", "Quản lý phiếu sửa chữa thiết bị: tiếp nhận máy, ghi lỗi, báo giá, giao thợ, theo dõi tình trạng và bảo hành sửa chữa"),
    REPAIR_VIEW_ALL("Xem Tất Cả Phiếu Sửa Chữa", "Xem phiếu sửa chữa của tất cả nhân viên; nếu không có quyền này, chỉ xem được phiếu tự tạo"),
    BUYBACK("Mua Bán Đồ Cũ", "Mua đồ cũ của khách rồi bán lại; dùng cho tiệm vàng, cửa hàng xe máy, đồ cũ"),
    TRADE_IN("Thu Cũ Đổi Mới", "Định giá xe cũ của khách và quy đổi vào đơn bán xe mới hoặc mua đứt; dùng cho cửa hàng xe"),
    TRADE_IN_VIEW_ALL("Xem Tất Cả Phiếu Thu Cũ", "Xem phiếu thu cũ đổi mới của tất cả nhân viên; nếu không có quyền này, chỉ xem được phiếu tự tạo"),
    INSTALLMENT("Bán Trả Góp", "Bán hàng trả góp theo nhiều kỳ: lập lịch trả, theo dõi kỳ đến hạn và thu tiền từng kỳ"),
    INSTALLMENT_VIEW_ALL("Xem Tất Cả Hợp Đồng Trả Góp", "Xem hợp đồng trả góp của tất cả nhân viên; nếu không có quyền này, chỉ xem được hợp đồng tự tạo"),
    CONSIGNMENT("Ký Gửi Hàng", "Nhận hàng ký gửi từ nhà cung cấp/NXB, theo dõi số lượng đã bán và thanh toán theo doanh số; dùng cho nhà sách, cửa hàng ký gửi"),
    CONSIGNMENT_VIEW_ALL("Xem Tất Cả Phiếu Ký Gửi", "Xem phiếu ký gửi của tất cả nhân viên; nếu không có quyền này, chỉ xem được phiếu tự tạo"),

    // ── Master-tenant / agent features (no shop access) — seeded in V001, enforced via @RequiresFeature ──
    TENANT_MGMT("Quản Lý Cửa Hàng", "Quản lý các cửa hàng (tenant) trong hệ thống — chỉ dành cho cơ sở dữ liệu chính"),
    AGENT_MGMT("Quản Lý Đại Lý", "Quản lý các đại lý bán hàng của nền tảng — chỉ dành cho cơ sở dữ liệu chính"),
    MASTER_DASHBOARD("Bảng Điều Khiển Tổng", "Thống kê toàn nền tảng cho quản trị viên và đại lý"),
    FEEDBACK_MGMT("Quản Lý Góp Ý", "Xem và xử lý góp ý gửi từ các cửa hàng — chỉ dành cho cơ sở dữ liệu chính"),
    CONTACT_LEAD_MGMT("Quản Lý Yêu Cầu Liên Hệ", "Quản lý yêu cầu dùng thử từ trang giới thiệu — chỉ dành cho cơ sở dữ liệu chính"),
    PRODUCT_CATALOG("Danh Mục Sản Phẩm Chung", "Cơ sở dữ liệu mã vạch sản phẩm dùng chung — chỉ dành cho cơ sở dữ liệu chính");

    /**
     * -- GETTER --
     *  Get the Vietnamese display name
     */
    private final String displayName;
    /**
     * -- GETTER --
     *  Get the Vietnamese description
     */
    private final String description;

    FeatureEnum(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the feature key (enum name)
     */
    public String getKey() {
        return this.name();
    }

    /**
     * Check if a feature key exists
     */
    public static boolean exists(String key) {
        try {
            FeatureEnum.valueOf(key);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get a feature by its key
     */
    public static FeatureEnum getByKey(String key) {
        try {
            return FeatureEnum.valueOf(key);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}


package com.knp.model.enums;

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
    MY_WORK("Công Việc Của Tôi", "Xem công việc được giao cho nhân viên hiện tại"),
    PRODUCT("Sản Phẩm & Dịch Vụ", "Quản lý danh sách sản phẩm, dịch vụ, giá cả và hoa hồng"),
    PROMOTION("Khuyến Mãi", "Tạo và quản lý các chương trình khuyến mãi, giảm giá"),
    EMPLOYEE("Nhân Viên", "Quản lý nhân viên, chức vụ, lương cơ bản"),
    SALARY("Lương Nhân Viên", "Quản lý bảng lương, tính toán lương, chi trả"),
    CUSTOMER("Khách Hàng", "Quản lý thông tin khách hàng, lịch sử mua hàng"),
    LOYALTY("Tích Điểm Khách Hàng", "Chương trình tích điểm và phần thưởng khách hàng"),
    INVOICE("Hóa Đơn", "Quản lý hóa đơn, xuất hóa đơn điện tử"),
    REVENUE("Doanh Thu", "Xem báo cáo doanh thu, lợi nhuận"),
    EXPENSE("Chi Phí", "Theo dõi và quản lý chi phí hoạt động cửa hàng"),
    USER("Người Dùng", "Quản lý tài khoản người dùng, quyền truy cập"),
    SHOP_INFO("Thông Tin Cửa Hàng", "Cập nhật thông tin cửa hàng, cấu hình hệ thống"),
    VENDOR("Nhà Cung Cấp", "Quản lý nhà cung cấp, đơn đặt hàng, nhập hàng"),
    INVENTORY("Quản Lý Kho", "Quản lý tồn kho, nhập xuất kho và kiểm kho"),
    POS("Điểm Bán Hàng", "Bán hàng tại quầy, thanh toán và in hóa đơn"),
    ACTIVITY_LOG("Nhật Ký Hoạt Động", "Xem nhật ký hoạt động của người dùng trong cửa hàng"),
    PAWN("Cầm Đồ", "Quản lý hợp đồng cầm đồ, lãi suất và thanh lý tài sản"),
    NOTIFICATION("Thông Báo", "Nhận thông báo và nhắc nhở từ hệ thống"),
    FEEDBACK("Góp Ý", "Gửi phản hồi và đề xuất đến quản trị hệ thống"),
    PRINT_TEMPLATE("Mẫu In", "Quản lý mẫu in biên nhận và hóa đơn"),
    BANK_ACCOUNT("Tài Khoản Ngân Hàng", "Quản lý tài khoản ngân hàng của cửa hàng"),
    ACCOUNTING("Kế Toán", "Xem báo cáo kế toán tổng hợp");

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


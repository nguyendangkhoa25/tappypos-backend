package com.knp.util;

import com.knp.model.entity.customer.Customer;
import com.knp.model.entity.pawn.PawnQuery;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Map.entry;

@Slf4j
public class FastExcelHelper {
    public static String PAWN_ID = "PAWN_ID";
    public static String CUSTOMER_ID = "CUSTOMER_ID";
    public static String CUSTOMER_NAME = "CUSTOMER_NAME";
    public static String CUSTOMER_PHONE = "CUSTOMER_PHONE";
    public static String ITEM_NAME = "ITEM_NAME";
    public static String ITEM_WEIGHT = "ITEM_WEIGHT";
    public static String ITEM_TYPE = "ITEM_TYPE";
    public static String ITEM_BRAND = "ITEM_BRAND";
    public static String GEM_WEIGHT = "GEM_WEIGHT";
    public static String PAWN_DATE = "PAWN_DATE";
    public static String PAWN_DUE_DATE = "PAWN_DUE_DATE";
    public static String PAWN_AMOUNT = "PAWN_AMOUNT";
    public static String REQUEST_MORE = "REQUEST_MORE";
    public static String ITEM_VALUE = "ITEM_VALUE";
    public static String INTEREST_RATE = "INTEREST_RATE";
    public static String INTEREST_CAL_TYPE = "INTEREST_CAL_TYPE";
    public static String STATUS = "STATUS";
    public static String REDEEM_DATE = "REDEEM_DATE";
    public static String INTEREST_AMOUNT = "INTEREST_AMOUNT";
    public static String TOTAL_AMOUNT = "TOTAL_AMOUNT";
    public static String FORFEITED_DATE = "FORFEITED_DATE";
    public static String FORFEITED_AMOUNT = "FORFEITED_AMOUNT";
    public static String FORFEITED_REASON = "FORFEITED_REASON";
    public static String CANCELED_REASON = "CANCELED_REASON";
    public static String CANCELED_DATE = "CANCELED_DATE";
    public static String CREATED_DATE = "CREATED_DATE";
    public static String CREATED_BY = "CREATED_BY";
    public static String UPDATED_BY = "UPDATED_BY";
    public static String UPDATED_DATE = "UPDATED_DATE";

    public static void exportPawnDataToExcel(String fileLocation,
                                             List<PawnQuery> reportEntities) throws IOException {
        log.info("Start writing Excel file!");
        try (OutputStream os = new FileOutputStream(fileLocation)) {
            Workbook workbook = new Workbook(os, "QL TIEM VANG", "1.0");
            Worksheet worksheet = workbook.newWorksheet("CAM DO");
            for (ExcelHeader excelHeader : PAWN_EXCEL_MATRIX.values()) {
                if (!excelHeader.isSkipHeader())
                    worksheet.value(0, excelHeader.getIndex(), excelHeader.getTitle());
            }
            AtomicInteger rowIndex = new AtomicInteger(1);
            reportEntities.forEach(pawn -> {
                log.info("Processing row PawnId {}", pawn.getPawnId());
                writePawnData(worksheet, rowIndex.getAndIncrement(), pawn);
            });
            workbook.finish();
        } catch (Exception e) {
            log.error("Exception while writing excel file", e);
            throw e;
        }
    }

    public static FileSystemResource downloadFile(String fileLocation) {
        log.info("Download file {}!", fileLocation);
        File file = new File(fileLocation);
        return new FileSystemResource(file);
    }

    public static String getExportFullDir() {
        String curDir = new File(".").getAbsolutePath();
        String directoryName = curDir.substring(0, curDir.length() - 1) + "export/";
        File directory = new File(directoryName);
        if (!directory.exists()) {
            boolean mkdir = directory.mkdir();
            if (!mkdir) {
                throw new RuntimeException(String.format("Unable to create directory %s", directoryName));
            }
        }
        return directory.getAbsolutePath();
    }

    public static String getExportFileFullDir(String fileName) {
        String directoryName = getExportFullDir();
        return Paths.get(directoryName, fileName).toString();
    }

    private synchronized static void writePawnData(Worksheet worksheet, int rowIndex, PawnQuery pawnQuery) {
        var customer = pawnQuery.getCustomer() != null ? pawnQuery.getCustomer() : new Customer();
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(PAWN_ID).getIndex(), String.valueOf(pawnQuery.getPawnId()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(CUSTOMER_ID).getIndex(), customer.getId() != null ? String.valueOf(customer.getId()) : "");
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(CUSTOMER_NAME).getIndex(), customer.getName());
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(CUSTOMER_PHONE).getIndex(), customer.getPhone());
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(ITEM_NAME).getIndex(), pawnQuery.getItemName());
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(ITEM_WEIGHT).getIndex(), NumberUtil.weightToString(pawnQuery.getItemWeight()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(ITEM_TYPE).getIndex(), pawnQuery.getItemType());
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(ITEM_BRAND).getIndex(), pawnQuery.getItemBrand());
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(GEM_WEIGHT).getIndex(), NumberUtil.weightToString(pawnQuery.getGemWeight()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(PAWN_DATE).getIndex(), DateTimeUtil.localDateTimeToString(pawnQuery.getPawnDate()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(PAWN_DUE_DATE).getIndex(), DateTimeUtil.localDateTimeToString(pawnQuery.getPawnDueDate()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(PAWN_AMOUNT).getIndex(), NumberUtil.amountToString(pawnQuery.getPawnAmount()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(REQUEST_MORE).getIndex(), String.valueOf(pawnQuery.getPawnId()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(ITEM_VALUE).getIndex(), NumberUtil.amountToString(pawnQuery.getItemValue()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(INTEREST_RATE).getIndex(), NumberUtil.amountToString(pawnQuery.getInterestRate()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(INTEREST_CAL_TYPE).getIndex(), PawnUtil.getPawnInterestCalculation(pawnQuery.getInterestDaysPerMonth()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(STATUS).getIndex(), pawnQuery.getPawnStatus() != null ? pawnQuery.getPawnStatus().label : "");
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(REDEEM_DATE).getIndex(), DateTimeUtil.localDateTimeToString(pawnQuery.getRedeemDate()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(INTEREST_AMOUNT).getIndex(), NumberUtil.amountToString(pawnQuery.getInterestAmount()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(TOTAL_AMOUNT).getIndex(), NumberUtil.amountToString(pawnQuery.getTotalAmount()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(FORFEITED_DATE).getIndex(), DateTimeUtil.localDateTimeToString(pawnQuery.getForfeitedDate()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(FORFEITED_AMOUNT).getIndex(), NumberUtil.amountToString(pawnQuery.getForfeitedAmount()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(FORFEITED_REASON).getIndex(), pawnQuery.getForfeitedReason());
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(CANCELED_REASON).getIndex(), pawnQuery.getCanceledReason());
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(CREATED_DATE).getIndex(), DateTimeUtil.localDateTimeToString(pawnQuery.getCreatedAt()));
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(CREATED_BY).getIndex(), pawnQuery.getCreatedBy());
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(UPDATED_BY).getIndex(), pawnQuery.getUpdatedBy());
        writeData(worksheet, rowIndex, PAWN_EXCEL_MATRIX.get(UPDATED_DATE).getIndex(), DateTimeUtil.localDateTimeToString(pawnQuery.getUpdatedAt()));
    }

    private synchronized static void writeData(Worksheet worksheet, int rowNumber, int colNumber, String value) {
        worksheet.value(rowNumber, colNumber, value);
    }

    public static final Map<String, ExcelHeader> PAWN_EXCEL_MATRIX = Map.ofEntries(
            entry(PAWN_ID, new ExcelHeader(0, "Mã đơn")),
            entry(CUSTOMER_ID, new ExcelHeader(1, "Mã KH")),
            entry(CUSTOMER_NAME, new ExcelHeader(2, "Tên KH")),
            entry(CUSTOMER_PHONE, new ExcelHeader(3, "Số Đt")),
            entry(ITEM_NAME, new ExcelHeader(4, "Món hàng")),
            entry(ITEM_WEIGHT, new ExcelHeader(5, "TL vàng")),
            entry(ITEM_TYPE, new ExcelHeader(6, "Tuổi")),
            entry(ITEM_BRAND, new ExcelHeader(7, "Chành")),
            entry(GEM_WEIGHT, new ExcelHeader(8, "TL hột")),
            entry(PAWN_DATE, new ExcelHeader(9, "Ngày cầm")),
            entry(PAWN_DUE_DATE, new ExcelHeader(10, "Ngày đến hạn")),
            entry(PAWN_AMOUNT, new ExcelHeader(11, "Số tiền cầm")),
            entry(REQUEST_MORE, new ExcelHeader(12, "Lấy thêm")),
            entry(ITEM_VALUE, new ExcelHeader(13, "Số tiền cầm tối đa")),
            entry(INTEREST_RATE, new ExcelHeader(14, "Lãi suất")),
            entry(INTEREST_CAL_TYPE, new ExcelHeader(15, "Kiểu tính lãi")),
            entry(STATUS, new ExcelHeader(16, "Trạng thái")),
            entry(REDEEM_DATE, new ExcelHeader(17, "Ngày chuộc")),
            entry(INTEREST_AMOUNT, new ExcelHeader(18, "Tiền lãi")),
            entry(TOTAL_AMOUNT, new ExcelHeader(19, "Số tiền thu")),
            entry(FORFEITED_DATE, new ExcelHeader(20, "Ngày thanh lý")),
            entry(FORFEITED_AMOUNT, new ExcelHeader(21, "Tiền thanh lý")),
            entry(FORFEITED_REASON, new ExcelHeader(22, "Lý do thanh lý")),
            entry(CANCELED_REASON, new ExcelHeader(23, "Lý do hủy")),
            entry(CANCELED_DATE, new ExcelHeader(24, "Ngày do hủy")),
            entry(CREATED_DATE, new ExcelHeader(25, "Ngày tạo")),
            entry(CREATED_BY, new ExcelHeader(26, "Người tạo")),
            entry(UPDATED_BY, new ExcelHeader(27, "Người sửa cuối")),
            entry(UPDATED_DATE, new ExcelHeader(28, "Ngày sửa cuối"))
    );
}

@Getter
@AllArgsConstructor
class ExcelHeader {
    private int index;
    private String title;
    private boolean skipHeader = false;

    public ExcelHeader(int index, String title) {
        this.index = index;
        this.title = title;
    }
}

package com.tappy.pos.service.tax;

import com.tappy.pos.model.dto.tax.TaxDeclarationDTO;
import com.tappy.pos.model.dto.tax.TaxDeclarationRequest;
import com.tappy.pos.model.dto.tax.TaxEstimateDTO;
import com.tappy.pos.model.dto.tax.TaxRateCatalogDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaxDeclarationService {

    /** Danh mục nhóm ngành thuế + tỷ lệ (cho dropdown). */
    List<TaxRateCatalogDTO> getRateCatalog();

    /** Ước tính nhanh thuế của một kỳ (chưa lưu). */
    TaxEstimateDTO estimate(String periodType, int year, int number);

    Page<TaxDeclarationDTO> list(Integer year, Pageable pageable);

    TaxDeclarationDTO getById(Long id);

    TaxDeclarationDTO createDraft(TaxDeclarationRequest request);

    TaxDeclarationDTO update(Long id, TaxDeclarationRequest request);

    TaxDeclarationDTO finalizeDeclaration(Long id);

    TaxDeclarationDTO markSubmitted(Long id, String govRefNumber);

    void cancel(Long id);

    /**
     * Xuất bản in HTML của tờ khai (mẫu 01/CNKD) — chủ shop "In → Lưu PDF" để nộp.
     * Bản render PDF/XML chuẩn schema là bước nâng cấp sau (xem TAX_DECLARATION_TECH_PLAN §1.6).
     */
    byte[] exportPrintableHtml(Long id);
}

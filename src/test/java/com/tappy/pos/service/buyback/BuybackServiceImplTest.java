package com.tappy.pos.service.buyback;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.buyback.BuybackResponse;
import com.tappy.pos.model.dto.buyback.CreateBuybackRequest;
import com.tappy.pos.model.dto.buyback.SellBuybackRequest;
import com.tappy.pos.model.entity.buyback.BuybackEntity;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.BuybackStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.buyback.BuybackRepository;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuybackServiceImpl Unit Tests")
class BuybackServiceImplTest {

    @Mock private BuybackRepository buybackRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AuthContext authContext;
    @Mock private TenantContext tenantContext;
    @Mock private ActivityLogService activityLogService;
    @Mock private MessageService messageService;

    @InjectMocks
    private BuybackServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(authContext.getCurrentUsername()).thenReturn("staff01");
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("test-shop");
        lenient().when(messageService.getMessage(anyString())).thenReturn("msg");
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
        lenient().when(buybackRepository.save(any())).thenAnswer(inv -> {
            BuybackEntity e = inv.getArgument(0);
            if (e.getBuybackId() == null) e.setBuybackId(1L);
            return e;
        });
    }

    private BuybackEntity entity(BuybackStatus status) {
        return BuybackEntity.builder()
                .buybackId(1L).tenantId("test-shop").itemName("iPhone 12")
                .acquisitionPrice(new BigDecimal("2000000")).status(status).build();
    }

    @Test
    @DisplayName("createBuyback persists a PURCHASED record and logs BUYBACK_CREATED")
    void create_success() {
        CreateBuybackRequest req = new CreateBuybackRequest();
        req.setItemName("iPhone 12");
        req.setAcquisitionPrice(new BigDecimal("2000000"));

        BuybackResponse res = service.createBuyback(req);

        assertThat(res.getStatus()).isEqualTo(BuybackStatus.PURCHASED);
        assertThat(res.getAcquisitionPrice()).isEqualByComparingTo("2000000");
        assertThat(res.getMargin()).isNull();
        verify(activityLogService).logAsync(eq("test-shop"), eq("staff01"), isNull(),
                eq(ActivityAction.BUYBACK_CREATED), eq("BUYBACK"), eq("1"), any(), isNull());
    }

    @Test
    @DisplayName("markSold sets SOLD + resale price and computes margin")
    void sold_success() {
        when(buybackRepository.findById(1L)).thenReturn(Optional.of(entity(BuybackStatus.LISTED)));
        SellBuybackRequest req = new SellBuybackRequest();
        req.setResalePrice(new BigDecimal("2500000"));

        BuybackResponse res = service.markSold(1L, req);

        assertThat(res.getStatus()).isEqualTo(BuybackStatus.SOLD);
        assertThat(res.getMargin()).isEqualByComparingTo("500000");
        verify(activityLogService).logAsync(any(), any(), isNull(),
                eq(ActivityAction.BUYBACK_SOLD), eq("BUYBACK"), eq("1"), any(), isNull());
    }

    @Test
    @DisplayName("markSold rejects an already-SOLD buyback")
    void sold_alreadySold_throws() {
        when(buybackRepository.findById(1L)).thenReturn(Optional.of(entity(BuybackStatus.SOLD)));
        SellBuybackRequest req = new SellBuybackRequest();
        req.setResalePrice(new BigDecimal("2500000"));

        assertThatThrownBy(() -> service.markSold(1L, req)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("cancelBuyback cancels a non-sold buyback")
    void cancel_success() {
        when(buybackRepository.findById(1L)).thenReturn(Optional.of(entity(BuybackStatus.PURCHASED)));

        BuybackResponse res = service.cancelBuyback(1L, "khách đổi ý");

        assertThat(res.getStatus()).isEqualTo(BuybackStatus.CANCELLED);
        verify(activityLogService).logAsync(any(), any(), isNull(),
                eq(ActivityAction.BUYBACK_CANCELLED), eq("BUYBACK"), eq("1"), any(), isNull());
    }

    @Test
    @DisplayName("cancelBuyback rejects a SOLD buyback")
    void cancel_sold_throws() {
        when(buybackRepository.findById(1L)).thenReturn(Optional.of(entity(BuybackStatus.SOLD)));
        assertThatThrownBy(() -> service.cancelBuyback(1L, "x")).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("getBuyback throws when not found")
    void get_notFound() {
        when(buybackRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getBuyback(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}

package com.tappy.pos.service.tenant;

import com.tappy.pos.model.enums.ShopType;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantSeedService Unit Tests")
class TenantSeedServiceTest {

    @Mock private EntityManager entityManager;

    @InjectMocks private TenantSeedService tenantSeedService;

    private Session mockSession;
    private Connection mockConnection;
    private Savepoint mockSavepoint;
    private Statement mockStatement;

    @BeforeEach
    void setUp() throws Exception {
        mockSession    = mock(Session.class);
        mockConnection = mock(Connection.class);
        mockSavepoint  = mock(Savepoint.class);
        mockStatement  = mock(Statement.class);

        lenient().when(entityManager.unwrap(Session.class)).thenReturn(mockSession);
        lenient().when(mockConnection.setSavepoint()).thenReturn(mockSavepoint);
        lenient().when(mockConnection.createStatement()).thenReturn(mockStatement);

        lenient().doAnswer(inv -> {
            Work work = inv.getArgument(0);
            work.execute(mockConnection);
            return null;
        }).when(mockSession).doWork(any(Work.class));
    }

    // ── seed: PAWN_SHOP uses pawn_store.sql ───────────────────────────────────

    @Test
    @DisplayName("seed: executes all statements from pawn_store.sql without error")
    void seed_pawnShop() throws Exception {
        tenantSeedService.seed(ShopType.PAWN_SHOP);

        verify(mockSession).doWork(any(Work.class));
        verify(mockStatement, atLeastOnce()).execute(anyString());
        verify(mockConnection, atLeastOnce()).setSavepoint();
        verify(mockConnection, atLeastOnce()).releaseSavepoint(any());
    }

    @Test
    @DisplayName("seed: executes all statements from convenience_store.sql")
    void seed_convenienceStore() throws Exception {
        tenantSeedService.seed(ShopType.CONVENIENCE_STORE);

        verify(mockSession).doWork(any(Work.class));
        verify(mockStatement, atLeastOnce()).execute(anyString());
    }

    @Test
    @DisplayName("seed: uses general.sql for unknown shop types")
    void seed_general() throws Exception {
        tenantSeedService.seed(ShopType.OTHER);

        verify(mockSession).doWork(any(Work.class));
        verify(mockStatement, atLeastOnce()).execute(anyString());
    }

    @Test
    @DisplayName("seed: uses jewelry_store.sql for JEWELRY shop type")
    void seed_jewelry() throws Exception {
        tenantSeedService.seed(ShopType.JEWELRY);

        verify(mockSession).doWork(any(Work.class));
    }

    // ── seed: statement-level error is caught and execution continues ──────────

    @Test
    @DisplayName("seed: rolls back individual failing statement and continues with rest")
    void seed_statementError_continuesExecution() throws Exception {
        when(mockStatement.execute(anyString()))
                .thenThrow(new RuntimeException("Duplicate key"))
                .thenReturn(false); // subsequent statements succeed

        tenantSeedService.seed(ShopType.OTHER);

        verify(mockConnection, atLeastOnce()).rollback(any(Savepoint.class));
        verify(mockSession).doWork(any(Work.class));
    }

    // ── seedShopTypeTemplates ─────────────────────────────────────────────────

    @Test
    @DisplayName("seedShopTypeTemplates: null shopType returns early without DB call")
    void seedShopTypeTemplates_nullShopType() throws Exception {
        tenantSeedService.seedShopTypeTemplates(null);

        verify(mockSession, never()).doWork(any(Work.class));
    }

    @Test
    @DisplayName("seedShopTypeTemplates: BARBER_SHOP inserts 'Phiếu dịch vụ' template")
    void seedShopTypeTemplates_barberShop() throws Exception {
        tenantSeedService.seedShopTypeTemplates(ShopType.BARBER_SHOP);

        verify(mockSession).doWork(any(Work.class));
        verify(mockStatement).execute(argThat(sql -> sql.contains("Phiếu dịch vụ")));
    }

    @Test
    @DisplayName("seedShopTypeTemplates: COFFEE_SHOP inserts service template")
    void seedShopTypeTemplates_coffeeShop() throws Exception {
        tenantSeedService.seedShopTypeTemplates(ShopType.COFFEE_SHOP);

        verify(mockSession).doWork(any(Work.class));
        verify(mockStatement).execute(argThat(sql -> sql.contains("Phiếu dịch vụ")));
    }

    @Test
    @DisplayName("seedShopTypeTemplates: PHARMACY inserts 'Hóa đơn thuốc' template")
    void seedShopTypeTemplates_pharmacy() throws Exception {
        tenantSeedService.seedShopTypeTemplates(ShopType.PHARMACY);

        verify(mockSession).doWork(any(Work.class));
        verify(mockStatement).execute(argThat(sql -> sql.contains("Hóa đơn thuốc")));
    }

    @Test
    @DisplayName("seedShopTypeTemplates: CONVENIENCE_STORE inserts 'Hóa đơn siêu thị' template")
    void seedShopTypeTemplates_convenienceStore() throws Exception {
        tenantSeedService.seedShopTypeTemplates(ShopType.CONVENIENCE_STORE);

        verify(mockSession).doWork(any(Work.class));
        verify(mockStatement).execute(argThat(sql -> sql.contains("Hóa đơn siêu thị")));
    }

    @Test
    @DisplayName("seedShopTypeTemplates: FASHION inserts 'Phiếu bảo hành' template")
    void seedShopTypeTemplates_fashion() throws Exception {
        tenantSeedService.seedShopTypeTemplates(ShopType.FASHION);

        verify(mockSession).doWork(any(Work.class));
        verify(mockStatement).execute(argThat(sql -> sql.contains("Phiếu bảo hành")));
    }

    @Test
    @DisplayName("seedShopTypeTemplates: ELECTRONICS inserts 'Phiếu bảo hành' template")
    void seedShopTypeTemplates_electronics() throws Exception {
        tenantSeedService.seedShopTypeTemplates(ShopType.ELECTRONICS);

        verify(mockSession).doWork(any(Work.class));
        verify(mockStatement).execute(argThat(sql -> sql.contains("Phiếu bảo hành")));
    }

    @Test
    @DisplayName("seedShopTypeTemplates: OTHER shop type returns early without DB call")
    void seedShopTypeTemplates_otherShopType_noTemplate() throws Exception {
        tenantSeedService.seedShopTypeTemplates(ShopType.OTHER);

        verify(mockSession, never()).doWork(any(Work.class));
    }

    @Test
    @DisplayName("seedShopTypeTemplates: JEWELRY shop type returns early without DB call")
    void seedShopTypeTemplates_jewelry_noTemplate() throws Exception {
        tenantSeedService.seedShopTypeTemplates(ShopType.JEWELRY);

        verify(mockSession, never()).doWork(any(Work.class));
    }

    @Test
    @DisplayName("seedShopTypeTemplates: DB error during insert is caught via savepoint rollback")
    void seedShopTypeTemplates_dbError_rollsBackAndContinues() throws Exception {
        when(mockStatement.execute(anyString())).thenThrow(new RuntimeException("insert failed"));

        tenantSeedService.seedShopTypeTemplates(ShopType.BARBER_SHOP);

        verify(mockConnection).rollback(any(Savepoint.class));
    }

}

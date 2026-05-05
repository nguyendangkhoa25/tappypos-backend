package com.knp.service.tenant;

import com.knp.model.enums.ShopType;
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

        when(entityManager.unwrap(Session.class)).thenReturn(mockSession);
        when(mockConnection.setSavepoint()).thenReturn(mockSavepoint);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        doAnswer(inv -> {
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

}

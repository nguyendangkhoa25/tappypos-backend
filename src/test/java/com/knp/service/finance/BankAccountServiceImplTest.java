package com.knp.service.finance;

import com.knp.model.dto.bank.BankAccountDTO;
import com.knp.model.dto.bank.SaveBankAccountRequest;
import com.knp.model.entity.finance.BankAccount;
import com.knp.repository.finance.BankAccountRepository;
import com.knp.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankAccountServiceImpl Unit Tests")
class BankAccountServiceImplTest {

    @Mock private BankAccountRepository repo;
    @Mock private MessageService messageService;

    @InjectMocks
    private BankAccountServiceImpl service;

    private BankAccount account;
    private SaveBankAccountRequest saveRequest;

    @BeforeEach
    void setUp() {
        account = BankAccount.builder()
                .bankBin("970436")
                .bankCode("VCB")
                .bankName("Vietcombank")
                .accountNumber("1234567890")
                .accountName("NGUYEN VAN A")
                .isDefault(false)
                .build();

        saveRequest = new SaveBankAccountRequest();
        saveRequest.setBankBin("970436");
        saveRequest.setBankCode("VCB");
        saveRequest.setBankName("Vietcombank");
        saveRequest.setAccountNumber("1234567890");
        saveRequest.setAccountName("NGUYEN VAN A");

        lenient().when(messageService.getMessage(anyString(), any(Object[].class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll: returns all active accounts")
    void getAll() {
        when(repo.findAllActive()).thenReturn(List.of(account));

        List<BankAccountDTO> result = service.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBankCode()).isEqualTo("VCB");
    }

    // ── getDefault ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDefault: returns default account when present")
    void getDefault_found() {
        account.setIsDefault(true);
        when(repo.findDefault()).thenReturn(Optional.of(account));

        BankAccountDTO dto = service.getDefault();

        assertThat(dto).isNotNull();
        assertThat(dto.getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("getDefault: returns null when no default set")
    void getDefault_notFound() {
        when(repo.findDefault()).thenReturn(Optional.empty());

        assertThat(service.getDefault()).isNull();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: clears other defaults when creating as default")
    void create_asDefault() {
        saveRequest.setIsDefault(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(saveRequest);

        verify(repo).clearOtherDefaults(-1L);
        ArgumentCaptor<BankAccount> cap = ArgumentCaptor.forClass(BankAccount.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("create: does not clear defaults when not set as default")
    void create_notDefault() {
        saveRequest.setIsDefault(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(saveRequest);

        verify(repo, never()).clearOtherDefaults(anyLong());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: updates fields and sets as default when requested")
    void update_setAsDefault() {
        when(repo.findById(1L)).thenReturn(Optional.of(account));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        saveRequest.setIsDefault(true);

        service.update(1L, saveRequest);

        verify(repo).clearOtherDefaults(1L);
        assertThat(account.getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("update: sets isDefault to false when not requested")
    void update_notDefault() {
        account.setIsDefault(true);
        when(repo.findById(1L)).thenReturn(Optional.of(account));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        saveRequest.setIsDefault(false);

        service.update(1L, saveRequest);

        assertThat(account.getIsDefault()).isFalse();
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: soft-deletes the account")
    void delete_success() {
        when(repo.findById(1L)).thenReturn(Optional.of(account));
        when(repo.save(any())).thenReturn(account);

        service.delete(1L);

        assertThat(account.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("delete: throws when account not found")
    void delete_notFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── setDefault ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setDefault: clears others and marks account as default")
    void setDefault_success() {
        when(repo.findById(1L)).thenReturn(Optional.of(account));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.setDefault(1L);

        verify(repo).clearOtherDefaults(1L);
        assertThat(account.getIsDefault()).isTrue();
    }
}

package com.tappy.pos.service.customer;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.customer.CreateCustomerRequest;
import com.tappy.pos.model.dto.customer.CustomerDTO;
import com.tappy.pos.model.dto.customer.UpdateCustomerRequest;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.repository.customer.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private CreateCustomerRequest createRequest;
    private UpdateCustomerRequest updateRequest;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        customer = Customer.builder()
                .name("John Doe")
                .phone("1234567890")
                .email("john@example.com")
                .notes("Regular customer")
                .zaloId("zalo123")
                .facebookId("fb123")
                .preferredServices("Haircut, Styling")
                .allergiesOrSensitivities("None")
                .hairType("Curly")
                .specialRequests("Extra long on top")
                .build();
        customer.setId(1L);
        customer.setDeleted(false);
        customer.setCreatedAt(LocalDateTime.now());

        createRequest = CreateCustomerRequest.builder()
                .name("John Doe")
                .phone("1234567890")
                .email("john@example.com")
                .notes("Regular customer")
                .zaloId("zalo123")
                .facebookId("fb123")
                .preferredServices("Haircut, Styling")
                .allergiesOrSensitivities("None")
                .hairType("Curly")
                .specialRequests("Extra long on top")
                .build();

        updateRequest = UpdateCustomerRequest.builder()
                .name("Jane Doe")
                .phone("0987654321")
                .email("jane@example.com")
                .notes("VIP customer")
                .hairType("Straight")
                .build();
    }

    @Test
    @DisplayName("Should create customer successfully")
    void testCreateCustomer_Success() {
        // Given
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.createCustomer(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should get all customers with pagination")
    void testGetAllCustomers_Success() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 1);
        when(customerRepository.findAllActive(any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers(null, "id", "DESC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("John Doe");
        verify(customerRepository).findAllActive(any(Pageable.class));
    }

    @Test
    @DisplayName("Should search customers by keyword")
    void testGetAllCustomers_WithSearch_Success() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 1);
        when(customerRepository.searchByKeyword(anyString(), any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers("John", "name", "ASC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(customerRepository).searchByKeyword(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get customer by ID successfully")
    void testGetCustomerById_Success() {
        // Given
        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));

        // When
        CustomerDTO result = customerService.getCustomerById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        verify(customerRepository).findByIdActive(1L);
    }

    @Test
    @DisplayName("Should throw exception when customer not found by ID")
    void testGetCustomerById_NotFound() {
        // Given
        when(customerRepository.findByIdActive(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.customer.not.found", 999L)).thenReturn("Customer not found");

        // When & Then
        assertThatThrownBy(() -> customerService.getCustomerById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update customer successfully")
    void testUpdateCustomer_Success() {
        // Given
        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.updateCustomer(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(customerRepository).findByIdActive(1L);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should delete customer successfully")
    void testDeleteCustomer_Success() {
        // Given
        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        customerService.deleteCustomer(1L);

        // Then
        verify(customerRepository).findByIdActive(1L);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should get customer count")
    void testGetCustomerCount_Success() {
        // Given
        when(customerRepository.countAllActive()).thenReturn(100L);

        // When
        Long result = customerService.getCustomerCount();

        // Then
        assertThat(result).isEqualTo(100L);
        verify(customerRepository).countAllActive();
    }

    @Test
    @DisplayName("Should get customer by phone number")
    void testGetCustomerByPhone_Success() {
        // Given
        when(customerRepository.findByPhone("1234567890")).thenReturn(Optional.of(customer));

        // When
        CustomerDTO result = customerService.getCustomerByPhone("1234567890");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPhone()).isEqualTo("1234567890");
        verify(customerRepository).findByPhone("1234567890");
    }

    @Test
    @DisplayName("Should get customer by email")
    void testGetCustomerByEmail_Success() {
        // Given
        when(customerRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));

        // When
        CustomerDTO result = customerService.getCustomerByEmail("john@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        verify(customerRepository).findByEmail("john@example.com");
    }

    // ==================== Additional Edge Case Tests ====================

    @Test
    @DisplayName("Should throw exception when deleting non-existent customer")
    void testDeleteCustomer_NotFound() {
        // Given
        when(customerRepository.findByIdActive(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.customer.not.found", 999L)).thenReturn("Customer not found");

        // When & Then
        assertThatThrownBy(() -> customerService.deleteCustomer(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent customer")
    void testUpdateCustomer_NotFound() {
        // Given
        when(customerRepository.findByIdActive(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("error.customer.not.found", 999L)).thenReturn("Customer not found");

        // When & Then
        assertThatThrownBy(() -> customerService.updateCustomer(999L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent customer by phone")
    void testGetCustomerByPhone_NotFound() {
        // Given
        when(customerRepository.findByPhone("9999999999")).thenReturn(Optional.empty());
        when(messageService.getMessage("error.customer.not.found", "9999999999")).thenReturn("Customer not found");

        // When & Then
        assertThatThrownBy(() -> customerService.getCustomerByPhone("9999999999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent customer by email")
    void testGetCustomerByEmail_NotFound() {
        // Given
        when(customerRepository.findByEmail("invalid@example.com")).thenReturn(Optional.empty());
        when(messageService.getMessage("error.customer.not.found", "invalid@example.com")).thenReturn("Customer not found");

        // When & Then
        assertThatThrownBy(() -> customerService.getCustomerByEmail("invalid@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should handle null values in customer creation request")
    void testCreateCustomer_WithNullOptionalFields() {
        // Given
        CreateCustomerRequest requestWithNulls = CreateCustomerRequest.builder()
                .name("John Doe")
                .phone("1234567890")
                .email(null)
                .notes(null)
                .zaloId(null)
                .facebookId(null)
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.createCustomer(requestWithNulls);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should handle empty search results in getAllCustomers")
    void testGetAllCustomers_EmptyResults() {
        // Given
        Page<Customer> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(customerRepository.findAllActive(any(Pageable.class))).thenReturn(emptyPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers(null, "id", "DESC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Should handle multiple customers in pagination")
    void testGetAllCustomers_MultiplePagesWithSorting() {
        // Given
        Customer customer2 = Customer.builder()
                .name("Jane Doe")
                .phone("0987654321")
                .email("jane@example.com")
                .build();
        customer2.setId(2L);
        customer2.setDeleted(false);

        Page<Customer> customerPage = new PageImpl<>(
                java.util.List.of(customer, customer2),
                PageRequest.of(0, 10),
                2
        );
        when(customerRepository.findAllActive(any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers(null, "name", "ASC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should search customers with special characters in keyword")
    void testGetAllCustomers_SearchWithSpecialCharacters() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 1);
        when(customerRepository.searchByKeyword(anyString(), any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers("John@#$%", "name", "ASC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(customerRepository).searchByKeyword(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle partial updates on customer")
    void testUpdateCustomer_PartialFieldsOnly() {
        // Given
        UpdateCustomerRequest partialRequest = UpdateCustomerRequest.builder()
                .name("Jane Doe")
                .build();

        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.updateCustomer(1L, partialRequest);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should handle customer with special notes and requirements")
    void testCreateCustomer_WithComplexData() {
        // Given
        CreateCustomerRequest complexRequest = CreateCustomerRequest.builder()
                .name("John Doe")
                .phone("1234567890")
                .email("john@example.com")
                .notes("Very long customer notes with special characters: @#$%^&*()")
                .specialRequests("Multiple requests with line breaks and special formatting")
                .allergiesOrSensitivities("Allergic to: Chemical X, Chemical Y, Chemical Z")
                .hairType("Curly")
                .preferredServices("Service1, Service2, Service3")
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.createCustomer(complexRequest);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should get customer count with zero customers")
    void testGetCustomerCount_Empty() {
        // Given
        when(customerRepository.countAllActive()).thenReturn(0L);

        // When
        Long result = customerService.getCustomerCount();

        // Then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("Should get customer count with multiple customers")
    void testGetCustomerCount_Multiple() {
        // Given
        when(customerRepository.countAllActive()).thenReturn(5000L);

        // When
        Long result = customerService.getCustomerCount();

        // Then
        assertThat(result).isEqualTo(5000L);
        verify(customerRepository).countAllActive();
    }

    @Test
    @DisplayName("Should search customers with empty keyword")
    void testGetAllCustomers_SearchWithEmptyKeyword() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 1);
        when(customerRepository.findAllActive(any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers("", "id", "DESC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should search customers with whitespace-only keyword")
    void testGetAllCustomers_SearchWithWhitespace() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 1);
        when(customerRepository.findAllActive(any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers("   ", "id", "DESC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle customer with all fields populated")
    void testCreateCustomer_AllFieldsPopulated() {
        // Given
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.createCustomer(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getPhone()).isEqualTo("1234567890");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getZaloId()).isEqualTo("zalo123");
        assertThat(result.getFacebookId()).isEqualTo("fb123");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should update customer with all fields")
    void testUpdateCustomer_AllFieldsPopulated() {
        // Given
        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.updateCustomer(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(customerRepository).findByIdActive(1L);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should handle ascending sort order")
    void testGetAllCustomers_AscendingSort() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 1);
        when(customerRepository.findAllActive(any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers(null, "name", "ASC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle different sort fields")
    void testGetAllCustomers_SortByDifferentFields() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 1);
        when(customerRepository.findAllActive(any(Pageable.class))).thenReturn(customerPage);

        // When - Test sorting by different fields
        Page<CustomerDTO> result1 = customerService.getAllCustomers(null, "createdAt", "DESC", PageRequest.of(0, 10));
        Page<CustomerDTO> result2 = customerService.getAllCustomers(null, "name", "ASC", PageRequest.of(0, 10));
        Page<CustomerDTO> result3 = customerService.getAllCustomers(null, "phone", "DESC", PageRequest.of(0, 10));

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result3).isNotNull();
    }

    @Test
    @DisplayName("Should map customer entity to DTO correctly")
    void testCustomerEntityToDTO_FieldMapping() {
        // Given
        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));

        // When
        CustomerDTO result = customerService.getCustomerById(1L);

        // Then
        assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", 1L)
                .hasFieldOrPropertyWithValue("name", "John Doe")
                .hasFieldOrPropertyWithValue("phone", "1234567890")
                .hasFieldOrPropertyWithValue("email", "john@example.com")
                .hasFieldOrPropertyWithValue("notes", "Regular customer")
                .hasFieldOrPropertyWithValue("zaloId", "zalo123")
                .hasFieldOrPropertyWithValue("facebookId", "fb123")
                .hasFieldOrPropertyWithValue("preferredServices", "Haircut, Styling")
                .hasFieldOrPropertyWithValue("allergiesOrSensitivities", "None")
                .hasFieldOrPropertyWithValue("hairType", "Curly")
                .hasFieldOrPropertyWithValue("specialRequests", "Extra long on top");
    }

    @Test
    @DisplayName("Should handle pagination with different page numbers")
    void testGetAllCustomers_DifferentPages() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 20);
        when(customerRepository.findAllActive(any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result1 = customerService.getAllCustomers(null, "id", "DESC", PageRequest.of(0, 10));
        Page<CustomerDTO> result2 = customerService.getAllCustomers(null, "id", "DESC", PageRequest.of(1, 10));

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1.getContent()).hasSize(1);
    }
    @Test
    @DisplayName("Should handle sorting by different fields correctly")
    void testGetAllCustomers_SortByCreatedAtDescending() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 1);
        when(customerRepository.findAllActive(any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers(null, "createdAt", "DESC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should search with null search keyword")
    void testGetAllCustomers_NullSearchKeyword() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 1);
        when(customerRepository.findAllActive(any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers(null, "id", "DESC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(customerRepository).findAllActive(any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle customer with minimal information")
    void testCreateCustomer_MinimalInformation() {
        // Given
        CreateCustomerRequest minimalRequest = CreateCustomerRequest.builder()
                .name("Jane")
                .phone("123456")
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.createCustomer(minimalRequest);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should handle update customer with minimal changes")
    void testUpdateCustomer_MinimalChanges() {
        // Given
        UpdateCustomerRequest minimalUpdate = UpdateCustomerRequest.builder()
                .phone("0000000000")
                .build();

        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.updateCustomer(1L, minimalUpdate);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should handle very long customer name")
    void testCreateCustomer_VeryLongName() {
        // Given
        CreateCustomerRequest longNameRequest = CreateCustomerRequest.builder()
                .name("John Doe " + "X".repeat(500))
                .phone("1234567890")
                .email("john@example.com")
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.createCustomer(longNameRequest);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should retrieve customer count successfully")
    void testGetCustomerCount_Successful() {
        // Given
        when(customerRepository.countAllActive()).thenReturn(42L);

        // When
        Long result = customerService.getCustomerCount();

        // Then
        assertThat(result).isEqualTo(42L);
        verify(customerRepository, times(1)).countAllActive();
    }

    @Test
    @DisplayName("Should handle concurrent field updates")
    void testUpdateCustomer_AllFieldsAtOnce() {
        // Given
        UpdateCustomerRequest fullUpdate = UpdateCustomerRequest.builder()
                .name("Updated Name")
                .phone("9999999999")
                .email("updated@example.com")
                .notes("Updated notes")
                .zaloId("new_zalo")
                .facebookId("new_fb")
                .preferredServices("New Services")
                .allergiesOrSensitivities("New Allergies")
                .hairType("Straight")
                .specialRequests("New Requests")
                .build();

        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.updateCustomer(1L, fullUpdate);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should handle search with numeric keyword")
    void testGetAllCustomers_SearchWithPhoneNumber() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 1);
        when(customerRepository.searchByKeyword(anyString(), any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers("1234567890", "phone", "ASC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle search with email keyword")
    void testGetAllCustomers_SearchWithEmail() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 1);
        when(customerRepository.searchByKeyword(anyString(), any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers("john@example.com", "email", "ASC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle get customer by phone with valid result")
    void testGetCustomerByPhone_ValidResult() {
        // Given
        when(customerRepository.findByPhone("1234567890")).thenReturn(Optional.of(customer));

        // When
        CustomerDTO result = customerService.getCustomerByPhone("1234567890");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPhone()).isEqualTo("1234567890");
        assertThat(result.getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should handle get customer by email with valid result")
    void testGetCustomerByEmail_ValidResult() {
        // Given
        when(customerRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));

        // When
        CustomerDTO result = customerService.getCustomerByEmail("john@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should handle descending sort order")
    void testGetAllCustomers_DescendingSort() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 10), 1);
        when(customerRepository.findAllActive(any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers(null, "name", "DESC", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle large page size pagination")
    void testGetAllCustomers_LargePageSize() {
        // Given
        Page<Customer> customerPage = new PageImpl<>(Collections.singletonList(customer), PageRequest.of(0, 1000), 1);
        when(customerRepository.findAllActive(any(Pageable.class))).thenReturn(customerPage);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers(null, "id", "DESC", PageRequest.of(0, 1000));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSize()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Should verify DTO mapping with all fields")
    void testCustomerToDTO_CompleteFieldMapping() {
        // Given
        Customer completeCustomer = Customer.builder()
                .name("Complete Customer")
                .phone("9876543210")
                .email("complete@example.com")
                .notes("Complete notes")
                .zaloId("complete_zalo")
                .facebookId("complete_fb")
                .preferredServices("All Services")
                .allergiesOrSensitivities("All Allergies")
                .hairType("Wavy")
                .specialRequests("All Requests")
                .build();
        completeCustomer.setId(99L);
        completeCustomer.setDeleted(false);
        completeCustomer.setCreatedAt(LocalDateTime.now());

        when(customerRepository.findByIdActive(99L)).thenReturn(Optional.of(completeCustomer));

        // When
        CustomerDTO result = customerService.getCustomerById(99L);

        // Then
        assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", 99L)
                .hasFieldOrPropertyWithValue("name", "Complete Customer")
                .hasFieldOrPropertyWithValue("phone", "9876543210")
                .hasFieldOrPropertyWithValue("email", "complete@example.com")
                .hasFieldOrPropertyWithValue("zaloId", "complete_zalo")
                .hasFieldOrPropertyWithValue("facebookId", "complete_fb");
    }

    @Test
    @DisplayName("Should handle delete with repository interactions")
    void testDeleteCustomer_VerifyRepositoryInteractions() {
        // Given
        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        customerService.deleteCustomer(1L);

        // Then
        verify(customerRepository).findByIdActive(1L);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should handle create with repository save")
    void testCreateCustomer_VerifyRepositorySave() {
        // Given
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.createCustomer(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should handle update with repository find and save")
    void testUpdateCustomer_VerifyRepositoryInteractions() {
        // Given
        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.updateCustomer(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).findByIdActive(1L);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should handle customer with special characters in notes")
    void testCreateCustomer_SpecialCharactersInNotes() {
        // Given
        CreateCustomerRequest specialCharsRequest = CreateCustomerRequest.builder()
                .name("John Doe")
                .phone("1234567890")
                .email("john@example.com")
                .notes("Notes with special chars: @#$%^&*()_+-=[]{}|;:',.<>?/\\\"")
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.createCustomer(specialCharsRequest);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should handle customer with unicode characters")
    void testCreateCustomer_UnicodeCharacters() {
        // Given
        CreateCustomerRequest unicodeRequest = CreateCustomerRequest.builder()
                .name("顧客 Khách hàng 고객")
                .phone("1234567890")
                .email("unicode@example.com")
                .notes("Ghi chú 注記 メモ")
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.createCustomer(unicodeRequest);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    // ============= getOrCreateCustomer Tests =============

    @Test
    @DisplayName("Should return existing customer when phone number exists")
    void testGetOrCreateCustomer_ExistingCustomer() {
        // Given
        when(customerRepository.findByPhone("1234567890")).thenReturn(Optional.of(customer));

        // When
        CustomerDTO result = customerService.getOrCreateCustomer(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getPhone()).isEqualTo("1234567890");
        verify(customerRepository).findByPhone("1234567890");
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should create new customer when phone does not exist")
    void testGetOrCreateCustomer_NewCustomer() {
        // Given
        when(customerRepository.findByPhone("1234567890")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.getOrCreateCustomer(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        verify(customerRepository).findByPhone("1234567890");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should create new customer when phone is null")
    void testGetOrCreateCustomer_PhoneNull() {
        // Given
        CreateCustomerRequest requestWithoutPhone = CreateCustomerRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .phone(null)
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.getOrCreateCustomer(requestWithoutPhone);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository, never()).findByPhone(anyString());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should create new customer when phone is empty string")
    void testGetOrCreateCustomer_PhoneEmpty() {
        // Given
        CreateCustomerRequest requestWithEmptyPhone = CreateCustomerRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .phone("")
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.getOrCreateCustomer(requestWithEmptyPhone);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository, never()).findByPhone("");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should return existing customer with all fields populated")
    void testGetOrCreateCustomer_ExistingCustomerWithAllFields() {
        // Given
        Customer existingCustomer = Customer.builder()
                .name("Jane Smith")
                .phone("9876543210")
                .email("jane.smith@example.com")
                .notes("VIP customer")
                .zaloId("zalo456")
                .facebookId("fb456")
                .preferredServices("Styling, Color")
                .allergiesOrSensitivities("Allergic to parabens")
                .hairType("Straight")
                .specialRequests("Avoid healing")
                .build();
        existingCustomer.setId(2L);
        existingCustomer.setDeleted(false);
        existingCustomer.setCreatedAt(LocalDateTime.now());

        CreateCustomerRequest requestForExisting = CreateCustomerRequest.builder()
                .phone("9876543210")
                .name("Jane Smith")
                .email("jane.smith@example.com")
                .build();

        when(customerRepository.findByPhone("9876543210")).thenReturn(Optional.of(existingCustomer));

        // When
        CustomerDTO result = customerService.getOrCreateCustomer(requestForExisting);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getName()).isEqualTo("Jane Smith");
        assertThat(result.getPhone()).isEqualTo("9876543210");
        assertThat(result.getZaloId()).isEqualTo("zalo456");
        assertThat(result.getFacebookId()).isEqualTo("fb456");
        verify(customerRepository).findByPhone("9876543210");
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should create customer with minimal fields when phone not found")
    void testGetOrCreateCustomer_MinimalFieldsNewCustomer() {
        // Given
        CreateCustomerRequest minimalRequest = CreateCustomerRequest.builder()
                .phone("5555555555")
                .build();

        Customer minimalCustomer = Customer.builder()
                .phone("5555555555")
                .build();
        minimalCustomer.setId(3L);
        minimalCustomer.setDeleted(false);
        minimalCustomer.setCreatedAt(LocalDateTime.now());

        when(customerRepository.findByPhone("5555555555")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(minimalCustomer);

        // When
        CustomerDTO result = customerService.getOrCreateCustomer(minimalRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getPhone()).isEqualTo("5555555555");
        verify(customerRepository).findByPhone("5555555555");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should handle special characters in phone number")
    void testGetOrCreateCustomer_PhoneWithSpecialChars() {
        // Given
        CreateCustomerRequest requestWithSpecialPhone = CreateCustomerRequest.builder()
                .phone("+1 (234) 567-8900")
                .name("Bob Johnson")
                .build();

        when(customerRepository.findByPhone("+1 (234) 567-8900")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.getOrCreateCustomer(requestWithSpecialPhone);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).findByPhone("+1 (234) 567-8900");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should return existing customer with identical phone multiple times")
    void testGetOrCreateCustomer_SamePhoneMultipleCalls() {
        // Given
        when(customerRepository.findByPhone("1234567890")).thenReturn(Optional.of(customer));

        // When
        CustomerDTO result1 = customerService.getOrCreateCustomer(createRequest);
        CustomerDTO result2 = customerService.getOrCreateCustomer(createRequest);

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1.getId()).isEqualTo(result2.getId());
        verify(customerRepository, times(2)).findByPhone("1234567890");
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should map existing customer correctly to DTO")
    void testGetOrCreateCustomer_DTOMappingExistingCustomer() {
        // Given
        Customer customerWithDates = Customer.builder()
                .name("Test Customer")
                .phone("1111111111")
                .email("test@example.com")
                .notes("Test notes")
                .build();
        customerWithDates.setId(1L);
        customerWithDates.setDeleted(false);
        customerWithDates.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0, 0));
        customerWithDates.setUpdatedAt(LocalDateTime.of(2025, 1, 15, 15, 30, 0));

        CreateCustomerRequest requestForDTOTest = CreateCustomerRequest.builder()
                .phone("1111111111")
                .name("Test Customer")
                .build();

        when(customerRepository.findByPhone("1111111111")).thenReturn(Optional.of(customerWithDates));

        // When
        CustomerDTO result = customerService.getOrCreateCustomer(requestForDTOTest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Customer");
        assertThat(result.getPhone()).isEqualTo("1111111111");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getNotes()).isEqualTo("Test notes");
    }

    @Test
    @DisplayName("Should correctly handle getOrCreateCustomer with international phone format")
    void testGetOrCreateCustomer_UnicodePhone() {
        // Given
        String intlPhone = "+84-98-765-4321";
        CreateCustomerRequest intlPhoneRequest = CreateCustomerRequest.builder()
                .phone(intlPhone)
                .name("International Customer")
                .build();

        when(customerRepository.findByPhone(intlPhone)).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // When
        CustomerDTO result = customerService.getOrCreateCustomer(intlPhoneRequest);

        // Then
        assertThat(result).isNotNull();
        verify(customerRepository).findByPhone(intlPhone);
        verify(customerRepository).save(any(Customer.class));
    }

    // ── updateCustomer extended fields ────────────────────────────────────────

    @Test
    @DisplayName("updateCustomer: sets all extended fields when provided")
    void testUpdateCustomer_ExtendedFields() {
        UpdateCustomerRequest req = UpdateCustomerRequest.builder()
                .zaloId("zalo-new")
                .facebookId("fb-new")
                .preferredServices("Massage, Spa")
                .allergiesOrSensitivities("Latex")
                .specialRequests("No loud music")
                .idCardNumber("123456789")
                .gender("MALE")
                .permanentAddress("123 ABC Street")
                .idCardIssuedPlace("Ha Noi")
                .build();

        when(customerRepository.findByIdActive(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        customerService.updateCustomer(1L, req);

        verify(customerRepository).save(argThat(c ->
                "zalo-new".equals(c.getZaloId()) &&
                "fb-new".equals(c.getFacebookId()) &&
                "Massage, Spa".equals(c.getPreferredServices()) &&
                "123456789".equals(c.getIdCardNumber())));
    }

    // ── deleteCustomer: walk-in customer ─────────────────────────────────────

    @Test
    @DisplayName("deleteCustomer: throws when trying to delete walk-in customer")
    void testDeleteCustomer_WalkIn_Throws() {
        Customer walkIn = Customer.builder().name("Khách lẻ").phone("0000000000").build();
        walkIn.setId(99L);
        walkIn.setDeleted(false);
        when(customerRepository.findByIdActive(99L)).thenReturn(Optional.of(walkIn));
        when(messageService.getMessage("error.customer.cannot.delete.walkin")).thenReturn("Cannot delete walk-in");

        assertThatThrownBy(() -> customerService.deleteCustomer(99L))
                .isInstanceOf(com.tappy.pos.exception.BadRequestException.class);
    }

    // ── getWalkinCustomer ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getWalkinCustomer: returns DTO for customer with phone 0000000000")
    void testGetWalkinCustomer_Success() {
        customer.setPhone("0000000000");
        when(customerRepository.findByPhone("0000000000")).thenReturn(Optional.of(customer));

        CustomerDTO dto = customerService.getWalkinCustomer();

        assertThat(dto).isNotNull();
        assertThat(dto.getPhone()).isEqualTo("0000000000");
    }

    // ── findCustomerByPhone ───────────────────────────────────────────────────

    @Test
    @DisplayName("findCustomerByPhone: returns DTO when customer exists")
    void testFindCustomerByPhone_Found() {
        when(customerRepository.findByPhone("1234567890")).thenReturn(Optional.of(customer));

        CustomerDTO dto = customerService.findCustomerByPhone("1234567890");

        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("findCustomerByPhone: returns null when no customer with that phone")
    void testFindCustomerByPhone_NotFound() {
        when(customerRepository.findByPhone("0000000001")).thenReturn(Optional.empty());

        CustomerDTO dto = customerService.findCustomerByPhone("0000000001");

        assertThat(dto).isNull();
    }

}

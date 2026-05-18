package com.tappy.pos.repository.customer;

import com.tappy.pos.model.entity.customer.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.enabled=false")
@DisplayName("CustomerRepository Integration Tests")
class CustomerRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer persist(String name, String phone, boolean deleted) {
        Customer c = Customer.builder().name(name).phone(phone).build();
        c.setTenantId("tenant-01");
        c.setDeleted(deleted);
        return entityManager.persistAndFlush(c);
    }

    @Test
    @DisplayName("findAllActive: active customers are returned, deleted are excluded")
    void findAllActive_filtersOutDeleted() {
        long before = customerRepository.findAllActive(PageRequest.of(0, 1000)).getTotalElements();
        persist("Nguyen Van A", "0910000101", false);
        persist("Tran Thi B", "0910000102", false);
        persist("Deleted User", "0910000103", true);

        Page<Customer> result = customerRepository.findAllActive(PageRequest.of(0, 1000));

        assertThat(result.getTotalElements()).isEqualTo(before + 2);
        assertThat(result.getContent()).extracting(Customer::getPhone)
                .contains("0910000101", "0910000102")
                .doesNotContain("0910000103");
    }

    @Test
    @DisplayName("findByPhone: returns customer with matching phone")
    void findByPhone_matchingPhone_returnsCustomer() {
        persist("Le Van C", "0920000101", false);

        Optional<Customer> result = customerRepository.findByPhone("0920000101");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Le Van C");
    }

    @Test
    @DisplayName("findByPhone: returns empty for deleted customer")
    void findByPhone_deletedCustomer_returnsEmpty() {
        persist("Deleted Person", "0920000102", true);

        Optional<Customer> result = customerRepository.findByPhone("0920000102");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchByKeyword: matches partial name case-insensitively")
    void searchByKeyword_partialNameMatch_returnsMatchingCustomers() {
        persist("Pham Thi Lan", "0930000101", false);
        persist("Hoang Van Nam", "0930000102", false);
        persist("Nguyen Lan Anh", "0930000103", false);

        Page<Customer> result = customerRepository.searchByKeyword("lan", PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Customer::getName)
                .containsExactlyInAnyOrder("Pham Thi Lan", "Nguyen Lan Anh");
    }

    @Test
    @DisplayName("searchByKeyword: matches partial phone number")
    void searchByKeyword_phoneMatch_returnsMatchingCustomers() {
        persist("Customer X", "0940000101", false);
        persist("Customer Y", "0940000102", false);
        persist("Customer Z", "0850000101", false);

        Page<Customer> result = customerRepository.searchByKeyword("094000010", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Customer::getPhone)
                .allSatisfy(phone -> assertThat(phone).startsWith("094"));
    }

    @Test
    @DisplayName("searchByKeyword: excludes soft-deleted customers")
    void searchByKeyword_deletedCustomer_notReturned() {
        persist("Deleted Matchname", "0950000101", true);

        Page<Customer> result = customerRepository.searchByKeyword("matchname", PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("countAllActive: counts only non-deleted customers (delta-based)")
    void countAllActive_countsOnlyNonDeleted() {
        long before = customerRepository.countAllActive();
        persist("Active 1", "0960000101", false);
        persist("Active 2", "0960000102", false);
        persist("Deleted", "0960000103", true);

        long count = customerRepository.countAllActive();

        assertThat(count).isEqualTo(before + 2);
    }

    @Test
    @DisplayName("findByIdActive: returns present for active, empty for deleted")
    void findByIdActive_activeVsDeleted() {
        Customer active = persist("Active Customer", "0970000101", false);
        Customer deleted = persist("Deleted Customer", "0970000102", true);

        assertThat(customerRepository.findByIdActive(active.getId())).isPresent();
        assertThat(customerRepository.findByIdActive(deleted.getId())).isEmpty();
    }

    @Test
    @DisplayName("findByPhoneAndTenantId: returns customer matching phone AND tenant")
    void findByPhoneAndTenantId_matchingTenant_returnsCustomer() {
        Customer c = Customer.builder().name("Shop A Customer").phone("0980000101").build();
        c.setTenantId("tenant-A");
        entityManager.persistAndFlush(c);

        Optional<Customer> result = customerRepository.findByPhoneAndTenantId("0980000101", "tenant-A");
        Optional<Customer> noMatch = customerRepository.findByPhoneAndTenantId("0980000101", "tenant-B");

        assertThat(result).isPresent().map(Customer::getName).hasValue("Shop A Customer");
        assertThat(noMatch).isEmpty();
    }

    @Test
    @DisplayName("findByEmail: returns customer matching email")
    void findByEmail_match_returnsCustomer() {
        Customer c = Customer.builder().name("Email Customer").phone("0990000101")
                .email("test@example.com").build();
        c.setTenantId("tenant-01");
        entityManager.persistAndFlush(c);

        Optional<Customer> result = customerRepository.findByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("searchByKeyword: returns empty page when no match")
    void searchByKeyword_noMatch_returnsEmptyPage() {
        persist("Known Customer", "0901000101", false);

        Page<Customer> result = customerRepository.searchByKeyword("zzznomatch999", PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }
}

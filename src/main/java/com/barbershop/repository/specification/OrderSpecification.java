package com.barbershop.repository.specification;

import com.barbershop.model.dto.order.OrderFilterRequest;
import com.barbershop.model.entity.Order;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> withFilters(OrderFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter out deleted orders
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

            if (filter == null) {
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }

            // Filter by keyword (customer name or phone)
            if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
                String keyword = "%" + filter.getKeyword().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("customer").get("name")),
                        keyword
                );
                Predicate phonePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("customer").get("phone")),
                        keyword
                );
                predicates.add(criteriaBuilder.or(namePredicate, phonePredicate));
            }

            // Filter by status
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("status"),
                        Order.OrderStatus.valueOf(filter.getStatus())
                ));
            }

            // Filter by customer
            if (filter.getCustomerId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("customer").get("id"),
                        filter.getCustomerId()
                ));
            }

            // Filter by minimum amount
            if (filter.getMinAmount() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("totalAmount"),
                        filter.getMinAmount()
                ));
            }

            // Filter by maximum amount
            if (filter.getMaxAmount() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("totalAmount"),
                        filter.getMaxAmount()
                ));
            }

            // Filter by start date
            if (filter.getFromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        filter.getFromDate()
                ));
            }

            // Filter by end date
            if (filter.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"),
                        filter.getToDate()
                ));
            }

            // Filter by orders without invoice
            if (filter.getWithoutInvoice() != null && filter.getWithoutInvoice()) {
                predicates.add(criteriaBuilder.isNull(root.get("invoiceId")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}


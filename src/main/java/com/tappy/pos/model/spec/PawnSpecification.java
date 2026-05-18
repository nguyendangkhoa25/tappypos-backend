package com.tappy.pos.model.spec;

import com.tappy.pos.model.dto.pawn.DateFilterRequest;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.pawn.*;
import com.tappy.pos.model.enums.PawnStatus;
import jakarta.persistence.criteria.*;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@NoArgsConstructor
public class PawnSpecification {
    private static final String ANY_STRING = "%";

    public static Specification<PawnQuery> searchPawnId(Long pawnId) {
        Specification<PawnQuery> pawnIdSpec = Specification.where(null);
        pawnIdSpec = pawnIdSpec.and(
                ((root, criteriaQuery, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("pawnId"), pawnId)));
        return pawnIdSpec;
    }

    public static Specification<PawnQuery> includeVisibleStatus() {
        Specification<PawnQuery> spec = Specification.where(null);
        spec = spec.or(
                        (root, criteriaQuery, criteriaBuilder) ->
                                criteriaBuilder.isNull(root.get("visible")))
                .or(
                        ((root, criteriaQuery, criteriaBuilder) ->
                                criteriaBuilder.isTrue(root.get("visible"))));
        return spec;
    }

    public static Specification<PawnQuery> searchStatus(List<PawnStatus> pawnStatuses) {
        Specification<PawnQuery> pawnIdSpec = Specification.where(null);
        pawnIdSpec = pawnIdSpec.and(
                ((root, criteriaQuery, criteriaBuilder) ->
                        criteriaBuilder.and(root.get("pawnStatus").in(pawnStatuses))));
        return pawnIdSpec;
    }

    public static Specification<PawnQuery> searchAmount(Long pawnAmount) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("pawnAmount"), pawnAmount);
    }

    public static Specification<PawnQuery> searchByItemName(String itemName) {
        Specification<PawnQuery> pawnIdSpec = Specification.where(null);
        pawnIdSpec = pawnIdSpec.and(
                ((root, criteriaQuery, criteriaBuilder) ->
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("itemName")),
                                ANY_STRING + itemName.toLowerCase() + ANY_STRING)));
        return pawnIdSpec;
    }

    public static Specification<PawnQuery> searchCustomerId(Long customerId) {
        return (root, query, criteriaBuilder) -> {
            Join<PawnQuery, Customer> customer = root.join("customer");
            return criteriaBuilder.equal(customer.get("id"), customerId);
        };
    }

    public static Specification<PawnQuery> freeSearch(String searchWord) {
        Specification<PawnQuery> spec = Specification.where(null);
        spec = spec.or(
                        ((root, criteriaQuery, criteriaBuilder) ->
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("itemName")),
                                        ANY_STRING + searchWord.toLowerCase() + ANY_STRING)))
                .or(
                        ((root, criteriaQuery, criteriaBuilder) ->
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("pawnDate").as(String.class)),
                                        ANY_STRING + rotateDateString(searchWord, "/") + ANY_STRING)))
                .or(
                        ((root, criteriaQuery, criteriaBuilder) ->
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("pawnId").as(String.class)),
                                        ANY_STRING + searchWord.toLowerCase() + ANY_STRING)))
                .or(
                        ((root, criteriaQuery, criteriaBuilder) ->
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("pawnStatus").as(String.class)),
                                        ANY_STRING + searchWord.toLowerCase() + ANY_STRING)));
        return spec;
    }

    public static String rotateDateString(String dateString, String regex) {
        String[] parts = dateString.split(regex);
        if (parts.length == 3) {
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        } else if (parts.length == 2) {
            return parts[1] + "-" + parts[0];
        } else {
            return dateString;
        }
    }

    public static Specification<PawnQuery> searchCustomer(String searchWord) {
        return (root, query, criteriaBuilder) -> {
            Join<PawnQuery, Customer> customer = root.join("customer", JoinType.LEFT);
            String pattern = ANY_STRING + searchWord.toLowerCase() + ANY_STRING;
            Predicate byName = criteriaBuilder.like(criteriaBuilder.lower(customer.get("name")), pattern);
            Predicate byPhone = criteriaBuilder.like(criteriaBuilder.lower(customer.get("phone")), pattern);
            return criteriaBuilder.or(byName, byPhone);
        };
    }

    public static Specification<PawnQuery> searchPawnDueDate(DateFilterRequest pawnDueDate) {
        Specification<PawnQuery> spec = Specification.where(null);
        if (pawnDueDate.getFromDate() > 0) {
            LocalDateTime fromDate = Instant.ofEpochMilli(pawnDueDate.getFromDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.greaterThanOrEqualTo(root.get("pawnDueDate"), fromDate)));
        }
        if (pawnDueDate.getToDate() > 0) {
            LocalDateTime toDate = Instant.ofEpochMilli(pawnDueDate.getToDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.lessThanOrEqualTo(root.get("pawnDueDate"), toDate)));
        }
        if (pawnDueDate.getEqualDate() > 0) {
            LocalDateTime equalDate = Instant.ofEpochMilli(pawnDueDate.getEqualDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.equal(root.get("pawnDueDate"), equalDate)));
        }
        return spec;
    }

    public static Specification<PawnQuery> searchPawnDate(DateFilterRequest pawnDate) {
        Specification<PawnQuery> spec = Specification.where(null);
        if (pawnDate.getFromDate() > 0) {
            LocalDateTime fromDate = Instant.ofEpochMilli(pawnDate.getFromDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.greaterThanOrEqualTo(root.get("pawnDate"), fromDate)));
        }
        if (pawnDate.getToDate() > 0) {
            LocalDateTime toDate = Instant.ofEpochMilli(pawnDate.getToDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.lessThanOrEqualTo(root.get("pawnDate"), toDate)));
        }
        if (pawnDate.getEqualDate() > 0) {
            LocalDateTime equalDate = Instant.ofEpochMilli(pawnDate.getEqualDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.equal(root.get("pawnDate"), equalDate)));
        }
        return spec;
    }

    public static Specification<PawnQuery> searchRedeemDate(DateFilterRequest redeemDate) {
        Specification<PawnQuery> spec = Specification.where(null);
        if (redeemDate.getFromDate() > 0) {
            LocalDateTime fromDate = Instant.ofEpochMilli(redeemDate.getFromDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.greaterThanOrEqualTo(root.get("redeemDate"), fromDate)));
        }
        if (redeemDate.getToDate() > 0) {
            LocalDateTime toDate = Instant.ofEpochMilli(redeemDate.getToDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.lessThanOrEqualTo(root.get("redeemDate"), toDate)));
        }
        if (redeemDate.getEqualDate() > 0) {
            LocalDateTime equalDate = Instant.ofEpochMilli(redeemDate.getEqualDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.equal(root.get("redeemDate"), equalDate)));
        }
        return spec;
    }

    public static Specification<PawnQuery> searchForfeitedDate(DateFilterRequest forfeitedDate) {
        Specification<PawnQuery> spec = Specification.where(null);
        if (forfeitedDate.getFromDate() > 0) {
            LocalDateTime fromDate = Instant.ofEpochMilli(forfeitedDate.getFromDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.greaterThanOrEqualTo(root.get("forfeitedDate"), fromDate)));
        }
        if (forfeitedDate.getToDate() > 0) {
            LocalDateTime toDate = Instant.ofEpochMilli(forfeitedDate.getToDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.lessThanOrEqualTo(root.get("forfeitedDate"), toDate)));
        }
        if (forfeitedDate.getEqualDate() > 0) {
            LocalDateTime equalDate = Instant.ofEpochMilli(forfeitedDate.getEqualDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) ->
                            criteriaBuilder.equal(root.get("forfeitedDate"), equalDate)));
        }
        return spec;
    }

    public static Specification<PawnQuery> searchRequestMoneyRequestDate(DateFilterRequest requestDate) {
        Specification<PawnQuery> spec = Specification.where(null);
        if (requestDate.getFromDate() > 0) {
            LocalDateTime fromDate = Instant.ofEpochMilli(requestDate.getFromDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) -> {
                        Join<PawnQuery, ReqMoneyEntity> req = root.join("reqMoneys");
                        return criteriaBuilder.greaterThanOrEqualTo(req.get("requestDate"), fromDate);
                    }));
        }
        if (requestDate.getToDate() > 0) {
            LocalDateTime toDate = Instant.ofEpochMilli(requestDate.getToDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) -> {
                        Join<PawnQuery, ReqMoneyEntity> req = root.join("reqMoneys");
                        return criteriaBuilder.lessThanOrEqualTo(req.get("requestDate"), toDate);
                    }));
        }
        if (requestDate.getEqualDate() > 0) {
            LocalDateTime equalDate = Instant.ofEpochMilli(requestDate.getEqualDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            spec = spec.and(
                    ((root, criteriaQuery, criteriaBuilder) -> {
                        Join<PawnQuery, ReqMoneyEntity> req = root.join("reqMoneys");
                        return criteriaBuilder.equal(req.get("requestDate"), equalDate);
                    }));
        }
        return spec;
    }

    public static Specification<PawnQuery> filterByPawnCategory(String category) {
        return (root, query, cb) -> cb.equal(root.get("pawnCategory"), category);
    }

    public static Specification<PawnQuery> filterByElectronicsAttribute(String field, String value) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PawnElectronicsEntity> e = sub.from(PawnElectronicsEntity.class);
            sub.select(e.get("pawnId"))
               .where(cb.and(
                   cb.equal(e.get("pawnId"), root.get("pawnId")),
                   cb.like(cb.lower(e.get(field)), "%" + value.toLowerCase() + "%")
               ));
            return cb.exists(sub);
        };
    }

    public static Specification<PawnQuery> filterByVehicleAttribute(String field, String value) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PawnVehicleEntity> e = sub.from(PawnVehicleEntity.class);
            sub.select(e.get("pawnId"))
               .where(cb.and(
                   cb.equal(e.get("pawnId"), root.get("pawnId")),
                   cb.like(cb.lower(e.get(field)), "%" + value.toLowerCase() + "%")
               ));
            return cb.exists(sub);
        };
    }

    public static Specification<PawnQuery> filterByWatchAttribute(String field, String value) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PawnWatchEntity> e = sub.from(PawnWatchEntity.class);
            sub.select(e.get("pawnId"))
               .where(cb.and(
                   cb.equal(e.get("pawnId"), root.get("pawnId")),
                   cb.like(cb.lower(e.get(field)), "%" + value.toLowerCase() + "%")
               ));
            return cb.exists(sub);
        };
    }

    public static Specification<PawnQuery> filterByRealEstateAttribute(String field, String value) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PawnRealEstateEntity> e = sub.from(PawnRealEstateEntity.class);
            sub.select(e.get("pawnId"))
               .where(cb.and(
                   cb.equal(e.get("pawnId"), root.get("pawnId")),
                   cb.like(cb.lower(e.get(field)), "%" + value.toLowerCase() + "%")
               ));
            return cb.exists(sub);
        };
    }

    public static Specification<PawnQuery> filterByGeneralAttribute(String field, String value) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PawnGeneralEntity> e = sub.from(PawnGeneralEntity.class);
            sub.select(e.get("pawnId"))
               .where(cb.and(
                   cb.equal(e.get("pawnId"), root.get("pawnId")),
                   cb.like(cb.lower(e.get(field)), "%" + value.toLowerCase() + "%")
               ));
            return cb.exists(sub);
        };
    }

    public static Specification<PawnQuery> excludeOldRedeemedItems(LocalDateTime cutoffDate) {
        return (root, query, criteriaBuilder) -> {
            Predicate redeemedStatusPredicate = criteriaBuilder.equal(root.get("pawnStatus"), PawnStatus.REDEEMED);
            Predicate oldRedeemDatePredicate = criteriaBuilder.lessThan(root.get("redeemDate"), cutoffDate);

            Predicate redeemedAndOldPredicate = criteriaBuilder.and(redeemedStatusPredicate, oldRedeemDatePredicate);
            return criteriaBuilder.not(redeemedAndOldPredicate);
        };
    }
}

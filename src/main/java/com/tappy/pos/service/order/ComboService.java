package com.tappy.pos.service.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ComboService {

    List<Map<String, Object>> list(Boolean active);

    Map<String, Object> create(Map<String, Object> body);

    Map<String, Object> update(Long id, Map<String, Object> body);

    void delete(Long id);

    Map<String, Object> getAnalytics(LocalDateTime from, LocalDateTime to, String granularity, int limit);
}

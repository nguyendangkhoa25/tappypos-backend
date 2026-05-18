package com.tappy.pos.service.integration;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.integration.IntegrationProvider;
import com.tappy.pos.model.dto.integration.IntegrationStatusDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IntegrationServiceImpl implements IntegrationService {

    private final Map<String, IntegrationProvider> providers;

    /** Spring injects all IntegrationProvider beans automatically. */
    public IntegrationServiceImpl(List<IntegrationProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(IntegrationProvider::getType, Function.identity()));
        log.info("Registered integration providers: {}", providers.keySet());
    }

    @Override
    public String getAuthUrl(String integrationType, String tenantId) {
        return getProvider(integrationType).buildAuthUrl(tenantId);
    }

    @Override
    public IntegrationStatusDTO handleOAuthCallback(String code, String state, String integrationType) {
        return getProvider(integrationType).handleCallback(code, state);
    }

    @Override
    public IntegrationStatusDTO getStatus(String integrationType, String tenantId) {
        return getProvider(integrationType).getStatus(tenantId);
    }

    @Override
    public void disconnect(String integrationType, String tenantId) {
        getProvider(integrationType).disconnect(tenantId);
    }

    private IntegrationProvider getProvider(String type) {
        IntegrationProvider provider = providers.get(type.toUpperCase());
        if (provider == null) {
            throw new ResourceNotFoundException("Unknown integration type: " + type);
        }
        return provider;
    }
}

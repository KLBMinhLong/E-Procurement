package com.eprocure.approval.infrastructure.persistence.repository;

import com.eprocure.approval.domain.model.ApprovalCondition;
import com.eprocure.approval.domain.model.ApprovalRule;
import com.eprocure.approval.domain.model.ApprovalRuleType;
import com.eprocure.approval.domain.model.ApprovalStepTemplate;
import com.eprocure.approval.domain.model.ApprovalStepType;
import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.eprocure.approval.domain.model.vo.Money;
import com.eprocure.approval.domain.repository.ApprovalRuleRepository;
import com.eprocure.approval.infrastructure.persistence.entity.ApprovalRuleDbEntity;
import com.eprocure.approval.infrastructure.persistence.entity.ApprovalRuleStepDbEntity;
import com.eprocure.approval.infrastructure.persistence.mapper.ApprovalRuleMapper;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

@Repository
public class ApprovalRuleRepositoryImpl implements ApprovalRuleRepository {
    private static final Logger log = LogManager.getLogger(ApprovalRuleRepositoryImpl.class);
    private static final String DEFAULT_CURRENCY = "VND";

    private final ApprovalRuleMapper mapper;

    public ApprovalRuleRepositoryImpl(ApprovalRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ApprovalRule> findActiveRules() {
        log.debug("[REPO] findActiveRules approval_rules");
        return mapper.findActiveRules().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ApprovalRule> findAllRules() {
        log.debug("[REPO] findAllRules approval_rules");
        return mapper.findAllRules().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ApprovalRule> findById(UUID id) {
        log.debug("[REPO] findById approval_rules | id={}", id);
        return Optional.ofNullable(mapper.findById(id)).map(this::toDomain);
    }

    @Override
    public boolean existsByRuleName(String ruleName, UUID excludedId) {
        String normalized = normalizeRuleName(ruleName);
        return mapper.existsByRuleName(normalized, excludedId);
    }

    @Override
    public void save(ApprovalRule rule, UUID actorId) {
        log.debug("[REPO] insert approval_rules | id={}", rule.getId());
        mapper.insertRule(toEntity(rule), actorId);
        rule.getStepTemplates().forEach(step -> mapper.insertStep(toStepEntity(rule.getId(), step), actorId));
    }

    @Override
    public void update(ApprovalRule rule, UUID actorId) {
        log.debug("[REPO] update approval_rules | id={}", rule.getId());
        mapper.updateRule(toEntity(rule), actorId);
        mapper.softDeleteStepsByRuleId(rule.getId(), actorId);
        rule.getStepTemplates().forEach(step -> mapper.insertStep(toStepEntity(rule.getId(), step), actorId));
    }

    @Override
    public void deactivate(UUID id, UUID actorId) {
        log.debug("[REPO] deactivate approval_rules | id={}", id);
        mapper.deactivateRule(id, actorId);
    }

    private ApprovalRule toDomain(ApprovalRuleDbEntity entity) {
        ApprovalCondition condition = ApprovalCondition.of(
                toMoney(entity.getMinValue()),
                toMoney(entity.getMaxValue()),
                toStringSet(entity.getCategories()),
                toUuidSet(entity.getDepartmentIds()),
                toPrioritySet(entity.getPriorities()));
        return ApprovalRule.restore(
                entity.getId(),
                entity.getRuleName(),
                entity.getPriority(),
                Boolean.TRUE.equals(entity.getActive()),
                ApprovalRuleType.valueOf(entity.getRuleType()),
                condition,
                entity.getSteps().stream().map(this::toStepTemplate).toList(),
                entity.getDescription());
    }

    private ApprovalStepTemplate toStepTemplate(ApprovalRuleStepDbEntity entity) {
        return new ApprovalStepTemplate(
                entity.getStepIndex(),
                entity.getApproverRole(),
                ApprovalStepType.valueOf(entity.getStepType()),
                entity.getSlaHours(),
                Boolean.TRUE.equals(entity.getRequired()));
    }

    private ApprovalRuleDbEntity toEntity(ApprovalRule rule) {
        ApprovalRuleDbEntity entity = new ApprovalRuleDbEntity();
        entity.setId(rule.getId());
        entity.setRuleName(rule.getRuleName());
        entity.setPriority(rule.getPriority());
        entity.setActive(rule.isActive());
        entity.setRuleType(rule.getRuleType().name());
        entity.setMinValue(rule.getCondition().getMinValue() == null ? null : rule.getCondition().getMinValue().amount());
        entity.setMaxValue(rule.getCondition().getMaxValue() == null ? null : rule.getCondition().getMaxValue().amount());
        entity.setCategories(rule.getCondition().getCategories().toArray(String[]::new));
        entity.setDepartmentIds(rule.getCondition().getDepartmentIds().toArray(UUID[]::new));
        entity.setPriorities(rule.getCondition().getPriorities().stream().map(Enum::name).toArray(String[]::new));
        entity.setDescription(rule.getDescription());
        return entity;
    }

    private ApprovalRuleStepDbEntity toStepEntity(UUID ruleId, ApprovalStepTemplate step) {
        ApprovalRuleStepDbEntity entity = new ApprovalRuleStepDbEntity();
        entity.setId(UUID.randomUUID());
        entity.setRuleId(ruleId);
        entity.setStepIndex(step.stepIndex());
        entity.setApproverRole(step.approverRole());
        entity.setStepType(step.stepType().name());
        entity.setSlaHours(step.slaHours());
        entity.setRequired(step.required());
        return entity;
    }

    private Money toMoney(BigDecimal value) {
        return value == null ? null : new Money(value, DEFAULT_CURRENCY);
    }

    private Set<String> toStringSet(Object value) {
        return arrayValues(value).stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<UUID> toUuidSet(Object value) {
        return arrayValues(value).stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(item -> !item.isBlank())
                .map(UUID::fromString)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<PurchaseRequestPriority> toPrioritySet(Object value) {
        return arrayValues(value).stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(item -> !item.isBlank())
                .map(PurchaseRequestPriority::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    private List<Object> arrayValues(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Array sqlArray) {
            try {
                Object rawArray = sqlArray.getArray();
                if (rawArray instanceof Object[] values) {
                    return Arrays.asList(values);
                }
                return List.of(rawArray);
            } catch (SQLException exception) {
                throw new IllegalStateException("Cannot read PostgreSQL array", exception);
            }
        }
        if (value instanceof Object[] values) {
            return Arrays.asList(values);
        }
        if (value instanceof Collection<?> values) {
            return values.stream().map(item -> (Object) item).toList();
        }
        return List.of(value);
    }

    private String normalizeRuleName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}

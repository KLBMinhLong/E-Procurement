package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.port.in.ManageCatalogCategoryCommand;
import com.eprocure.pr.application.service.CatalogCategoryAdminView;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.model.CatalogCategory;
import com.eprocure.pr.domain.model.CatalogCategoryAdmin;
import com.eprocure.pr.domain.model.vo.Money;
import com.eprocure.pr.domain.repository.CatalogCategoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCatalogCategoryUseCase {
    private static final Logger log = LogManager.getLogger(CreateCatalogCategoryUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "create-catalog-category";
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]+$");

    private final CatalogCategoryRepository catalogCategoryRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public CreateCatalogCategoryUseCase(
            CatalogCategoryRepository catalogCategoryRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.catalogCategoryRepository = catalogCategoryRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public CatalogCategoryAdminView execute(ManageCatalogCategoryCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                CatalogCategoryAdminView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit CreateCatalogCategory | actorId={} | key={}",
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return cached.get();
        }

        validate(command);
        if (catalogCategoryRepository.existsByCode(command.code())) {
            throw new BusinessException(ErrorCode.PR_017);
        }
        validateParent(command.code(), command.parentCode());

        log.info("[ACTION] Start CreateCatalogCategory | actorId={} | code={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.code());
        CatalogCategory category = CatalogCategory.create(
                command.code(),
                command.name(),
                command.parentCode(),
                command.requiresSpecialApproval(),
                command.specialApproverRole(),
                toMoney(command),
                command.capex(),
                Instant.now(clock));
        catalogCategoryRepository.create(category, command.actorId());
        CatalogCategoryAdminView view = catalogCategoryRepository.findAdminByCode(command.code())
                .map(this::toView)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_016));
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete CreateCatalogCategory | actorId={} | code={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.code());
        return view;
    }

    private void validate(ManageCatalogCategoryCommand command) {
        if (!CODE_PATTERN.matcher(command.code()).matches()
                || command.name().isBlank()
                || command.name().length() > 200
                || (command.specialApproverRole() != null && command.specialApproverRole().length() > 50)) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
        if (command.parentCode() != null && !CODE_PATTERN.matcher(command.parentCode()).matches()) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
    }

    private void validateParent(String code, String parentCode) {
        if (parentCode == null) {
            return;
        }
        if (code.equals(parentCode)) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
        if (!catalogCategoryRepository.existsActiveByCode(parentCode)) {
            throw new BusinessException(ErrorCode.PR_016);
        }
    }

    private Money toMoney(ManageCatalogCategoryCommand command) {
        try {
            return command.requiresRfqAbove() == null ? null : new Money(command.requiresRfqAbove(), "VND");
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
    }

    private CatalogCategoryAdminView toView(CatalogCategoryAdmin category) {
        return new CatalogCategoryAdminView(
                category.code(),
                category.name(),
                category.parentCode().orElse(null),
                category.requiresSpecialApproval(),
                category.specialApproverRole().orElse(null),
                category.requiresRfqAbove().map(money -> money.amount().toPlainString()).orElse(null),
                category.capex(),
                category.itemCount(),
                category.deleted());
    }
}

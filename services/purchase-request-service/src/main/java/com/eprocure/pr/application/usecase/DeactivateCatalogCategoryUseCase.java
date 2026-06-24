package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.port.in.DeactivateCatalogCategoryCommand;
import com.eprocure.pr.application.service.CatalogCategoryAdminView;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.model.CatalogCategoryAdmin;
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
public class DeactivateCatalogCategoryUseCase {
    private static final Logger log = LogManager.getLogger(DeactivateCatalogCategoryUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "deactivate-catalog-category";
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]+$");

    private final CatalogCategoryRepository catalogCategoryRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public DeactivateCatalogCategoryUseCase(
            CatalogCategoryRepository catalogCategoryRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.catalogCategoryRepository = catalogCategoryRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public CatalogCategoryAdminView execute(DeactivateCatalogCategoryCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                CatalogCategoryAdminView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit DeactivateCatalogCategory | actorId={} | key={}",
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return cached.get();
        }
        if (!CODE_PATTERN.matcher(command.code()).matches()) {
            throw new BusinessException(ErrorCode.VAL_001);
        }

        CatalogCategoryAdmin existing = catalogCategoryRepository.findAdminByCode(command.code())
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_016));
        if (existing.deleted()) {
            CatalogCategoryAdminView view = toView(existing);
            idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
            return view;
        }
        if (catalogCategoryRepository.countActiveItems(command.code()) > 0) {
            throw new BusinessException(ErrorCode.PR_018);
        }

        log.info("[ACTION] Start DeactivateCatalogCategory | actorId={} | code={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.code());
        catalogCategoryRepository.deactivate(command.code(), command.actorId(), Instant.now(clock));
        CatalogCategoryAdminView view = catalogCategoryRepository.findAdminByCode(command.code())
                .map(this::toView)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_016));
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete DeactivateCatalogCategory | actorId={} | code={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.code());
        return view;
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

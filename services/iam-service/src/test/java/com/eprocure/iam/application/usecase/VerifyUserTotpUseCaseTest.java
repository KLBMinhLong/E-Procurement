package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.eprocure.iam.application.port.in.VerifyUserTotpCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.TotpSecretCipher;
import com.eprocure.iam.application.service.TotpService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.UserRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerifyUserTotpUseCaseTest {
    private static final UUID USER_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final String DEV_AES_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String SECRET = "JBSWY3DPEHPK3PXP";

    @Mock
    private UserRepository userRepository;

    private TotpService totpService;

    private TotpSecretCipher totpSecretCipher;

    private VerifyUserTotpUseCase useCase;

    @BeforeEach
    void setUp() {
        totpService = new TotpService("eProcure");
        totpSecretCipher = new TotpSecretCipher(DEV_AES_KEY);
        useCase = new VerifyUserTotpUseCase(userRepository, new IdempotencyGuard(), totpService, totpSecretCipher);
    }

    @Test
    void should_verify_totp_when_user_has_enabled_two_factor() {
        User user = twoFactorUser(totpSecretCipher.encrypt(SECRET));
        String code = codeForNow();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        useCase.execute(new VerifyUserTotpCommand(USER_ID, code), IDEMPOTENCY_KEY.toString());
    }

    @Test
    void should_throw_iam_006_when_totp_code_is_invalid() {
        User user = twoFactorUser(totpSecretCipher.encrypt(SECRET));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        String invalidCode = codeForNow().equals("000000") ? "111111" : "000000";

        assertThatThrownBy(() -> useCase.execute(new VerifyUserTotpCommand(USER_ID, invalidCode), IDEMPOTENCY_KEY.toString()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_006);
    }

    @Test
    void should_throw_iam_006_when_two_factor_is_not_enabled() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(activeUser()));

        assertThatThrownBy(() -> useCase.execute(new VerifyUserTotpCommand(USER_ID, "123456"), IDEMPOTENCY_KEY.toString()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_006);
    }

    @Test
    void should_throw_iam_030_when_user_is_not_found() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new VerifyUserTotpCommand(USER_ID, "123456"), IDEMPOTENCY_KEY.toString()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_030);
    }

    private static User twoFactorUser(String encryptedSecret) {
        User user = activeUser();
        setField(user, "twoFactorEnabled", true);
        setField(user, "twoFactorSecretEncrypted", encryptedSecret);
        return user;
    }

    private static User activeUser() {
        return User.create(
                USER_ID,
                "EMP-2026-00001",
                "superadmin",
                "superadmin@eprocure.local",
                "Super Admin",
                DEPARTMENT_ID,
                UserStatus.ACTIVE,
                Instant.parse("2026-06-15T00:00:00Z"));
    }

    private static void setField(User user, String fieldName, Object value) {
        try {
            Field field = User.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(user, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String codeForNow() {
        try {
            Method method = TotpService.class.getDeclaredMethod("generateCode", String.class, Instant.class);
            method.setAccessible(true);
            return (String) method.invoke(totpService, SECRET, Instant.now());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

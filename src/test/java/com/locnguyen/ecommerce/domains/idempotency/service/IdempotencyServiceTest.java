package com.locnguyen.ecommerce.domains.idempotency.service;

import com.locnguyen.ecommerce.common.config.AppProperties;
import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.domains.idempotency.entity.IdempotencyKey;
import com.locnguyen.ecommerce.domains.idempotency.enums.IdempotencyActionType;
import com.locnguyen.ecommerce.domains.idempotency.enums.IdempotencyStatus;
import com.locnguyen.ecommerce.domains.idempotency.repository.IdempotencyKeyRepository;
import com.locnguyen.ecommerce.domains.idempotency.service.impl.IdempotencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link IdempotencyService}.
 *
 * Covers all branch points in findOrCreateProcessing:
 * - New key → inserts PROCESSING
 * - COMPLETED + same hash → replay
 * - COMPLETED + different hash → IDEMPOTENCY_KEY_CONFLICT
 * - PROCESSING + same hash + recent → IDEMPOTENCY_REQUEST_IN_PROGRESS
 * - PROCESSING + same hash + stale → delete + insert new PROCESSING
 * - PROCESSING + different hash → IDEMPOTENCY_KEY_CONFLICT
 * - FAILED + same hash + retryable action → delete + insert new PROCESSING
 * - FAILED + same hash + non-retryable → IDEMPOTENCY_REPLAY_NOT_AVAILABLE
 * - FAILED + different hash → IDEMPOTENCY_KEY_CONFLICT
 * - Concurrent DIVE race → reload and evaluate existing
 * - validateKey: blank → REQUIRED, too long → TOO_LONG
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IdempotencyServiceTest {

    private static final UUID USER_ID = new UUID(0L, 1L);
    private static final String KEY = "client-key-abc";
    private static final String HASH = "aabbccdd";
    private static final String OTHER_HASH = "11223344";

    @Mock IdempotencyKeyRepository repository;
    @Mock AppProperties appProperties;
    @Mock AppProperties.Idempotency idempotencyConfig;

    @InjectMocks IdempotencyServiceImpl service;

    @BeforeEach
    void stubConfig() {
        when(appProperties.getIdempotency()).thenReturn(idempotencyConfig);
        when(idempotencyConfig.getTtlHours()).thenReturn(24L);
        when(idempotencyConfig.getStaleProcessingMinutes()).thenReturn(5L);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private IdempotencyKey record(IdempotencyStatus status, String hash) {
        IdempotencyKey k = new IdempotencyKey();
        k.setId(42L);
        k.setStatus(status);
        k.setRequestHash(hash);
        k.setUserId(USER_ID);
        k.setActionType(IdempotencyActionType.CHECKOUT);
        k.setIdempotencyKey(KEY);
        k.setExpiresAt(LocalDateTime.now().plusHours(24));
        k.setCreatedAt(LocalDateTime.now());
        return k;
    }

    // ─── findOrCreateProcessing ───────────────────────────────────────────────

    @Nested
    class FindOrCreateProcessing {

        @Test
        void inserts_PROCESSING_when_key_does_not_exist() {
            when(repository.findByUserIdAndActionTypeAndIdempotencyKey(any(), any(), any()))
                    .thenReturn(Optional.empty());

            IdempotencyKey result = service.findOrCreateProcessing(
                    USER_ID, IdempotencyActionType.CHECKOUT, KEY, HASH);

            verify(repository).save(any(IdempotencyKey.class));
            assertThat(result.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
        }

        @Test
        void returns_COMPLETED_record_for_replay_when_hash_matches() {
            IdempotencyKey completed = record(IdempotencyStatus.COMPLETED, HASH);
            completed.setResourceId(UUID.randomUUID().toString());
            when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));
            when(repository.findByUserIdAndActionTypeAndIdempotencyKey(any(), any(), any()))
                    .thenReturn(Optional.of(completed));

            IdempotencyKey result = service.findOrCreateProcessing(
                    USER_ID, IdempotencyActionType.CHECKOUT, KEY, HASH);

            assertThat(result.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        }

        @Test
        void throws_IDEMPOTENCY_KEY_CONFLICT_when_COMPLETED_but_hash_differs() {
            IdempotencyKey completed = record(IdempotencyStatus.COMPLETED, HASH);
            when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));
            when(repository.findByUserIdAndActionTypeAndIdempotencyKey(any(), any(), any()))
                    .thenReturn(Optional.of(completed));

            assertThatThrownBy(() -> service.findOrCreateProcessing(
                    USER_ID, IdempotencyActionType.CHECKOUT, KEY, OTHER_HASH))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }

        @Test
        void throws_IDEMPOTENCY_REQUEST_IN_PROGRESS_when_PROCESSING_recent_and_hash_matches() {
            IdempotencyKey processing = record(IdempotencyStatus.PROCESSING, HASH);
            // createdAt = now → not stale
            when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));
            when(repository.findByUserIdAndActionTypeAndIdempotencyKey(any(), any(), any()))
                    .thenReturn(Optional.of(processing));

            assertThatThrownBy(() -> service.findOrCreateProcessing(
                    USER_ID, IdempotencyActionType.CHECKOUT, KEY, HASH))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS);
        }

        @Test
        void replaces_stale_PROCESSING_with_new_PROCESSING() {
            IdempotencyKey stale = record(IdempotencyStatus.PROCESSING, HASH);
            // Make it stale: 10 minutes old (threshold is 5)
            stale.setCreatedAt(LocalDateTime.now().minusMinutes(10));
            when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(repository.findByUserIdAndActionTypeAndIdempotencyKey(any(), any(), any()))
                    .thenReturn(Optional.of(stale));

            IdempotencyKey result = service.findOrCreateProcessing(
                    USER_ID, IdempotencyActionType.CHECKOUT, KEY, HASH);

            verify(repository).delete(stale);
            verify(repository).flush();
            assertThat(result.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
        }

        @Test
        void throws_IDEMPOTENCY_KEY_CONFLICT_when_PROCESSING_and_hash_differs() {
            IdempotencyKey processing = record(IdempotencyStatus.PROCESSING, HASH);
            when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));
            when(repository.findByUserIdAndActionTypeAndIdempotencyKey(any(), any(), any()))
                    .thenReturn(Optional.of(processing));

            assertThatThrownBy(() -> service.findOrCreateProcessing(
                    USER_ID, IdempotencyActionType.CHECKOUT, KEY, OTHER_HASH))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }

        @Test
        void allows_retry_of_FAILED_for_retryable_action_CHECKOUT() {
            IdempotencyKey failed = record(IdempotencyStatus.FAILED, HASH);
            when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(repository.findByUserIdAndActionTypeAndIdempotencyKey(any(), any(), any()))
                    .thenReturn(Optional.of(failed));

            IdempotencyKey result = service.findOrCreateProcessing(
                    USER_ID, IdempotencyActionType.CHECKOUT, KEY, HASH);

            verify(repository).delete(failed);
            assertThat(result.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
        }

        @Test
        void allows_retry_of_FAILED_for_retryable_action_PAYMENT_INITIATE() {
            IdempotencyKey failed = record(IdempotencyStatus.FAILED, HASH);
            failed.setActionType(IdempotencyActionType.PAYMENT_INITIATE);
            when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(repository.findByUserIdAndActionTypeAndIdempotencyKey(any(), any(), any()))
                    .thenReturn(Optional.of(failed));

            IdempotencyKey result = service.findOrCreateProcessing(
                    USER_ID, IdempotencyActionType.PAYMENT_INITIATE, KEY, HASH);

            verify(repository).delete(failed);
            assertThat(result.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
        }

        @Test
        void throws_IDEMPOTENCY_REPLAY_NOT_AVAILABLE_for_FAILED_non_retryable_action() {
            IdempotencyKey failed = record(IdempotencyStatus.FAILED, HASH);
            failed.setActionType(IdempotencyActionType.ORDER_CONFIRM);
            failed.setErrorCode("ORDER_STATUS_INVALID");
            when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));
            when(repository.findByUserIdAndActionTypeAndIdempotencyKey(any(), any(), any()))
                    .thenReturn(Optional.of(failed));

            assertThatThrownBy(() -> service.findOrCreateProcessing(
                    USER_ID, IdempotencyActionType.ORDER_CONFIRM, KEY, HASH))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.IDEMPOTENCY_REPLAY_NOT_AVAILABLE);
        }

        @Test
        void throws_IDEMPOTENCY_KEY_CONFLICT_when_FAILED_and_hash_differs() {
            IdempotencyKey failed = record(IdempotencyStatus.FAILED, HASH);
            when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));
            when(repository.findByUserIdAndActionTypeAndIdempotencyKey(any(), any(), any()))
                    .thenReturn(Optional.of(failed));

            assertThatThrownBy(() -> service.findOrCreateProcessing(
                    USER_ID, IdempotencyActionType.CHECKOUT, KEY, OTHER_HASH))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
    }

    // ─── validateKey ─────────────────────────────────────────────────────────

    @Nested
    class ValidateKey {

        @Test
        void accepts_normal_key() {
            String result = service.validateKey("  my-order-key  ");
            assertThat(result).isEqualTo("my-order-key");
        }

        @Test
        void throws_IDEMPOTENCY_KEY_REQUIRED_when_blank() {
            assertThatThrownBy(() -> service.validateKey("   "))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        @Test
        void throws_IDEMPOTENCY_KEY_REQUIRED_when_null() {
            assertThatThrownBy(() -> service.validateKey(null))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        @Test
        void throws_IDEMPOTENCY_KEY_TOO_LONG_when_exceeds_100_chars() {
            String longKey = "x".repeat(101);
            assertThatThrownBy(() -> service.validateKey(longKey))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_TOO_LONG);
        }

        @Test
        void accepts_exactly_100_char_key() {
            String key = "k".repeat(100);
            assertThat(service.validateKey(key)).isEqualTo(key);
        }
    }

    // ─── markComplete ────────────────────────────────────────────────────────

    @Nested
    class MarkComplete {

        @Test
        void calls_repository_markCompleted_with_correct_fields() {
            service.markComplete(42L, "ORDER", "some-order-uuid", 201);

            verify(repository).markCompleted(
                    eq(42L),
                    eq(IdempotencyStatus.COMPLETED),
                    eq("ORDER"),
                    eq("some-order-uuid"),
                    eq(201),
                    isNull(),
                    any(LocalDateTime.class));
        }
    }

    // ─── markFailed ──────────────────────────────────────────────────────────

    @Nested
    class MarkFailed {

        @Test
        void calls_repository_markFailed_with_correct_error_code() {
            service.markFailed(42L, "CART_NOT_FOUND");

            verify(repository).markFailed(
                    eq(42L),
                    eq(IdempotencyStatus.FAILED),
                    eq("CART_NOT_FOUND"),
                    any(LocalDateTime.class));
        }

        @Test
        void does_not_throw_when_repository_fails() {
            doThrow(new RuntimeException("DB down")).when(repository)
                    .markFailed(anyLong(), any(), any(), any());

            // Should swallow the exception (best-effort)
            service.markFailed(42L, "SOME_ERROR");
        }
    }
}

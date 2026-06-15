package lucas.basemodel.modules.wealth.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.wealth.models.WealthSnapshot;
import lucas.basemodel.modules.wealth.models.WealthSuggestion;
import lucas.basemodel.modules.wealth.repositories.WealthSnapshotRepository;
import lucas.basemodel.modules.wealth.repositories.WealthSuggestionRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantém snapshots de patrimônio em cache, atualizados de forma assíncrona.
 * Elimina o custo de updateAllValuations() + createSnapshot() + generateSuggestions()
 * do request HTTP síncrono do dashboard.
 *
 * Estratégia:
 *  - Snapshot é recalculado a cada 5 minutos para todos os usuários ativos.
 *  - O cache pode ser invalidado explicitamente via invalidate(userId)
 *    (chamado ao salvar uma nova transação/ativo).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WealthSnapshotCacheService {

    private final WealthService wealthService;
    private final WealthSuggestionService suggestionService;
    private final UsuarioRepository usuarioRepository;
    private final WealthSuggestionRepository suggestionRepository;
    private final WealthSnapshotRepository snapshotRepository;

    // userId → cached snapshot
    private final Map<UUID, WealthSnapshot> snapshotCache = new ConcurrentHashMap<>();
    // userId → last refresh timestamp
    private final Map<UUID, Long> lastRefreshAt = new ConcurrentHashMap<>();

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L; // 5 minutos

    /**
     * Retorna o snapshot do cache, recalculando em background se expirado.
     * Nunca bloqueia o request thread — usa dado anterior se disponível.
     */
    public WealthSnapshot getSnapshot(User user) {
        UUID userId = user.getId();
        WealthSnapshot cached = snapshotCache.get(userId);
        long now = System.currentTimeMillis();

        if (cached == null || isExpired(userId, now)) {
            // Se não tem cache ainda, calcula de forma síncrona (primeira vez apenas)
            if (cached == null) {
                return refreshSync(user);
            }
            // Se tem cache mas expirou, retorna o antigo e dispara atualização em background
            refreshAsync(user);
        }
        return cached;
    }

    /**
     * Retorna o histórico de snapshots do usuário (últimos N), consultando o banco.
     * Usado pelo dashboard para o gráfico de evolução do patrimônio.
     */
    public List<WealthSnapshot> getSnapshotHistory(UUID userId, int limit) {
        List<WealthSnapshot> all = snapshotRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (all.size() > limit) {
            return new java.util.ArrayList<>(all.subList(0, limit));
        }
        return new java.util.ArrayList<>(all);
    }

    /** Invalida o cache de um usuário (chamar após salvar transação ou ativo). */
    public void invalidate(UUID userId) {
        snapshotCache.remove(userId);
        lastRefreshAt.remove(userId);
        log.debug("Wealth snapshot cache invalidated for user: {}", userId);
    }

    /** Scheduled — atualiza todos os usuários com cache expirado a cada 5 minutos. */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60_000L)
    public void refreshAll() {
        long now = System.currentTimeMillis();
        for (UUID userId : snapshotCache.keySet()) {
            if (isExpired(userId, now)) {
                usuarioRepository.findById(userId).ifPresent(user -> {
                    try {
                        refreshSync(user);
                    } catch (Exception e) {
                        log.warn("Failed to refresh wealth snapshot for user {}: {}", userId, e.getMessage());
                    }
                });
            }
        }
    }

    @Async
    public void refreshAsync(User user) {
        try {
            refreshSync(user);
        } catch (Exception e) {
            log.warn("Async wealth snapshot refresh failed for user {}: {}", user.getId(), e.getMessage());
        }
    }

    @Transactional
    public WealthSnapshot refreshSync(User user) {
        wealthService.updateAllValuations(user.getId());
        WealthSnapshot snapshot = wealthService.createSnapshot(user);
        suggestionService.generateSuggestions(user, snapshot);
        snapshotCache.put(user.getId(), snapshot);
        lastRefreshAt.put(user.getId(), System.currentTimeMillis());
        log.debug("Wealth snapshot refreshed for user: {}", user.getId());
        return snapshot;
    }

    /** Retorna as suggestions do repositório (já populadas pelo refreshSync). */
    public List<WealthSuggestion> getSuggestions(UUID userId) {
        return suggestionRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId);
    }

    private boolean isExpired(UUID userId, long now) {
        Long last = lastRefreshAt.get(userId);
        return last == null || (now - last) > CACHE_TTL_MS;
    }
}

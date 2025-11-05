package com.david.open.problem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.david.core.exception.BusinessException;
import com.david.open.problem.dto.ProblemCardView;
import com.david.open.problem.dto.ProblemCardView.DifficultyInfo;
import com.david.open.problem.dto.ProblemCardView.ProblemMetadata;
import com.david.open.problem.dto.ProblemCardView.ProblemStats;
import com.david.open.problem.dto.ProblemCardView.TagInfo;
import com.david.open.problem.dto.ProblemDetailResponse;
import com.david.open.problem.dto.ProblemDetailResponse.LanguageConfig;
import com.david.open.problem.dto.ProblemListResponse;
import com.david.open.problem.entity.Difficulty;
import com.david.open.problem.entity.Language;
import com.david.open.problem.entity.Problem;
import com.david.open.problem.entity.ProblemLanguageConfig;
import com.david.open.problem.entity.ProblemStatement;
import com.david.open.problem.mapper.DifficultyMapper;
import com.david.open.problem.mapper.LanguageMapper;
import com.david.open.problem.mapper.ProblemMapper;
import com.david.open.problem.mapper.ProblemLanguageConfigMapper;
import com.david.open.problem.mapper.ProblemStatementMapper;
import com.david.open.problem.mapper.ProblemTagMapper;
import com.david.open.problem.mapper.ProblemTagMapper.TagRelationRow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ProblemQueryService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String DEFAULT_LANG = "zh-CN";
    private static final List<String> FALLBACK_LANGS = List.of("zh-CN", "en");

    private final ProblemMapper problemMapper;
    private final ProblemStatementMapper problemStatementMapper;
    private final DifficultyMapper difficultyMapper;
    private final ProblemTagMapper problemTagMapper;
    private final ProblemLanguageConfigMapper problemLanguageConfigMapper;
    private final LanguageMapper languageMapper;
    private final ObjectMapper objectMapper;

    public ProblemQueryService(
            ProblemMapper problemMapper,
            ProblemStatementMapper problemStatementMapper,
            DifficultyMapper difficultyMapper,
            ProblemTagMapper problemTagMapper,
            ProblemLanguageConfigMapper problemLanguageConfigMapper,
            LanguageMapper languageMapper,
            ObjectMapper objectMapper) {
        this.problemMapper = problemMapper;
        this.problemStatementMapper = problemStatementMapper;
        this.difficultyMapper = difficultyMapper;
        this.problemTagMapper = problemTagMapper;
        this.problemLanguageConfigMapper = problemLanguageConfigMapper;
        this.languageMapper = languageMapper;
        this.objectMapper = objectMapper;
    }

    public ProblemListResponse listProblems(int page, int size, String langCode) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;
        String normalizedLang = normalizeLang(langCode);

        LambdaQueryWrapper<Problem> countQuery = basePublishedProblemQuery();
        long total = problemMapper.selectCount(countQuery);
        if (total == 0) {
            return new ProblemListResponse(0, safePage, safeSize, false, List.of());
        }

        LambdaQueryWrapper<Problem> pageQuery =
                basePublishedProblemQuery()
                        .orderByDesc(Problem::getUpdatedAt)
                        .last("LIMIT " + safeSize + " OFFSET " + offset);
        List<Problem> problems = problemMapper.selectList(pageQuery);
        if (problems.isEmpty()) {
            return new ProblemListResponse(total, safePage, safeSize, false, List.of());
        }

        List<Long> problemIds = problems.stream().map(Problem::getId).toList();
        Map<Long, ProblemStatement> statements = loadStatements(problemIds, normalizedLang);
        Map<Integer, Difficulty> difficultyMap =
                loadDifficulties(
                        problems.stream()
                                .map(Problem::getDifficultyId)
                                .collect(Collectors.toSet()));
        Map<Long, List<TagInfo>> tagsByProblem = loadTags(problemIds);

        List<ProblemCardView> items =
                problems.stream()
                        .map(
                                problem ->
                                        toView(
                                                problem,
                                                statements.get(problem.getId()),
                                                difficultyMap.get(problem.getDifficultyId()),
                                                tagsByProblem.getOrDefault(
                                                        problem.getId(), List.of())))
                        .toList();

        boolean hasMore = (long) safePage * safeSize < total;
        return new ProblemListResponse(total, safePage, safeSize, hasMore, items);
    }

    public ProblemDetailResponse getProblemDetail(String slug, String langCode) {
        if (slug == null || slug.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "题目标识不能为空");
        }
        String normalizedSlug = slug.trim();
        String normalizedLang = normalizeLang(langCode);

        Problem problem =
                problemMapper.selectOne(
                        basePublishedProblemQuery().eq(Problem::getSlug, normalizedSlug));
        if (problem == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "题目不存在或尚未公开");
        }

        ProblemStatement statement =
                loadStatements(List.of(problem.getId()), normalizedLang).get(problem.getId());
        if (statement == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "题目描述暂不可用");
        }

        Difficulty difficulty =
                problem.getDifficultyId() == null
                        ? null
                        : difficultyMapper.selectById(problem.getDifficultyId());
        List<TagInfo> tags = loadTags(List.of(problem.getId())).getOrDefault(problem.getId(), List.of());
        DifficultyInfo difficultyInfo =
                difficulty == null
                        ? null
                        : new DifficultyInfo(
                                difficulty.getId(),
                                difficulty.getCode(),
                                difficultyLabel(difficulty.getCode()));

        ProblemStats stats = new ProblemStats(problem.getTimeLimitMs(), problem.getMemoryLimitKb());
        ProblemMetadata metadata = parseMetadata(problem.getId(), problem.getMetaJson());
        List<LanguageConfig> languageConfigs = loadLanguageConfigs(problem.getId());

        return new ProblemDetailResponse(
                problem.getId(),
                problem.getSlug(),
                statement.getTitle(),
                statement.getDescriptionMd(),
                statement.getConstraintsMd(),
                statement.getExamplesMd(),
                difficultyInfo,
                stats,
                metadata,
                tags,
                languageConfigs,
                problem.getUpdatedAt());
    }

    private LambdaQueryWrapper<Problem> basePublishedProblemQuery() {
        return Wrappers.lambdaQuery(Problem.class)
                .eq(Problem::getIsPublic, 1)
                .eq(Problem::getLifecycleStatus, "published")
                .eq(Problem::getReviewStatus, "approved");
    }

    private Map<Long, ProblemStatement> loadStatements(
            Collection<Long> problemIds, String preferredLang) {
        if (problemIds.isEmpty()) {
            return Map.of();
        }
        List<String> languageOrder = buildLanguagePreference(preferredLang);
        LambdaQueryWrapper<ProblemStatement> statementQuery =
                Wrappers.lambdaQuery(ProblemStatement.class)
                        .in(ProblemStatement::getProblemId, problemIds)
                        .in(ProblemStatement::getLangCode, languageOrder);
        List<ProblemStatement> statements = problemStatementMapper.selectList(statementQuery);
        Map<Long, Map<String, ProblemStatement>> grouped = new LinkedHashMap<>();
        for (ProblemStatement statement : statements) {
            grouped.computeIfAbsent(statement.getProblemId(), ignored -> new LinkedHashMap<>())
                    .put(statement.getLangCode(), statement);
        }

        Map<Long, ProblemStatement> resolved = new LinkedHashMap<>();
        for (Long problemId : problemIds) {
            Map<String, ProblemStatement> candidates = grouped.get(problemId);
            if (candidates == null || candidates.isEmpty()) {
                continue;
            }
            for (String lang : languageOrder) {
                ProblemStatement preferred = candidates.get(lang);
                if (preferred != null) {
                    resolved.put(problemId, preferred);
                    break;
                }
            }
            if (!resolved.containsKey(problemId)) {
                resolved.put(problemId, candidates.values().iterator().next());
            }
        }
        return resolved;
    }

    private Map<Integer, Difficulty> loadDifficulties(Set<Integer> difficultyIds) {
        if (difficultyIds == null || difficultyIds.isEmpty()) {
            return Map.of();
        }
        return difficultyMapper.selectBatchIds(difficultyIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Difficulty::getId, Function.identity()));
    }

    private Map<Long, List<TagInfo>> loadTags(Collection<Long> problemIds) {
        if (problemIds.isEmpty()) {
            return Map.of();
        }
        List<TagRelationRow> rows = problemTagMapper.selectTagsForProblems(problemIds);
        Map<Long, List<TagInfo>> result = new LinkedHashMap<>();
        for (TagRelationRow row : rows) {
            result.computeIfAbsent(row.problemId(), key -> new ArrayList<>())
                    .add(new TagInfo(row.tagId(), row.name(), row.slug()));
        }
        return result;
    }

    private List<LanguageConfig> loadLanguageConfigs(Long problemId) {
        List<ProblemLanguageConfig> configs =
                problemLanguageConfigMapper.selectList(
                        Wrappers.lambdaQuery(ProblemLanguageConfig.class)
                                .eq(ProblemLanguageConfig::getProblemId, problemId));
        if (configs.isEmpty()) {
            return List.of();
        }

        Set<Integer> languageIds =
                configs.stream()
                        .map(ProblemLanguageConfig::getLanguageId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Map<Integer, Language> languageMap =
                languageIds.isEmpty()
                        ? Map.of()
                        : languageMapper.selectBatchIds(languageIds).stream()
                                .filter(Objects::nonNull)
                                .collect(Collectors.toMap(Language::getId, Function.identity()));

        return configs.stream()
                .map(
                        cfg -> {
                            Language lang = languageMap.get(cfg.getLanguageId());
                            return new LanguageConfig(
                                    cfg.getLanguageId(),
                                    lang != null ? lang.getCode() : null,
                                    lang != null ? lang.getDisplayName() : null,
                                    cfg.getFunctionName(),
                                    cfg.getStarterCode());
                        })
                .sorted(
                        (a, b) -> {
                            String left = a.languageName() != null ? a.languageName() : "";
                            String right = b.languageName() != null ? b.languageName() : "";
                            return left.compareToIgnoreCase(right);
                        })
                .toList();
    }

    private ProblemCardView toView(
            Problem problem,
            ProblemStatement statement,
            Difficulty difficulty,
            List<TagInfo> tags) {
        String title =
                statement != null && statement.getTitle() != null
                        ? statement.getTitle()
                        : problem.getSlug();
        DifficultyInfo difficultyInfo =
                difficulty == null
                        ? null
                        : new DifficultyInfo(
                                difficulty.getId(),
                                difficulty.getCode(),
                                difficultyLabel(difficulty.getCode()));

        ProblemMetadata metadata = parseMetadata(problem.getId(), problem.getMetaJson());
        ProblemStats stats = new ProblemStats(problem.getTimeLimitMs(), problem.getMemoryLimitKb());
        LocalDateTime updatedAt = problem.getUpdatedAt();

        return new ProblemCardView(
                problem.getId(),
                problem.getSlug(),
                title,
                difficultyInfo,
                tags,
                stats,
                metadata,
                updatedAt);
    }

    private ProblemMetadata parseMetadata(Long problemId, String metaJson) {
        if (metaJson == null || metaJson.isBlank()) {
            return ProblemMetadata.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(metaJson);
            List<String> companies = new ArrayList<>();
            JsonNode companiesNode = root.path("companies");
            if (companiesNode.isArray()) {
                for (JsonNode node : companiesNode) {
                    if (node.isTextual()) {
                        companies.add(node.asText());
                    }
                }
            }
            Double frequency =
                    root.path("frequency").isNumber() ? root.get("frequency").doubleValue() : null;
            Boolean paidOnly = extractBoolean(root, "paid_only");
            Boolean leetcodeStyle = extractBoolean(root, "leetcode_style");
            Integer frontendId =
                    root.hasNonNull("frontend_id") ? root.get("frontend_id").asInt() : null;
            return new ProblemMetadata(companies, frequency, paidOnly, frontendId, leetcodeStyle);
        } catch (Exception e) {
            log.warn("解析题目 {} 的元数据失败: {}", problemId, e.getMessage());
            return ProblemMetadata.empty();
        }
    }

    private Boolean extractBoolean(JsonNode root, String field) {
        if (!root.has(field) || root.get(field).isNull()) {
            return null;
        }
        JsonNode node = root.get(field);
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isTextual()) {
            return Boolean.parseBoolean(node.asText());
        }
        if (node.isNumber()) {
            return node.asInt() != 0;
        }
        return null;
    }

    private String difficultyLabel(String code) {
        if (code == null) {
            return "未知";
        }
        return switch (code.toLowerCase()) {
            case "easy" -> "简单";
            case "medium" -> "中等";
            case "hard" -> "困难";
            default -> "未知";
        };
    }

    private String normalizeLang(String langCode) {
        if (langCode == null || langCode.isBlank()) {
            return DEFAULT_LANG;
        }
        return langCode.trim();
    }

    private List<String> buildLanguagePreference(String preferredLang) {
        LinkedHashSet<String> langs = new LinkedHashSet<>();
        if (preferredLang != null && !preferredLang.isBlank()) {
            langs.add(preferredLang.trim());
        }
        langs.addAll(FALLBACK_LANGS);
        return List.copyOf(langs);
    }
}

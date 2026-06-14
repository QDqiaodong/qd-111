package com.instrument.service.impl;

import com.instrument.service.DictService;
import com.instrument.vo.DictVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY = "instrument:dict";

    private static final List<DictVO> ACCESSORY_TYPES = Arrays.asList(
            new DictVO("string", "琴弦"),
            new DictVO("bow", "琴弓"),
            new DictVO("pick", "拨片"),
            new DictVO("rosin", "松香"),
            new DictVO("capo", "变调夹"),
            new DictVO("strap", "背带"),
            new DictVO("cleaner", "清洁用品"),
            new DictVO("other", "其他")
    );

    private static final List<DictVO> INSTRUMENTS = Arrays.asList(
            new DictVO("guitar-acoustic", "木吉他"),
            new DictVO("guitar-electric", "电吉他"),
            new DictVO("guitar-bass", "贝斯"),
            new DictVO("violin", "小提琴"),
            new DictVO("piano", "钢琴"),
            new DictVO("ukulele", "尤克里里"),
            new DictVO("erhu", "二胡"),
            new DictVO("other", "其他")
    );

    private static final List<DictVO> WORN_STATUSES = Arrays.asList(
            new DictVO("good", "完好"),
            new DictVO("slight", "轻微磨损"),
            new DictVO("severe", "严重损耗"),
            new DictVO("broken", "已损坏")
    );

    private static final List<DictVO> REPLACEMENT_CYCLES = Arrays.asList(
            new DictVO("30", "30天（月抛）"),
            new DictVO("60", "60天"),
            new DictVO("90", "90天（季抛）"),
            new DictVO("180", "180天（半年）"),
            new DictVO("365", "365天（年抛）"),
            new DictVO("730", "730天（两年）")
    );

    private static final Map<String, Integer> STANDARD_CYCLE_MAP = new HashMap<>();

    static {
        STANDARD_CYCLE_MAP.put("string", 90);
        STANDARD_CYCLE_MAP.put("bow", 365);
        STANDARD_CYCLE_MAP.put("pick", 60);
        STANDARD_CYCLE_MAP.put("rosin", 180);
        STANDARD_CYCLE_MAP.put("capo", 730);
        STANDARD_CYCLE_MAP.put("strap", 730);
        STANDARD_CYCLE_MAP.put("cleaner", 180);
        STANDARD_CYCLE_MAP.put("other", 365);
    }

    @PostConstruct
    public void init() {
        try {
            HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
            hashOps.putIfAbsent(CACHE_KEY, "accessoryTypes", ACCESSORY_TYPES);
            hashOps.putIfAbsent(CACHE_KEY, "instruments", INSTRUMENTS);
            hashOps.putIfAbsent(CACHE_KEY, "wornStatuses", WORN_STATUSES);
            hashOps.putIfAbsent(CACHE_KEY, "replacementCycles", REPLACEMENT_CYCLES);
            hashOps.putIfAbsent(CACHE_KEY, "standardCycles", STANDARD_CYCLE_MAP);
            redisTemplate.expire(CACHE_KEY, 24, TimeUnit.HOURS);
            log.info("字典数据已加载到Redis缓存");
        } catch (Exception e) {
            log.warn("Redis缓存初始化失败，将使用内存数据: {}", e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DictVO> accessoryTypes() {
        try {
            Object cached = redisTemplate.opsForHash().get(CACHE_KEY, "accessoryTypes");
            if (cached instanceof List) {
                return (List<DictVO>) cached;
            }
        } catch (Exception ignored) {}
        return ACCESSORY_TYPES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DictVO> instruments() {
        try {
            Object cached = redisTemplate.opsForHash().get(CACHE_KEY, "instruments");
            if (cached instanceof List) {
                return (List<DictVO>) cached;
            }
        } catch (Exception ignored) {}
        return INSTRUMENTS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DictVO> wornStatuses() {
        try {
            Object cached = redisTemplate.opsForHash().get(CACHE_KEY, "wornStatuses");
            if (cached instanceof List) {
                return (List<DictVO>) cached;
            }
        } catch (Exception ignored) {}
        return WORN_STATUSES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DictVO> replacementCycles() {
        try {
            Object cached = redisTemplate.opsForHash().get(CACHE_KEY, "replacementCycles");
            if (cached instanceof List) {
                return (List<DictVO>) cached;
            }
        } catch (Exception ignored) {}
        return REPLACEMENT_CYCLES;
    }

    @Override
    public String getAccessoryTypeLabel(String code) {
        return accessoryTypes().stream()
                .filter(d -> d.getCode().equals(code))
                .map(DictVO::getLabel)
                .findFirst().orElse(code);
    }

    @Override
    public String getInstrumentLabel(String code) {
        return instruments().stream()
                .filter(d -> d.getCode().equals(code))
                .map(DictVO::getLabel)
                .findFirst().orElse(code);
    }

    @Override
    public String getWornStatusLabel(String code) {
        return wornStatuses().stream()
                .filter(d -> d.getCode().equals(code))
                .map(DictVO::getLabel)
                .findFirst().orElse(code);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Integer getStandardCycle(String accessoryType) {
        try {
            Object cached = redisTemplate.opsForHash().get(CACHE_KEY, "standardCycles");
            if (cached instanceof Map) {
                Map<String, Integer> map = (Map<String, Integer>) cached;
                return map.getOrDefault(accessoryType, 90);
            }
        } catch (Exception ignored) {}
        return STANDARD_CYCLE_MAP.getOrDefault(accessoryType, 90);
    }
}

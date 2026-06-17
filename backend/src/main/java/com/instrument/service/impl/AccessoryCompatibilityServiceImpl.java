package com.instrument.service.impl;

import com.instrument.service.AccessoryCompatibilityService;
import com.instrument.service.DictService;
import com.instrument.vo.AccessoryCompatibilityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessoryCompatibilityServiceImpl implements AccessoryCompatibilityService {

    private final DictService dictService;

    private static final Set<String> STRING_INSTRUMENTS = new HashSet<>(Arrays.asList(
            "guitar-acoustic", "guitar-electric", "guitar-bass",
            "violin", "ukulele", "erhu"
    ));

    private static final Set<String> FRETED_INSTRUMENTS = new HashSet<>(Arrays.asList(
            "guitar-acoustic", "guitar-electric", "guitar-bass", "ukulele"
    ));

    private static final Set<String> BOWED_INSTRUMENTS = new HashSet<>(Arrays.asList(
            "violin", "erhu"
    ));

    private static final Set<String> VIOLIN_FAMILY = new HashSet<>(Arrays.asList(
            "violin"
    ));

    private static final Set<String> GUITAR_FAMILY = new HashSet<>(Arrays.asList(
            "guitar-acoustic", "guitar-electric"
    ));

    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+/\\d+|\\d+\\.?\\d*mm|\\d+\\.?\\d*cm)");
    private static final Pattern GAUGE_PATTERN = Pattern.compile("0\\d{2,3}");
    private static final Pattern THICKNESS_PATTERN = Pattern.compile("(\\d+\\.?\\d*)\\s*(mm|厘米|cm)?");

    @Override
    public AccessoryCompatibilityVO checkCompatibility(String typeCode, String instrument, String specification) {
        return checkCompatibility(typeCode, instrument, specification, null);
    }

    @Override
    public AccessoryCompatibilityVO checkCompatibility(String typeCode, String instrument, String specification, String name) {
        AccessoryCompatibilityVO vo = new AccessoryCompatibilityVO();
        vo.setCompatible(true);

        if (!StringUtils.hasText(typeCode)) {
            vo.addError("请先选择配件类型");
            vo.setCompatible(false);
            vo.setSummary("配件类型未选择");
            return vo;
        }

        if (!StringUtils.hasText(instrument)) {
            vo.addWarning("请选择适配乐器以进行完整的适配校验");
            vo.setSummary("适配乐器未选择，无法进行完整校验");
            return vo;
        }

        String typeName = dictService.getAccessoryTypeLabel(typeCode);
        String instrumentName = dictService.getInstrumentLabel(instrument);

        switch (typeCode) {
            case "string":
                checkStringCompatibility(vo, instrument, specification, instrumentName);
                break;
            case "pick":
                checkPickCompatibility(vo, instrument, specification, instrumentName);
                break;
            case "rosin":
                checkRosinCompatibility(vo, instrument, specification, instrumentName);
                break;
            case "capo":
                checkCapoCompatibility(vo, instrument, specification, instrumentName);
                break;
            case "bow":
                checkBowCompatibility(vo, instrument, specification, instrumentName);
                break;
            case "strap":
                checkStrapCompatibility(vo, instrument, instrumentName);
                break;
            case "cleaner":
                checkCleanerCompatibility(vo, instrument, specification, instrumentName);
                break;
            default:
                vo.addWarning("该配件类型暂无专门的适配校验规则");
                break;
        }

        if (vo.hasIssues()) {
            if (!vo.getErrors().isEmpty()) {
                vo.setCompatible(false);
                vo.setSummary(typeName + "与" + instrumentName + "存在" + vo.getErrors().size() + "个不匹配问题");
                vo.setSuggestion("请检查配件规格是否正确，或确认乐器选择是否合适");
            } else {
                vo.setSummary(typeName + "与" + instrumentName + "基本匹配，但有" + vo.getWarnings().size() + "个需要注意的事项");
                vo.setSuggestion("请确认规格参数是否符合实际需求");
            }
        } else {
            vo.setSummary(typeName + "与" + instrumentName + "适配良好");
            vo.setSuggestion("规格参数符合常规标准，可以正常建档");
        }

        return vo;
    }

    private void checkStringCompatibility(AccessoryCompatibilityVO vo, String instrument, String specification, String instrumentName) {
        if (!STRING_INSTRUMENTS.contains(instrument)) {
            vo.addError("琴弦配件仅适用于弦乐器，当前选择的" + instrumentName + "不是弦乐器");
            return;
        }

        if (!StringUtils.hasText(specification)) {
            vo.addWarning("请填写规格参数以进行更精确的琴弦适配校验");
            return;
        }

        String specLower = specification.toLowerCase();

        if (GUITAR_FAMILY.contains(instrument)) {
            checkGuitarString(vo, specLower, instrumentName);
        } else if (instrument.equals("guitar-bass")) {
            checkBassString(vo, specLower, instrumentName);
        } else if (VIOLIN_FAMILY.contains(instrument)) {
            checkViolinString(vo, specLower, specification, instrumentName);
        } else if (instrument.equals("ukulele")) {
            checkUkuleleString(vo, specLower, instrumentName);
        } else if (instrument.equals("erhu")) {
            checkErhuString(vo, specLower, instrumentName);
        }

        if (specLower.contains("bass") && !instrument.equals("guitar-bass")) {
            vo.addError("贝斯琴弦不适用于" + instrumentName + "，请检查是否选错了配件类型");
        }

        if (specLower.contains("violin") && !instrument.equals("violin")) {
            vo.addError("小提琴琴弦不适用于" + instrumentName);
        }
    }

    private void checkGuitarString(AccessoryCompatibilityVO vo, String specLower, String instrumentName) {
        Matcher gaugeMatcher = GAUGE_PATTERN.matcher(specLower);
        if (gaugeMatcher.find()) {
            String gauge = gaugeMatcher.group();
            try {
                int gaugeNum = Integer.parseInt(gauge);
                if (gaugeNum < 8) {
                    vo.addWarning("琴弦规格" + gauge + "过细，可能容易断弦");
                } else if (gaugeNum > 14) {
                    vo.addWarning("琴弦规格" + gauge + "较粗，按弦会比较费力，适合指弹或降弦");
                }
                if (gaugeNum >= 9 && gaugeNum <= 11) {
                    vo.addWarning(instrumentName + "琴弦规格" + gauge + "属于常用范围");
                }
            } catch (NumberFormatException ignored) {}
        }

        if (specLower.contains("磷铜") || specLower.contains("bronze")) {
            vo.addWarning("磷铜琴弦音色温暖，适合木吉他指弹");
        } else if (specLower.contains("黄铜") || specLower.contains("brass")) {
            vo.addWarning("黄铜琴弦音色明亮，适合木吉他扫弦");
        } else if (specLower.contains("镍") || specLower.contains("nickel")) {
            if (instrumentName.contains("木吉他")) {
                vo.addWarning("镍钢琴弦通常用于电吉他，木吉他使用可能音色偏硬");
            }
        }

        if (specLower.contains("覆膜") || specLower.contains("coated")) {
            vo.addWarning("覆膜琴弦寿命更长，但价格较高");
        }
    }

    private void checkBassString(AccessoryCompatibilityVO vo, String specLower, String instrumentName) {
        if (specLower.contains("040") || specLower.contains("045") || specLower.contains("050")) {
            vo.addWarning("贝斯琴弦规格正常");
        } else if (GAUGE_PATTERN.matcher(specLower).find()) {
            Matcher gaugeMatcher = GAUGE_PATTERN.matcher(specLower);
            if (gaugeMatcher.find()) {
                String gauge = gaugeMatcher.group();
                try {
                    int gaugeNum = Integer.parseInt(gauge);
                    if (gaugeNum < 40) {
                        vo.addWarning("贝斯琴弦规格" + gauge + "过细，可能音色不够饱满");
                    } else if (gaugeNum > 55) {
                        vo.addWarning("贝斯琴弦规格" + gauge + "较粗，按弦费力");
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    private void checkViolinString(AccessoryCompatibilityVO vo, String specLower, String specification, String instrumentName) {
        if (specification.contains("4/4") || specification.contains("3/4") || specification.contains("1/2")) {
            vo.addWarning("小提琴尺寸规格已填写，请确认与乐器尺寸一致");
        } else {
            vo.addWarning("建议在规格中注明小提琴尺寸（如4/4、3/4等）");
        }

        if (specLower.contains("钢弦") || specLower.contains("steel")) {
            vo.addWarning("钢弦音色明亮，适合练习");
        } else if (specLower.contains("羊肠") || specLower.contains("gut")) {
            vo.addWarning("羊肠弦音色温暖，但对湿度敏感，需要精心保养");
        }
    }

    private void checkUkuleleString(AccessoryCompatibilityVO vo, String specLower, String instrumentName) {
        if (specLower.contains("尼龙") || specLower.contains("nylon")) {
            vo.addWarning("尤克里里通常使用尼龙弦");
        } else if (specLower.contains("氟碳") || specLower.contains("fluorocarbon")) {
            vo.addWarning("氟碳弦音色透亮，是尤克里里的高端选择");
        }
    }

    private void checkErhuString(AccessoryCompatibilityVO vo, String specLower, String instrumentName) {
        if (specLower.contains("银质") || specLower.contains("silver")) {
            vo.addWarning("银质二胡弦音色通透");
        }
        if (specLower.contains("内弦") || specLower.contains("外弦")) {
            vo.addWarning("已注明内外弦，建议成套购买");
        }
    }

    private void checkPickCompatibility(AccessoryCompatibilityVO vo, String instrument, String specification, String instrumentName) {
        if (!FRETED_INSTRUMENTS.contains(instrument) && !instrument.equals("other")) {
            vo.addError("拨片通常用于吉他、贝斯、尤克里里等有品弦乐器，" + instrumentName + "一般不使用拨片");
            return;
        }

        if (!StringUtils.hasText(specification)) {
            vo.addWarning("请填写拨片厚度规格以进行适配校验");
            return;
        }

        double thickness = extractThickness(specification);
        if (thickness > 0) {
            if (instrument.equals("guitar-acoustic")) {
                if (thickness < 0.6) {
                    vo.addWarning("薄拨片（" + thickness + "mm）适合木吉他扫弦");
                } else if (thickness >= 0.6 && thickness <= 0.88) {
                    vo.addWarning("中等厚度拨片（" + thickness + "mm）适合木吉他综合演奏");
                } else {
                    vo.addWarning("厚拨片（" + thickness + "mm）适合木吉他独奏，但扫弦可能偏硬");
                }
            } else if (instrument.equals("guitar-electric")) {
                if (thickness < 0.7) {
                    vo.addWarning("薄拨片（" + thickness + "mm）适合电吉他节奏扫弦");
                } else if (thickness >= 0.7 && thickness <= 1.0) {
                    vo.addWarning("中等厚度拨片（" + thickness + "mm）适合电吉他综合演奏");
                } else {
                    vo.addWarning("厚拨片（" + thickness + "mm）适合电吉他速弹和重音色");
                }
            } else if (instrument.equals("guitar-bass")) {
                if (thickness < 1.0) {
                    vo.addWarning("薄拨片（" + thickness + "mm）适合贝斯slap演奏");
                } else {
                    vo.addWarning("厚拨片（" + thickness + "mm）适合贝斯指弹和稳准的音色");
                }
            } else if (instrument.equals("ukulele")) {
                if (thickness > 0.8) {
                    vo.addWarning("尤克里里建议使用较薄的拨片（0.8mm以下），太厚可能损伤琴弦");
                } else {
                    vo.addWarning("拨片厚度（" + thickness + "mm）适合尤克里里");
                }
            }
        }

        String specLower = specification.toLowerCase();
        if (specLower.contains("尼龙") || specLower.contains("nylon")) {
            vo.addWarning("尼龙拨片手感柔和，音色温暖");
        } else if (specLower.contains("赛璐珞") || specLower.contains("celluloid")) {
            vo.addWarning("赛璐珞拨片音色明亮，是最常用的材质");
        } else if (specLower.contains("delrin") || specLower.contains("树脂")) {
            vo.addWarning("树脂拨片耐磨，音色清晰");
        }
    }

    private void checkRosinCompatibility(AccessoryCompatibilityVO vo, String instrument, String specification, String instrumentName) {
        if (!BOWED_INSTRUMENTS.contains(instrument)) {
            vo.addError("松香仅适用于小提琴、二胡等拉弦乐器，" + instrumentName + "不需要使用松香");
            return;
        }

        if (!StringUtils.hasText(specification)) {
            vo.addWarning("请填写松香规格以进行更精确的校验");
            return;
        }

        String specLower = specification.toLowerCase();

        if (instrument.equals("violin")) {
            if (specLower.contains("轻型") || specLower.contains("light")) {
                vo.addWarning("轻型松香适合小提琴在温暖气候下使用");
            } else if (specLower.contains("重型") || specLower.contains("dark")) {
                vo.addWarning("重型松香适合小提琴在寒冷干燥气候下使用");
            }
            if (specLower.contains("无尘") || specLower.contains("low dust")) {
                vo.addWarning("无尘松香更健康，建议使用");
            }
        } else if (instrument.equals("erhu")) {
            if (specLower.contains("小提琴") || specLower.contains("violin")) {
                vo.addWarning("二胡可以使用小提琴松香，但建议使用专用二胡松香");
            }
        }

        if (specLower.contains("吉他") || specLower.contains("guitar")) {
            vo.addError("吉他松香不适用，请使用小提琴/二胡专用松香");
        }
    }

    private void checkCapoCompatibility(AccessoryCompatibilityVO vo, String instrument, String specification, String instrumentName) {
        if (!FRETED_INSTRUMENTS.contains(instrument)) {
            vo.addError("变调夹仅适用于吉他、尤克里里等有品弦乐器，" + instrumentName + "无法使用变调夹");
            return;
        }

        if (!StringUtils.hasText(specification)) {
            vo.addWarning("请填写变调夹规格以进行更精确的校验");
            return;
        }

        String specLower = specification.toLowerCase();

        if (instrument.equals("ukulele")) {
            if (specLower.contains("吉他") || specLower.contains("guitar")) {
                vo.addError("吉他变调夹过大，不适用于尤克里里，请使用尤克里里专用变调夹");
            }
        } else if (GUITAR_FAMILY.contains(instrument)) {
            if (specLower.contains("尤克里里") || specLower.contains("ukulele")) {
                vo.addError("尤克里里变调夹过小，不适用于" + instrumentName);
            }
        }

        if (instrument.equals("guitar-bass")) {
            if (!specLower.contains("贝斯") && !specLower.contains("bass")) {
                vo.addWarning("贝斯指板较宽，建议使用贝斯专用变调夹");
            }
        }

        if (specLower.contains("弹簧") || specLower.contains("spring")) {
            vo.addWarning("弹簧式变调夹使用方便，但力度不可调");
        } else if (specLower.contains("滚动") || specLower.contains("rolling")) {
            vo.addWarning("滚动式变调夹可以快速移动，但可能不如弹簧式稳固");
        } else if (specLower.contains("螺丝") || specLower.contains("screw")) {
            vo.addWarning("螺丝式变调夹力度可调，适合对音色要求高的演奏");
        }
    }

    private void checkBowCompatibility(AccessoryCompatibilityVO vo, String instrument, String specification, String instrumentName) {
        if (!BOWED_INSTRUMENTS.contains(instrument)) {
            vo.addError("琴弓仅适用于小提琴、二胡等拉弦乐器，" + instrumentName + "不使用琴弓");
            return;
        }

        if (!StringUtils.hasText(specification)) {
            vo.addWarning("请填写琴弓规格以进行更精确的校验");
            return;
        }

        if (instrument.equals("violin")) {
            if (specification.contains("4/4") || specification.contains("3/4") || specification.contains("1/2")) {
                vo.addWarning("小提琴尺寸规格已填写，请确认与乐器尺寸一致");
            } else {
                vo.addWarning("建议在规格中注明小提琴尺寸（如4/4、3/4等）");
            }

            String specLower = specification.toLowerCase();
            if (specLower.contains("巴西木") || specLower.contains("brazilwood")) {
                vo.addWarning("巴西木琴弓是入门级的不错选择");
            } else if (specLower.contains("苏木") || specLower.contains("pernambuco")) {
                vo.addWarning("苏木琴弓是专业级选择，弹性和重量俱佳");
            } else if (specLower.contains("碳纤维") || specLower.contains("carbon")) {
                vo.addWarning("碳纤维琴弓耐用，不受温湿度影响，适合练习");
            }

            if (specLower.contains("八角") || specLower.contains("octagonal")) {
                vo.addWarning("八角弓杆握持稳定，适合初学者");
            } else if (specLower.contains("圆形") || specLower.contains("round")) {
                vo.addWarning("圆形弓杆弹性好，适合进阶演奏者");
            }
        } else if (instrument.equals("erhu")) {
            if (specification.contains("小提琴") || specification.contains("violin")) {
                vo.addError("小提琴琴弓不适用于二胡，请使用二胡专用琴弓");
            }
            String specLower = specification.toLowerCase();
            if (specLower.contains("紫竹") || specLower.contains("紫竹杆")) {
                vo.addWarning("紫竹二胡琴弓是传统选择");
            } else if (specLower.contains("碳纤维") || specLower.contains("carbon")) {
                vo.addWarning("碳纤维二胡琴弓耐用稳定");
            }
        }
    }

    private void checkStrapCompatibility(AccessoryCompatibilityVO vo, String instrument, String instrumentName) {
        if (instrument.equals("violin") || instrument.equals("erhu")) {
            vo.addWarning(instrumentName + "通常使用肩托或琴托，而不是背带");
        } else if (instrument.equals("piano")) {
            vo.addError("钢琴不需要使用背带");
        }
    }

    private void checkCleanerCompatibility(AccessoryCompatibilityVO vo, String instrument, String specification, String instrumentName) {
        if (!StringUtils.hasText(specification)) {
            return;
        }

        String specLower = specification.toLowerCase();

        if (specLower.contains("指板油") || specLower.contains("柠檬油")) {
            if (instrument.equals("violin") || instrument.equals("erhu")) {
                vo.addWarning(instrumentName + "指板通常不使用柠檬油，请确认产品说明");
            } else if (instrument.equals("piano")) {
                vo.addError("钢琴不需要使用指板油");
            }
        }

        if (specLower.contains("琴弦油") || specLower.contains("string cleaner")) {
            if (!STRING_INSTRUMENTS.contains(instrument)) {
                vo.addError(instrumentName + "没有琴弦，不需要琴弦油");
            }
        }

        if (specLower.contains("烤漆") || specLower.contains("polish")) {
            if (instrument.equals("piano")) {
                vo.addWarning("钢琴烤漆清洁请使用专用钢琴清洁剂");
            }
        }
    }

    private double extractThickness(String specification) {
        if (!StringUtils.hasText(specification)) {
            return 0;
        }

        Matcher matcher = THICKNESS_PATTERN.matcher(specification);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        String[] commonThicknesses = {"0.46", "0.50", "0.60", "0.71", "0.73", "0.80", "0.88", "0.90", "1.0", "1.14", "1.5", "2.0"};
        for (String thickness : commonThicknesses) {
            if (specification.contains(thickness)) {
                try {
                    return Double.parseDouble(thickness);
                } catch (NumberFormatException ignored) {}
            }
        }

        return 0;
    }
}

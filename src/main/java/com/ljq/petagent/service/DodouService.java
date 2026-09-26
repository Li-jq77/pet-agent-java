package com.ljq.petagent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DodouService {

    private static final String DEFAULT_CITY = "广州";
    private static final double DEFAULT_LAT = 23.1291d;
    private static final double DEFAULT_LON = 113.2644d;
    private static final String SYSTEM_PROMPT =
        "你是宠物门店里的智能体豆豆。全程以“豆豆”的第三人称与用户交流，" +
        "绝不使用“我”自称，提到自己时只说“豆豆”。" +
        "你擅长宠物常见症状的初步居家观察、护理和就医时机建议，" +
        "但你不做疾病诊断，也不替代兽医；遇到危急信号要明确建议尽快就医。" +
        "回答简洁、温柔、口语化，先说观察重点，再给可执行建议。";

    private final RestTemplate http;
    private final String amapKey;
    private final String aiKey;
    private final String aiBaseUrl;
    private final String aiModel;

    public DodouService(
        @Value("${amap.web.api.key:}") String amapKey,
        @Value("${dodou.ai.api.key:}") String aiKey,
        @Value("${dodou.ai.base-url:https://api.openai.com/v1}") String aiBaseUrl,
        @Value("${dodou.ai.model:gpt-4o-mini}") String aiModel
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(20000);
        this.http = new RestTemplate(factory);
        this.amapKey = amapKey == null ? "" : amapKey.trim();
        this.aiKey = aiKey == null ? "" : aiKey.trim();
        this.aiBaseUrl = (aiBaseUrl == null ? "https://api.openai.com/v1" : aiBaseUrl).replaceAll("/+$", "");
        this.aiModel = aiModel == null ? "gpt-4o-mini" : aiModel.trim();
    }

    public boolean isWeatherIntent(String message) {
        String[] keywords = {
            "天气", "下雨", "遛狗", "遛弯", "适合遛", "出去遛", "适合散步",
            "出去散步", "散步天气", "出门遛", "风力", "气温", "预报", "要不要带伞"
        };
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public String extractCity(String message) {
        String clean = message.replace(" ", "")
            .replace("，", ",")
            .replace("。", ",")
            .replace("？", ",")
            .replace("?", ",");
        Pattern pattern = Pattern.compile(
            "(?:帮我查一下|帮我看看|帮我看下|帮我查|查一下|想知道|看看|去|到|在|查|看|^)" +
            "([\\u4e00-\\u9fa5]{2,8}?)(?=今天|明天|后天|现在|天气|气温|适不适合遛狗|适合遛狗|遛狗天气)"
        );
        Pattern placePattern = Pattern.compile("([\\u4e00-\\u9fa5]{2,8}(?:省|市|区|县|州))");
        Matcher matcher = pattern.matcher(clean);
        if (!matcher.find()) {
            matcher = placePattern.matcher(clean);
        }
        if (!matcher.find()) {
            return "";
        }
        String city = matcher.group(1).trim().replaceFirst("^(帮我查|查一下|看看|去|到|在)", "");
        List<String> ignored = Arrays.asList(
            "今天", "明天", "后天", "现在", "昨晚", "刚才", "宠物", "狗狗", "小猫", "猫咪"
        );
        return ignored.contains(city) ? "" : city;
    }

    public Map<String, Object> weatherReply(String city, Double lat, Double lon, String remoteIp) {
        if (!amapKey.isEmpty()) {
            try {
                return amapWeatherReply(city, lat, lon, remoteIp);
            } catch (Exception ignored) {
                // Fall back to Open-Meteo, matching the Django service.
            }
        }
        return openMeteoWeatherReply(city, lat, lon);
    }

    private Map<String, Object> openMeteoWeatherReply(String city, Double lat, Double lon) {
        String label;
        double resolvedLat;
        double resolvedLon;
        try {
            Location location = resolveLocation(city, lat, lon);
            label = location.label;
            resolvedLat = location.lat;
            resolvedLon = location.lon;
        } catch (Exception ex) {
            Map<String, Object> error = new LinkedHashMap<String, Object>();
            error.put("ok", false);
            error.put("text", "豆豆暂时没在天气服务里找到" + city +
                "的位置，可以确认一下城市名，或者直接让豆豆查广州的遛狗天气。");
            return error;
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://api.open-meteo.com/v1/forecast")
                .queryParam("latitude", resolvedLat)
                .queryParam("longitude", resolvedLon)
                .queryParam(
                    "current",
                    "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,is_day"
                )
                .queryParam(
                    "hourly",
                    "temperature_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,apparent_temperature"
                )
                .queryParam("forecast_days", 2)
                .queryParam("timezone", "Asia/Shanghai")
                .build()
                .toUriString();
            Map<String, Object> data = getJson(url, 9000);
            Map<String, Object> current = map(data.get("current"));
            Double temp = number(current.get("temperature_2m"));
            Double feels = number(current.get("apparent_temperature"));
            Double humidity = number(current.get("relative_humidity_2m"));
            Double wind = number(current.get("wind_speed_10m"));
            Double rain = number(current.get("precipitation"));
            int weatherCode = intValue(current.get("weather_code"), 0);
            String[] currentDesc = weatherCode(weatherCode);

            Map<String, Object> hourly = map(data.get("hourly"));
            List<Object> times = list(hourly.get("time"));
            LocalDateTime now = parseDateTime(string(current.get("time")));
            if (now == null) {
                now = LocalDateTime.now();
            }
            LocalDateTime startHour = now.plusHours(1);
            List<Integer> nextScores = new ArrayList<Integer>();
            String nextDesc = "";
            boolean nextPrecip = false;
            boolean nextWindy = false;
            boolean nextHot = false;
            for (int i = 0; i < times.size(); i++) {
                LocalDateTime slot = parseDateTime(string(times.get(i)));
                if (slot == null || slot.isBefore(startHour) || slot.isAfter(now.plusHours(4))) {
                    continue;
                }
                int code = intValue(valueAt(hourly, "weather_code", i), 0);
                Double hTemp = number(valueAt(hourly, "temperature_2m", i));
                Double hWind = number(valueAt(hourly, "wind_speed_10m", i));
                Double hRain = number(valueAt(hourly, "precipitation", i));
                Double hFeels = number(valueAt(hourly, "apparent_temperature", i));
                nextScores.add(weatherScore(code, hTemp, hWind, hRain, hFeels));
                if ((hRain != null && hRain > 0) || isPrecipCode(code)) {
                    nextPrecip = true;
                }
                if (hWind != null && hWind >= 35) {
                    nextWindy = true;
                }
                if ((hTemp != null && hTemp >= 33) || (hFeels != null && hFeels >= 33)) {
                    nextHot = true;
                }
                if (nextDesc.isEmpty()) {
                    nextDesc = weatherCode(code)[0] + weatherCode(code)[1];
                }
            }
            String trend;
            if (!nextScores.isEmpty()) {
                int ok = 0;
                int bad = 0;
                for (Integer score : nextScores) {
                    if (score >= 1) {
                        ok++;
                    }
                    if (score <= -2) {
                        bad++;
                    }
                }
                if (bad >= 2) {
                    if (nextHot) {
                        trend = "接下来 3 小时体感偏热，不太适合长时间遛狗。";
                    } else if (nextWindy) {
                        trend = "接下来 3 小时风力偏大，不太适合长时间遛狗。";
                    } else if (nextPrecip) {
                        trend = "接下来 3 小时有雨雪，不太适合长时间遛狗。";
                    } else {
                        trend = "接下来 3 小时天气条件一般，遛狗建议再等等。";
                    }
                } else if (ok >= 2) {
                    trend = "接下来 3 小时整体平稳，适合安排一次遛狗。";
                } else {
                    trend = "接下来 3 小时天气一般，可以短时遛一遛，随时观察天色。";
                }
            } else {
                trend = "接下来几小时天气变化不大，注意给毛孩子补水和防晒。";
            }

            int score = weatherScore(weatherCode, temp, wind, rain, feels);
            String advice;
            if (score <= -2) {
                advice = "豆豆建议先不要遛狗，改用室内嗅闻或小游戏消耗精力。";
            } else if (score < 2) {
                advice = "豆豆建议缩短散步时间，选在风小一点的时候出门。";
            } else {
                advice = "豆豆觉得现在很适合遛狗，记得牵好绳子、带好水。";
            }

            List<String> lines = new ArrayList<String>();
            lines.add("豆豆查看了" + label + "的遛狗天气：");
            lines.add(currentDesc[0] + " 当前：" + currentDesc[1] +
                "，气温 " + numberText(temp, "℃") +
                "，体感 " + numberText(feels, "℃"));
            List<String> extras = new ArrayList<String>();
            if (humidity != null) {
                extras.add("湿度 " + numberText(humidity, "%"));
            }
            if (wind != null) {
                extras.add("风力 " + numberText(wind, " km/h"));
            }
            if (!extras.isEmpty()) {
                lines.add(String.join("｜", extras));
            }
            if (!nextDesc.isEmpty()) {
                lines.add("接下来：" + nextDesc);
            }
            lines.add(trend);
            lines.add(advice);

            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("ok", true);
            result.put("kind", "weather");
            result.put("text", String.join("\n", lines));
            result.put("city", label);
            result.put("temperature", temp);
            result.put("weather_code", weatherCode);
            return result;
        } catch (Exception ex) {
            Map<String, Object> error = new LinkedHashMap<String, Object>();
            error.put("ok", false);
            error.put("text", "豆豆现在连不上天气服务，可能是网络暂时不稳定。" +
                "稍后再问豆豆一次，或者先按温度加减衣服、避开暴雨时段。");
            return error;
        }
    }

    private Map<String, Object> amapWeatherReply(
        String city,
        Double lat,
        Double lon,
        String remoteIp
    ) {
        Map<String, Object> location = null;
        if (lat != null && lon != null) {
            location = amapRegeo(lat, lon);
        } else if (city != null && !city.trim().isEmpty()) {
            location = amapGeocode(city);
        } else {
            location = amapIpLocation(remoteIp);
        }
        if (location == null) {
            location = amapGeocode(DEFAULT_CITY);
        }
        String adcode = string(location.get("adcode"));
        if (adcode.isEmpty()) {
            throw new IllegalStateException("高德没有返回城市编码");
        }
        Map<String, Object> data = amapRequest(
            "weather/weatherInfo",
            params("city", adcode, "extensions", "all")
        );
        List<Object> forecasts = list(data.get("forecasts"));
        Map<String, Object> forecast = forecasts.isEmpty() ? new HashMap<String, Object>() : map(forecasts.get(0));
        List<Object> casts = list(forecast.get("casts"));
        if (casts.isEmpty()) {
            throw new IllegalStateException("高德没有返回天气预报");
        }
        Map<String, Object> today = map(casts.get(0));
        Map<String, Object> tomorrow = casts.size() > 1 ? map(casts.get(1)) : null;
        String display = string(location.get("display"));
        if (display.isEmpty()) {
            display = string(forecast.get("city"));
        }
        if (display.isEmpty()) {
            display = "当前位置";
        }

        int dayScore = amapWeatherScore(
            string(today.get("dayweather")),
            string(today.get("daytemp")),
            string(today.get("daypower"))
        );
        int nightScore = amapWeatherScore(
            string(today.get("nightweather")),
            string(today.get("nighttemp")),
            string(today.get("nightpower"))
        );
        int hour = LocalDateTime.now().getHour();
        int score = hour >= 6 && hour < 19 ? dayScore : nightScore;
        String advice;
        if (score <= -2) {
            advice = dayScore <= -2 && hour >= 6 && hour < 19
                ? "豆豆建议白天先不要遛狗，高温或天气不适合时改成室内嗅闻或小游戏。"
                : "豆豆建议今天先不要遛狗，改为室内嗅闻或小游戏。";
        } else if (score < 1) {
            advice = "豆豆建议缩短遛狗时间，避开雨雪大风或温差大的时段。";
        } else {
            advice = "豆豆觉得今天适合遛狗，记得避开正午高温并带好水。";
        }

        List<String> lines = new ArrayList<String>();
        lines.add("豆豆查看了" + display + "的高德天气：");
        if (!string(today.get("date")).isEmpty()) {
            lines.add("今日 " + string(today.get("date")));
        }
        lines.add("白天：" + string(today.get("dayweather")) + "，" +
            string(today.get("daytemp")) + "℃，" +
            string(today.get("daywind")) + string(today.get("daypower")) + "级");
        lines.add("夜间：" + string(today.get("nightweather")) + "，" +
            string(today.get("nighttemp")) + "℃，" +
            string(today.get("nightwind")) + string(today.get("nightpower")) + "级");
        if (tomorrow != null) {
            lines.add("明天：白天 " + string(tomorrow.get("dayweather")) + " " +
                string(tomorrow.get("daytemp")) + "℃ / 夜间 " +
                string(tomorrow.get("nightweather")) + " " +
                string(tomorrow.get("nighttemp")) + "℃");
        }
        lines.add(advice);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("ok", true);
        result.put("kind", "weather");
        result.put("text", String.join("\n", lines));
        result.put("city", display);
        result.put("provider", "amap");
        return result;
    }

    public String llmReply(String message, List<Map<String, Object>> history) {
        if (aiKey.isEmpty()) {
            return null;
        }
        try {
            List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
            messages.add(message("system", SYSTEM_PROMPT));
            int start = Math.max(0, history.size() - 8);
            for (Map<String, Object> item : history.subList(start, history.size())) {
                String role = string(item.get("role"));
                String content = string(item.get("content"));
                if (("user".equals(role) || "assistant".equals(role)) && !content.isEmpty()) {
                    messages.add(message(role, content));
                }
            }
            messages.add(message("user", message));

            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("model", aiModel);
            payload.put("messages", messages);
            payload.put("temperature", 0.6d);
            payload.put("max_tokens", 900);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiKey);
            ResponseEntity<Map> response = http.exchange(
                aiBaseUrl + "/chat/completions",
                HttpMethod.POST,
                new HttpEntity<Map<String, Object>>(payload, headers),
                Map.class
            );
            Map<String, Object> data = response.getBody();
            if (data == null) {
                return null;
            }
            List<Object> choices = list(data.get("choices"));
            if (choices.isEmpty()) {
                return null;
            }
            String content = string(map(map(choices.get(0)).get("message")).get("content")).trim();
            return content.isEmpty() ? null : content;
        } catch (Exception ex) {
            return null;
        }
    }

    public String diagnosisReply(String message) {
        String[] critical = {
            "抽搐", "休克", "昏迷", "呼吸困难", "喘不上气", "窒息", "误食", "中毒",
            "大出血", "便血", "拉血", "呕吐不止", "不停呕吐", "站不起来", "被撞", "摔伤"
        };
        if (containsAny(message, critical)) {
            return "豆豆注意到这些可能是需要尽快就医的危急信号：抽搐、呼吸困难、误食中毒、" +
                "持续呕吐、便血或外伤。豆豆建议现在立刻联系附近宠物医院，途中尽量让宠物保持安静、" +
                "不要强行喂水或喂食，并把发病经过、吃过什么和最近接种情况告诉医生。";
        }
        if (containsAny(message, new String[]{"拉肚子", "腹泻", "软便", "便便稀"})) {
            return "先别太紧张，豆豆建议先观察三件事：次数、状态和有没有血。\n" +
                "1. 成年犬猫可以先停食 4-6 小时但保持饮水；幼龄宠物不要长时间禁食，尽快咨询兽医。\n" +
                "2. 可以喂少量易消化的熟鸡胸肉或处方罐头，少量多次。\n" +
                "3. 如果超过 24 小时没好转，或出现便血、反复呕吐、没精神，需要尽快带去医院。\n" +
                "豆豆还想知道，你家毛孩子是狗狗还是猫咪，大概几岁，最近有没有换粮或吃陌生人食物？";
        }
        if (containsAny(message, new String[]{"呕吐", "吐了", "反酸", "吐白沫", "吐黄水"})) {
            return "偶尔吐一次不用太慌张，但豆豆需要你继续观察频率和精神状态。\n" +
                "可以先停食 4-6 小时，让肠胃休息；期间少量饮水。恢复后给一点好消化的食物。\n" +
                "如果 24 小时内呕吐超过 3 次，或伴随没精神、拒食、腹痛、吐出血或异物，" +
                "豆豆建议尽快去医院检查。";
        }
        if (containsAny(message, new String[]{"不吃", "没食欲", "不吃饭", "食欲不好", "没胃口"})) {
            return "没精神加不吃饭通常是身体在发信号，豆豆建议先量一下体温：狗狗正常约 38-39.2℃，" +
                "猫咪约 38-39.5℃。再检查有没有呕吐、拉肚子、呼吸急促或牙龈颜色发白。\n" +
                "如果只是轻微没胃口且精神状态尚可，可以先温水泡软口粮试一下；" +
                "如果持续超过 24 小时不吃不喝、嗜睡或发热，就要尽快就医。";
        }
        if (containsAny(message, new String[]{"咳嗽", "打喷嚏", "流鼻涕", "鼻涕", "干咳"})) {
            return "豆豆帮你梳理一下观察重点：分泌物是清水样还是脓性、有没有发烧、" +
                "精神和食欲是否正常。\n保持环境干净通风，别让宠物吹到冷风，也不要在家随意用药。\n" +
                "如果咳嗽持续两天以上，或出现喘、嘴唇发紫、不吃不喝，请尽快去医院排查呼吸道感染。";
        }
        if (containsAny(message, new String[]{"抓耳朵", "耳朵臭", "甩头", "耳螨", "耳朵红"})) {
            return "频繁抓耳朵、耳朵臭或甩头，常见原因是耳道潮湿、耳螨或外耳炎。\n" +
                "豆豆建议先用宠物洗耳液轻轻清洁外耳，避免用棉签往深处掏；" +
                "如果耳廓内侧红肿、有黑色碎屑或褐色分泌物，最好让医生做个耳道检查并针对性用药。\n" +
                "洗澡和游泳后记得把耳朵擦干。";
        }
        if (containsAny(message, new String[]{"掉毛", "抓痒", "红疹", "皮屑", "真菌", "皮肤病", "一块块"})) {
            return "掉毛、起红疹或皮肤发痒的原因不少，可能是寄生虫、真菌或过敏。\n" +
                "豆豆建议先戴伊丽莎白圈防止抓破，检查体表和耳背有没有跳蚤或虫卵；" +
                "可以温和梳理并拍照记录范围。若出现圆形脱毛、破损流液或范围扩大，" +
                "建议去医院做皮肤刮片，不要自行买人用药膏涂抹。";
        }
        if (containsAny(message, new String[]{"眼睛", "流泪", "眼屎", "红眼", "眯眼"})) {
            return "眼睛问题要认真对待，豆豆建议先观察分泌物是清水还是黄绿色、是否频繁眯眼或抓挠。\n" +
                "可以用宠物专用湿巾或生理盐水从内眼角向外轻轻擦拭，不要让宠物抓眼睛。\n" +
                "如果角膜看着发白、眼睑红肿或分泌物明显增多，请尽快就医，避免拖延损伤视力。";
        }
        if (containsAny(message, new String[]{"瘸", "跛行", "腿疼", "不敢走", "一瘸一拐", "骨折"})) {
            return "出现跛行或不敢着地，先让宠物休息，别继续跑跳或爬楼梯。\n" +
                "豆豆建议轻轻检查脚垫有没有异物或伤口；如果明显肿胀、按压时抗拒、" +
                "关节变形或超过 24 小时未缓解，需要拍 X 光确认。";
        }
        if (containsAny(message, new String[]{"疫苗", "打疫苗", "接种", "狂犬", "猫三联", "犬四联"})) {
            return "疫苗问题最好参考宠物档案里的接种记录。豆豆的一般建议是：\n" +
                "幼犬幼猫按医生计划完成基础免疫，成年后每年定期复查抗体并补打相应疫苗；" +
                "接种前后一周尽量不洗澡、不剧烈运动、不换粮，打完留在医院观察 20-30 分钟。\n" +
                "如果最近准备出门寄养或洗澡，可以先和医生确认疫苗是否在有效保护期内。";
        }
        if (containsAny(message, new String[]{"驱虫", "虫子", "跳蚤", "蜱虫"})) {
            return "预防性驱虫建议按体重和药品说明进行，体内驱虫通常每 1-3 个月一次，" +
                "体外驱虫看环境和外出频率。\n发现跳蚤或蜱虫时不要直接硬拔蜱虫，" +
                "可用专用工具夹住头部拔出并消毒，同时清理家里角落。" +
                "若宠物很小、生病或刚到家，先让医生确认再用药。";
        }
        if (containsAny(message, new String[]{"你好", "您好", "嗨", "hello", "hi", "在吗"})) {
            return "你好呀，豆豆在的。宠物有什么小状况，或者想查遛狗天气，" +
                "都可以直接告诉豆豆：狗狗还是猫咪、什么症状、持续多久了？";
        }
        if (containsAny(message, new String[]{"谢谢", "感谢", "辛苦了"})) {
            return "不客气，豆豆会一直陪在毛孩子身边。照顾好它，有新的情况随时再来找豆豆。";
        }
        return "豆豆收到啦。为了给你更准确的观察建议，可以先说说：\n" +
            "1. 是狗狗还是猫咪，大概几岁、多重？\n" +
            "2. 主要出现什么症状，持续多久了？\n" +
            "3. 吃喝、精神和大小便还正常吗？\n" +
            "也可以直接输入“北京遛狗天气”之类的问题，豆豆帮你查天气。";
    }

    private Location resolveLocation(String city, Double lat, Double lon) {
        if (lat != null && lon != null) {
            return new Location(city == null || city.isEmpty() ? "当前位置" : city, lat, lon);
        }
        if (city != null && !city.trim().isEmpty()) {
            String clean = city.trim()
                .replace("市", "")
                .replace("省", "")
                .replace("区", "")
                .replace("县", "");
            String url = UriComponentsBuilder
                .fromHttpUrl("https://geocoding-api.open-meteo.com/v1/search")
                .queryParam("name", clean)
                .queryParam("count", 1)
                .queryParam("language", "zh")
                .queryParam("format", "json")
                .build()
                .toUriString();
            Map<String, Object> data = getJson(url, 10000);
            List<Object> results = list(data.get("results"));
            if (results.isEmpty()) {
                throw new IllegalStateException("找不到城市");
            }
            Map<String, Object> first = map(results.get(0));
            return new Location(
                string(first.get("name")).isEmpty() ? city : string(first.get("name")),
                number(first.get("latitude")),
                number(first.get("longitude"))
            );
        }
        return new Location(DEFAULT_CITY, DEFAULT_LAT, DEFAULT_LON);
    }

    private Map<String, Object> amapRequest(String path, Map<String, String> params) {
        if (amapKey.isEmpty()) {
            throw new IllegalStateException("AMAP_WEB_API_KEY 未配置");
        }
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl("https://restapi.amap.com/v3/" + path);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }
        builder.queryParam("key", amapKey).queryParam("output", "json");
        Map<String, Object> data = getJson(builder.build().toUriString(), 9000);
        if (!"1".equals(string(data.get("status")))) {
            throw new IllegalStateException(string(data.get("info")));
        }
        return data;
    }

    private Map<String, Object> amapGeocode(String city) {
        Map<String, Object> data = amapRequest("geocode/geo", params("address", city, "city", city));
        List<Object> results = list(data.get("geocodes"));
        if (results.isEmpty()) {
            throw new IllegalStateException("找不到城市");
        }
        Map<String, Object> first = map(results.get(0));
        String location = string(first.get("location"));
        String lon = "";
        String lat = "";
        if (location.contains(",")) {
            String[] parts = location.split(",", 2);
            lon = parts[0];
            lat = parts[1];
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("adcode", string(first.get("adcode")));
        result.put("lat", lat);
        result.put("lon", lon);
        result.put("display", amapCityName(
            string(first.get("province")),
            string(first.get("city")),
            string(first.get("district"))
        ));
        return result;
    }

    private Map<String, Object> amapRegeo(double lat, double lon) {
        Map<String, Object> data = amapRequest(
            "geocode/regeo",
            params("location", lon + "," + lat, "extensions", "base")
        );
        Map<String, Object> regeocode = map(data.get("regeocode"));
        Map<String, Object> component = map(regeocode.get("addressComponent"));
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("adcode", string(component.get("adcode")));
        result.put("display", amapCityName(
            string(component.get("province")),
            string(component.get("city")),
            string(component.get("district"))
        ));
        return result;
    }

    private Map<String, Object> amapIpLocation(String remoteIp) {
        if (remoteIp == null || remoteIp.isEmpty() || isPrivateIp(remoteIp)) {
            return null;
        }
        Map<String, Object> data = amapRequest("ip", params("ip", remoteIp));
        String adcode = string(data.get("adcode"));
        if (adcode.isEmpty()) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("adcode", adcode);
        result.put("display", amapCityName(
            string(data.get("province")),
            string(data.get("city")),
            ""
        ));
        return result;
    }

    private String amapCityName(String province, String city, String district) {
        if (!city.isEmpty()) {
            return city;
        }
        if (!district.isEmpty() && !province.isEmpty() && !district.equals(province)) {
            return province + district;
        }
        return province.isEmpty() ? "当前位置" : province;
    }

    private int amapWeatherScore(String weatherText, String tempText, String windPower) {
        int score = 0;
        if (containsAny(weatherText, new String[]{"雨", "雪", "雷", "冰雹", "雾", "霾", "沙尘"})) {
            score -= 3;
        }
        Matcher matcher = Pattern.compile("(\\d+)").matcher(string(windPower));
        if (matcher.find() && Integer.parseInt(matcher.group(1)) >= 5) {
            score -= 2;
        }
        try {
            double temperature = Double.parseDouble(tempText);
            if (temperature >= 10 && temperature <= 28) {
                score += 2;
            } else if (!(temperature >= 0 && temperature < 10) && !(temperature > 28 && temperature <= 33)) {
                score -= 4;
            }
        } catch (Exception ignored) {
            // Missing temperature does not change the weather score.
        }
        return score;
    }

    private int weatherScore(int code, Double temp, Double wind, Double rain, Double feels) {
        int score = temperatureScore(temp, feels);
        if (code == 0 || code == 1) {
            score += 2;
        } else if (code == 2 || code == 3 || code == 45 || code == 48) {
            score += 1;
        } else if (code >= 71 || isHeavyCode(code)) {
            score -= 3;
        } else if (isMediumRainCode(code)) {
            score -= 2;
        }
        if (wind != null && wind >= 35) {
            score -= 2;
        } else if (wind != null && wind >= 22) {
            score -= 1;
        }
        if (rain != null && rain > 0) {
            score -= 1;
        }
        return score;
    }

    private int temperatureScore(Double temp, Double feels) {
        if (temp == null && feels == null) {
            return 0;
        }
        if (feels != null) {
            if (feels >= 35) {
                return -5;
            }
            if (feels >= 33) {
                return -2;
            }
            if (feels <= -10) {
                return -4;
            }
        }
        double effective = temp == null ? feels : temp;
        if (effective >= 10 && effective <= 28) {
            return 2;
        }
        if ((effective >= 0 && effective < 10) || (effective > 28 && effective <= 33)) {
            return 0;
        }
        return -4;
    }

    private boolean isPrecipCode(int code) {
        int[] codes = {
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77,
            80, 81, 82, 85, 86, 95, 96, 99
        };
        for (int item : codes) {
            if (item == code) {
                return true;
            }
        }
        return false;
    }

    private boolean isHeavyCode(int code) {
        return code == 80 || code == 81 || code == 82 || code == 95 || code == 96 || code == 99;
    }

    private boolean isMediumRainCode(int code) {
        return code == 55 || code == 56 || code == 57 || code == 63 ||
            code == 65 || code == 66 || code == 67;
    }

    private String[] weatherCode(int code) {
        Map<Integer, String[]> table = new HashMap<Integer, String[]>();
        table.put(0, new String[]{"☀️", "晴朗"});
        table.put(1, new String[]{"🌤️", "大致晴朗"});
        table.put(2, new String[]{"⛅", "多云"});
        table.put(3, new String[]{"☁️", "阴天"});
        table.put(45, new String[]{"🌫️", "有雾"});
        table.put(48, new String[]{"🌫️", "雾凇"});
        table.put(51, new String[]{"🌦️", "毛毛雨"});
        table.put(53, new String[]{"🌦️", "小雨"});
        table.put(55, new String[]{"🌧️", "中雨"});
        table.put(56, new String[]{"🌧️", "冻毛毛雨"});
        table.put(57, new String[]{"🌧️", "冻雨"});
        table.put(61, new String[]{"🌧️", "小雨"});
        table.put(63, new String[]{"🌧️", "中雨"});
        table.put(65, new String[]{"🌧️", "大雨"});
        table.put(66, new String[]{"🌧️", "冻雨"});
        table.put(67, new String[]{"🌧️", "强冻雨"});
        table.put(71, new String[]{"🌨️", "小雪"});
        table.put(73, new String[]{"🌨️", "中雪"});
        table.put(75, new String[]{"❄️", "大雪"});
        table.put(77, new String[]{"❄️", "雪粒"});
        table.put(80, new String[]{"🌦️", "阵雨"});
        table.put(81, new String[]{"🌧️", "强阵雨"});
        table.put(82, new String[]{"⛈️", "暴雨"});
        table.put(85, new String[]{"🌨️", "阵雪"});
        table.put(86, new String[]{"❄️", "强阵雪"});
        table.put(95, new String[]{"⛈️", "雷雨"});
        table.put(96, new String[]{"⛈️", "雷雨伴冰雹"});
        table.put(99, new String[]{"⛈️", "强雷雨"});
        String[] value = table.get(code);
        return value == null ? new String[]{"🌡️", "天气变化"} : value;
    }

    private Map<String, Object> getJson(String url, int timeoutMillis) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "PetAgentDodou/1.0");
        ResponseEntity<Map> response = http.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<Object>(headers),
            Map.class
        );
        return response.getBody() == null
            ? Collections.<String, Object>emptyMap()
            : response.getBody();
    }

    private boolean isPrivateIp(String ip) {
        if (Arrays.asList("localhost", "::1", "0.0.0.0", "127.0.0.1").contains(ip.toLowerCase())) {
            return true;
        }
        try {
            return InetAddress.getByName(ip).isSiteLocalAddress()
                || InetAddress.getByName(ip).isLoopbackAddress()
                || InetAddress.getByName(ip).isAnyLocalAddress();
        } catch (Exception ex) {
            return true;
        }
    }

    private Map<String, String> params(String... values) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            params.put(values[i], values[i + 1]);
        }
        return params;
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<String, Object>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private Object valueAt(Map<String, Object> source, String key, int index) {
        List<Object> values = list(source.get(key));
        return index < values.size() ? values.get(index) : null;
    }

    private boolean containsAny(String text, String[] values) {
        for (String value : values) {
            if (text != null && text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String numberText(Double value, String suffix) {
        if (value == null) {
            return "";
        }
        if (value == Math.rint(value)) {
            return String.format("%.0f%s", value, suffix);
        }
        return value + suffix;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new HashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value) {
        return value instanceof List ? (List<Object>) value : new ArrayList<Object>();
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Double number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value == null ? null : Double.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static class Location {
        private final String label;
        private final double lat;
        private final double lon;

        Location(String label, double lat, double lon) {
            this.label = label;
            this.lat = lat;
            this.lon = lon;
        }
    }
}

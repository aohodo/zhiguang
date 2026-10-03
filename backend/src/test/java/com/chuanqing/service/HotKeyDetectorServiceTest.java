package com.chuanqing.service;

import com.chuanqing.config.CachePropertiesConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HotKeyDetectorServiceTest {
    @Test
    void slidingWindowLevelsAndTtl() {
        CachePropertiesConfig props = new CachePropertiesConfig();
        props.getHotkey().setWindowSeconds(30);
        props.getHotkey().setSegmentSeconds(10);
        props.getHotkey().setLevelLow(2);
        props.getHotkey().setLevelMedium(4);
        props.getHotkey().setLevelHigh(6);
        props.getHotkey().setExtendLowSeconds(10);
        props.getHotkey().setExtendMediumSeconds(20);
        props.getHotkey().setExtendHighSeconds(30);

        HotKeyDetectorService detector = new HotKeyDetectorService(props);

        String key = "k";
        detector.record(key);
        detector.record(key);
        Assertions.assertEquals(2, detector.heat(key));
        Assertions.assertEquals(HotKeyDetectorService.Level.LOW, detector.level(key));
        Assertions.assertEquals(70, detector.ttlForPublic(60, key));

        detector.record(key);
        detector.record(key);
        Assertions.assertEquals(4, detector.heat(key));
        Assertions.assertEquals(HotKeyDetectorService.Level.MEDIUM, detector.level(key));
        Assertions.assertEquals(80, detector.ttlForPublic(60, key));

        detector.record(key);
        detector.record(key);
        Assertions.assertEquals(6, detector.heat(key));
        Assertions.assertEquals(HotKeyDetectorService.Level.HIGH, detector.level(key));
        Assertions.assertEquals(90, detector.ttlForPublic(60, key));

        detector.rotate();
        detector.rotate();
        detector.rotate();
        Assertions.assertTrue(detector.heat(key) >= 0);
    }
}


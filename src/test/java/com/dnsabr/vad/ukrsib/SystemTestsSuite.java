package com.dnsabr.vad.ukrsib;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Запускает все системные тесты, кроме работающих слишком долго - LongTests.class
 */
@RunWith(JUnitPlatform.class)
@SpringBootTest
@SelectPackages(value = {"com.dnsabr.vad.ukrsib.systemtests"})
public class SystemTestsSuite {}

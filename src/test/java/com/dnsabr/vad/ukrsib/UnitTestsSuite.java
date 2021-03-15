package com.dnsabr.vad.ukrsib;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.runner.RunWith;

/**
 * Запускает все Unit-тесты
 */
@RunWith(JUnitPlatform.class)
@SelectPackages(value = {"com.dnsabr.vad.ukrsib"})
@ExcludeClassNamePatterns(value = {"^.*LongTests?$","^.*SystemTests?$","^.*ServiceTests?$","^.*EtcTests?$"})
public class UnitTestsSuite {}

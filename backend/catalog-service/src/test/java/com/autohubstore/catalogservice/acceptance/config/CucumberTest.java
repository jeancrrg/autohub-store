package com.autohubstore.catalogservice.acceptance.config;

import io.cucumber.junit.platform.engine.Constants;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.autohubstore.catalogservice.acceptance")
public class CucumberTest {

}

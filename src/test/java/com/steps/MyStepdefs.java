package com.steps;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.junit.Assert;

public class MyStepdefs {
    @When("^ssss$")
    public void ssss() throws Throwable {
        Assert.assertTrue(1==1);
    }

    @Then("^wwww$")
    public void wwww() throws Throwable {
    }
}


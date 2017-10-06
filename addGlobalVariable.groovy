import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*

def addVariable(String key, String value, Map envVars){
    String oldValue = envVars.get(key);
    if (oldValue != null) {
        println "Global variable exists: " + key + " -> " + oldValue;
    }
    else {
        envVars.put(key, value)
        println "Add global variable: " + key + " -> " + value;
    }
}


RV = NEW_RELEASE_VERSION  //"R9_7" //release_version
BRANCH = RELEASE_BRANCH   //"master"
VM = CI_BOX				  //"ci_ltxl1339"

nodes = Jenkins.getInstance().getGlobalNodeProperties()
nodes.getAll(hudson.slaves.EnvironmentVariablesNodeProperty.class)

if ( nodes.size() != 1 ) {
    println("error: unexpected number of environment variable containers: " + nodes.size() + " expected: 1")
} else {
    envVars = nodes.get(0).getEnvVars();

    addVariable("ALL_TESTS_SUITES_" + RV, "continuous_integration/all_tests/" + RV, envVars)
    addVariable("BRANCH_" + RV, BRANCH, envVars)
    addVariable("FEATURES_" + RV, "features/" + RV, envVars)
    addVariable("MAX_NUMBER_OF_KEEP_LOGS_" + RV, "20", envVars)
    addVariable("MVN_REPOSITORY_" + RV, "/opt/maven/ci_repositories/" + RV, envVars)
    addVariable("PROXY_ENVIRONMENT_" + RV, "ltxl0969_" + RV, envVars)
    addVariable("RECORDED_TESTS_SUITES_" + RV, "continuous_integration/successfully_replayed/" + RV, envVars)

    addVariable("SE_ENVIRONMENT_CERT_" + RV, VM + "_1", envVars);
    addVariable("SE_ENVIRONMENT_LCS_" + RV, VM + "_3_lcs", envVars);
    addVariable("SE_ENVIRONMENT_TSTS_" + RV, VM + "_2", envVars);
    addVariable("SE_ENVIRONMENT_NDC_" + RV, VM + "_s1c", envVars);

    addVariable("SE_ENVIRONMENT_" + RV + "_INST_2", VM + "_2", envVars);
    addVariable("SE_ENVIRONMENT_" + RV + "_INST_4", VM + "_4_lcs", envVars);
    addVariable("SE_ENVIRONMENT_" + RV + "_INST_5", VM + "_5_lcs", envVars);


    addVariable("TESTLOG_KEEPOLDLOGS_" + RV, "false", envVars);


    Jenkins.getInstance().save();
}


println "OK";

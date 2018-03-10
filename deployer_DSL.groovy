//Method declaration
def urlIsOK(String url) {
    out.print "Check URL: "+url
    def content
    try {
        content = url.toURL().openConnection().with { conn ->
            readTimeout = 10000
            if( responseCode != 200 ) {
                throw new Exception( 'Not Ok' )
            }
        }
    }
    catch( e ) {
        out.println " FAIL!"
        return false;
    }
    out.println " OK!"
    return true;
}

def invokeTest() {
    def testURL = "http://server.com:8080/job/CI_DC_Jobs_Invoker_R3_5_0/buildWithParameters?delay=5sec"
    testURL.toURL().text
}

def getSEContractVersion() {
    def warFile = "/project/whitelabel2/puppet_shared_files/app_builds/dc/SSW2010-"+params["DC_BRANCH"]+"-"+params["DC_BUILD_NUMBER"]+".war"
    def command = "/data/se/scripts/simple-bash-deployer/get_contract_version_from_dc_war.sh "+warFile;
    def proc = command.execute()
    proc.waitFor()

    return proc.in.text.trim()
}

def getSEBuildNumber(String seContractVersion) {
    def command = "/data/se/scripts/simple-bash-deployer/get_se_build_number_from_SEForSSW.py main "+seContractVersion;
    def proc = command.execute()
    proc.waitFor()

    return proc.in.text.trim()
}

server = "server"
out.println 'Deply DC application'

def dcBuildNumber = params["DC_BUILD_NUMBER"];
if ("lastStable".equals(dcBuildNumber)) {
    def authString  = "guest:".getBytes().encodeBase64().toString()
    def tcAddr = "http://teamcity.server/httpAuth/app/rest/builds?locator=buildType:(id:AsDc_DigitalConnect_Trunk),status:SUCCESS,count:1&fields=build(number)"
    URLConnection connection = new URL(tcAddr).openConnection();
    connection.setRequestProperty("Authorization", "Basic ${authString}")
    connection.setRequestProperty("Accept", "application/json")

    def builds = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(connection.getInputStream())));

    dcBuildNumber = builds.build[0].number
}

out.println "DC Build Number: " + dcBuildNumber

// Deploy DC ...
parallel (
        {build("deploy-any-DC-to-any-Box",
                TARGET_BRANCH: params["DC_BRANCH"], TARGET_BUILD_NUMBER: dcBuildNumber,
                APP_SERVER_HOSTNAME: server, TOMCAT_INSTANCE: "1",
                APPLICATIONS: "SSW2010")},
        {build("deploy-any-DC-to-any-Box",
                TARGET_BRANCH: params["DC_BRANCH"], TARGET_BUILD_NUMBER: dcBuildNumber,
                APP_SERVER_HOSTNAME: server, TOMCAT_INSTANCE: "3",
                APPLICATIONS: "SSW2010")}
)

def seBuildNumber = params["SE_BUILD_NUMBER"];

if ("true".equals(params["GET_AUTOMATICALLY_SE_VERSION"])) {
    def seContractVersion = getSEContractVersion()
    out.println "Contract Version: " +seContractVersion
    seBuildNumber = getSEBuildNumber(seContractVersion)
}
out.println "SE Build Number: "+seBuildNumber
out.println 'Deply SE application'
// Deplay SE ...

parallel (
        {build("deploy-any-SE-to-any-Box-Tomcat-8", TARGET_BUILD_ARCHIVE_DIRECTORY: params["SE_BUILDER_JOB"], TARGET_BUILD_NUMBER: seBuildNumber,
                APP_SERVER_HOSTNAME: server, TOMCAT_INSTANCE: "5", APPLICATIONS: "se.server")},
        {build("deploy-any-SE-to-any-Box-Tomcat-8", TARGET_BUILD_ARCHIVE_DIRECTORY: params["SE_BUILDER_JOB"], TARGET_BUILD_NUMBER: seBuildNumber,
                APP_SERVER_HOSTNAME: server, TOMCAT_INSTANCE: "6", APPLICATIONS: "se.server")},

        {build("deploy-any-SE-to-any-Box-Tomcat-8", TARGET_BUILD_ARCHIVE_DIRECTORY: params["SE_BUILDER_JOB"], TARGET_BUILD_NUMBER: seBuildNumber,
                APP_SERVER_HOSTNAME: server, TOMCAT_INSTANCE: "7", APPLICATIONS: "se.lcs.server")},
        {build("deploy-any-SE-to-any-Box-Tomcat-8", TARGET_BUILD_ARCHIVE_DIRECTORY: params["SE_BUILDER_JOB"], TARGET_BUILD_NUMBER: seBuildNumber,
                APP_SERVER_HOSTNAME: server, TOMCAT_INSTANCE: "8", APPLICATIONS: "se.lcs.server")}
)

parallel (
        {build("deploy-any-SE-to-any-Box-Tomcat-8", TARGET_BUILD_ARCHIVE_DIRECTORY: params["ANCILLARIES_BUILDER_JOB"], TARGET_BUILD_NUMBER: params["ANCILLARIES_BUILD_NUMBER"],
                APP_SERVER_HOSTNAME: server, TOMCAT_INSTANCE: "5", APPLICATIONS: "se.ancillaries.server")},
        {build("deploy-any-SE-to-any-Box-Tomcat-8", TARGET_BUILD_ARCHIVE_DIRECTORY: params["ANCILLARIES_BUILDER_JOB"], TARGET_BUILD_NUMBER: params["ANCILLARIES_BUILD_NUMBER"],
                APP_SERVER_HOSTNAME: server, TOMCAT_INSTANCE: "6", APPLICATIONS: "se.ancillaries.server")}
)

out.println 'Waiting to deploy DC/SE application'
seUrlInst1="http://"+server+":8082/SSW2010/api/v3.4/configuration/buildInfo?jipcc=VAVA"
seUrlInst3="http://"+server+":8084/SSW2010/api/v3.4/configuration/buildInfo?jipcc=VAVA"
seUrlInst5="http://"+server+":8085/se.server/jsp/serverInfo.jsp"
seUrlInst6="http://"+server+":8086/se.server/jsp/serverInfo.jsp"
seUrlInst7="http://"+server+":8087/se.lcs.server/jsp/serverInfo.jsp"
seUrlInst8="http://"+server+":8088/se.lcs.server/jsp/serverInfo.jsp"

def isFail = true;
for (i in 0..9) {
    def isOKInst1 = urlIsOK(seUrlInst1);
    def isOKInst3 = urlIsOK(seUrlInst3);

    def isOKInst5 = urlIsOK(seUrlInst5);
    def isOKInst6 = urlIsOK(seUrlInst6);
    def isOKInst7 = urlIsOK(seUrlInst7);
    def isOKInst8 = urlIsOK(seUrlInst8);
    if (isOKInst1 && isOKInst3 && isOKInst5 && isOKInst6 && isOKInst7 && isOKInst8) {
        if ("false".equals(params["ONLY_DEPLOY"])) {
            out.println 'All application are ok, triggering test'
            invokeTest();
        }
        isFail = false;
        break;
    }
    sleep(60000)
}

if (isFail) {
    out.println 'Error during DC/SE deployment. Build with CI tests has not been run'
    build.state.result = FAILURE
}

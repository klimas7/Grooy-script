import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.tasks.*
import groovy.transform.Field


// Input parameters ==========
//@Field String RV = "R9_7"

RV = RELEASE_VERSION; //Thread.currentThread()?.executable.buildVariableResolver.resolve("RELEASE_VERSION")     //"R9_7" //Release version
workspace = Thread.currentThread()?.executable.getWorkspace()

def jenkins = hudson.model.Hudson.instance
jobsToGetLog = jenkins.getItems(hudson.maven.MavenModuleSet.class)
        .findAll{(it.getName().endsWith(RV))}
        .findAll{(!it.getName().toLowerCase().contains("smoke"))}
        .findAll{(!it.getName().toLowerCase().contains("warm"))}
        .findAll{(it.getSomeWorkspace() != null)}

destinationDir = workspace.child(RV);
if (destinationDir.exists()) {
    destinationDir.deleteRecursive();
}

destinationDir.mkdirs()


for (job in  jobsToGetLog) {

    logDir = job.getSomeWorkspace().child("logs");
    jobName = job.getName()
    if (logDir.exists()) {
        logDir.list().get(0).zip(new FilePath(new File(destinationDir.child(jobName + ".zip").toURI())))
        println jobName + " " +logDir.list().get(0)
    }

}
masterZip = new FilePath(new File(workspace.child(RV + ".zip").toURI()))
destinationDir.zip(masterZip)

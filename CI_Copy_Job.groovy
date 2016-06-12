import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.tasks.*
import groovy.transform.Field
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.*
import org.jenkins_ci.plugins.run_condition.*
import hudson.plugins.parameterizedtrigger.*
import hudson.plugins.git.*

// Input parameters ==========
@Field String RV = "R9_7" //Release version
@Field String oldRV = "R9_6" //Old release version

def replaceVersion(String toReplace){
    rvDot = RV.replace("_",".")
    oldRvDot = oldRV.replace("_",".")
    replaced = toReplace.replaceAll(oldRV.toLowerCase(), RV.toLowerCase())
    replaced = replaced.replaceAll(oldRV, RV)
    replaced = replaced.replaceAll(oldRvDot,rvDot)
    return replaced;
}

def jenkins = hudson.model.Hudson.instance
jobsToCopy = jenkins.getItems(hudson.maven.MavenModuleSet.class).findAll{(it.getName().endsWith(oldRV))}

for (job in jobsToCopy) {
    oldJobName = job.getName()
    newJobName = replaceVersion(oldJobName)
    oldJob = jenkins.getItem(oldJobName)
    if (oldJob == null) {
        println("Job " + oldJobName + " not exists!")
        break;
    }
    newJob = jenkins.getItem(newJobName)
    if (newJob == null) {
        newJob = jenkins.copy(oldJob,newJobName)
        newJob.save()
    }
    else {
        println("New job " + newJobName + " exists")
    }
    scm = newJob.getScm()
    newBranchname = replaceVersion(scm.getBranches().get(0).getName())
    newScm = new GitSCM(scm.getUserRemoteConfigs(),Collections.singletonList(new BranchSpec(newBranchname)),false,Collections.<SubmoduleConfig>emptyList(), null, null, null)
    newJob.setScm(newScm);

    newJob.setGoals(replaceVersion(newJob.getGoals()))

    //Change pre Builders
    newPreBuilders = new ArrayList()
    removePreBuilders = new ArrayList()

    List<Builder> preBuilders = newJob.getPrebuilders()
    for (builder in preBuilders){
        if (builder instanceof hudson.tasks.Shell) {
            newCommand = replaceVersion(builder.getCommand())
            newPreBuilders.add new hudson.tasks.Shell(newCommand)
            removePreBuilders.add builder
        }
        else if (builder instanceof org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder){
            newCondition = null
            if (builder.getCondition() instanceof org.jenkins_ci.plugins.run_condition.core.BooleanCondition) {
                newToken = replaceVersion(builder.getCondition().getToken())
                newCondition = new org.jenkins_ci.plugins.run_condition.core.BooleanCondition(newToken)
            }
            else if (builder.getCondition() instanceof org.jenkins_ci.plugins.run_condition.logic.Not) {
                newToken = replaceVersion(builder.getCondition().getCondition().getToken())
                booleanCondition = new org.jenkins_ci.plugins.run_condition.core.BooleanCondition(newToken)
                newCondition = new org.jenkins_ci.plugins.run_condition.logic.Not(booleanCondition)
            }


            buildStep = builder.getBuildStep()
            config = buildStep.getConfigs().get(0)
            newProjectName = replaceVersion(config.getProjects());

            newConfig = new BlockableBuildTriggerConfig(newProjectName, config.getBlock(), config.getConfigFactories(), config.getConfigs())
            newBuildStep = new TriggerBuilder(newConfig)
            buildStepRunner = builder.getRunner()
            newSingleConditionalBuilder = new SingleConditionalBuilder(newBuildStep,newBooleanCondition, buildStepRunner)

            newPreBuilders.add newSingleConditionalBuilder
            removePreBuilders.add builder
        }

    }

    newPreBuilders.each { builder -> preBuilders.add(builder) }
    removePreBuilders.each { builder -> preBuilders.remove(builder)}
    newJob.save()
}

return "OK"
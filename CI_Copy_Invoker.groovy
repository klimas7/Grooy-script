import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.tasks.*
import groovy.transform.Field
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.*
import org.jenkins_ci.plugins.run_condition.*
import hudson.plugins.parameterizedtrigger.*


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

def renameParametersName(hudson.model.ParametersDefinitionProperty parametersDefinition){
    List<ParameterDefinition> copyParameters = new ArrayList();
    List<ParameterDefinition> parameters = parametersDefinition.getParameterDefinitions()
    for (parameter in parameters){
        def parametername = parameter.getName()
        if (parametername.contains(oldRV.toLowerCase()) || parametername.contains(oldRV)){
            copyParameters.add(parameter);
        }
        else {
            println("Parameter: " + parametername + " is OK");
        }
    }
    //Copy and remove
    for (parameter in copyParameters){
        def parametername = parameter.getName()
        def parameterType = parameter.getType()
        def newParameterName = replaceVersion(parametername)
        println("Copy parameter: " + parametername + " -> " + newParameterName + " (Type: " + parameterType + ")")
        switch (parameterType){
            case "BooleanParameterDefinition":
                ParameterDefinition newParameter = new BooleanParameterDefinition(newParameterName, parameter.getDefaultParameterValue().getValue(),"")
                parameters.add(newParameter)
                break;
        }
        parameters.remove(parameter)
    }
}


def jenkins = hudson.model.Hudson.instance
def oldInvoker = jenkins.getItem("CI_Jobs_Invoker_" + oldRV)
if (oldInvoker == null) {
    println("Invoker " + "CI_Jobs_Invoker_" + oldRV + " not exists!")
    return;
}
def newInvoker = jenkins.getItem("CI_Jobs_Invoker_" + RV)
if (newInvoker == null) {
    newInvoker = jenkins.copy(oldInvoker,"CI_Jobs_Invoker_" + RV)
    newInvoker.save()
}
else {
    println("New invoker CI_Jobs_Invoker_" + RV + " exists")
}

//Change properties
def props = newInvoker.getProperties()

for (prop in props){
    if (prop.value instanceof hudson.model.ParametersDefinitionProperty) {
        renameParametersName(prop.value)
    }
}
newInvoker.save()

//Change builders
newBuilders = new ArrayList()
removeBuilders = new ArrayList()

List<Builder> builders = newInvoker.getBuildersList()
for (builder in builders){
    if (builder instanceof hudson.tasks.Shell) {
        newCommand = replaceVersion(builder.getCommand())
        newBuilders.add new hudson.tasks.Shell(newCommand)
        removeBuilders.add builder
    }
    else if (builder instanceof org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder){
        newToken = replaceVersion(builder.getCondition().getToken())
        newBooleanCondition = new org.jenkins_ci.plugins.run_condition.core.BooleanCondition(newToken)

        buildStep = builder.getBuildStep()
        config = buildStep.getConfigs().get(0)
        newProjectName = replaceVersion(config.getProjects());

        newConfig = new BlockableBuildTriggerConfig(newProjectName, config.getBlock(), config.getConfigFactories(), config.getConfigs())
        newBuildStep = new TriggerBuilder(newConfig)
        buildStepRunner = builder.getRunner()
        newSingleConditionalBuilder = new SingleConditionalBuilder(newBuildStep,newBooleanCondition, buildStepRunner)

        newBuilders.add newSingleConditionalBuilder
        removeBuilders.add builder
    }
}

newBuilders.each { builder -> builders.add(builder) }
removeBuilders.each { builder -> builders.remove(builder)}

newInvoker.save()
ciInvokersView = jenkins.getView("CI - Jobs Invokers")
ciInvokersView.add(newInvoker)
ciInvokersView.save();
//Add invokers to view dodaj opis CI - Jobs Invokers
//newInvoker
return "OK"

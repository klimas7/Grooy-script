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

oldRvDot = oldRV.replace("_",".")

def replaceVersion(String toReplace){
    rvDot = RV.replace("_",".")
    oldRvDot = oldRV.replace("_",".")
    replaced = toReplace.replaceAll(oldRV.toLowerCase(), RV.toLowerCase())
    replaced = replaced.replaceAll(oldRV, RV)
    replaced = replaced.replaceAll(oldRvDot,rvDot)
    return replaced;
}

def jenkins = hudson.model.Hudson.instance
allViews = jenkins.getViews()
oldViews = allViews.findAll{(it.getViewName().contains(oldRvDot))}
if (oldViews.size() != 1) {
    return;
}

oldView = oldViews.get(0)
if (oldView instanceof hudson.plugins.nested_view.NestedView) {
    newViewName = replaceVersion(oldView.getViewName())
    newView = null
    //check view exists
    newViews = allViews.findAll{(it.getViewName().contains(newViewName))}
    if (newViews.size() == 1){
        newView = newViews.get(0)
        println "View " + newViewName + " exists!"
    }
    else {
        newView = new hudson.plugins.nested_view.NestedView(newViewName)
        jenkins.addView(newView)
        jenkins.save()
        println "Create new view " + newViewName
    }

    for (view in oldView.getViews()){
        newViewName = replaceVersion(view.getViewName())

        if (view instanceof com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView){
            buildMonitorNew = new com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView(newViewName, replaceVersion(view.getTitle()))
            buildMonitorNew.owner = newView
            items = view.getItems()
            items.each {it -> buildMonitorNew.add(Jenkins.getInstance().getItemByFullName(replaceVersion(it.getName())))}
            newView.addView(buildMonitorNew)
        }
        else if (view instanceof hudson.model.ListView){
            listView = new hudson.model.ListView(newViewName, newView)
            items = view.getItems()
            items.each {it -> listView.add(Jenkins.getInstance().getItemByFullName(replaceVersion(it.getName())))}
            newView.addView(listView)
        }
        println view.getViewName() +" -> " + newViewName
    }
    newView.save()
}
return "OK"
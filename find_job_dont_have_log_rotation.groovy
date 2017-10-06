import hudson.model.*
def jobs = Hudson.instance.items

jobs.findAll{ !it.logRotator}.each {
    println it.name;
}

return "OK"

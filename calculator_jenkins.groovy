def calculate(int first, int second, String operation, String numeral_system) {
    def answer = 0;
    switch (operation) {
        case "+":
            answer = first + second;
            break;
        case "-":
            answer = first - second;
            break;
        case "*":
            answer = first * second;
            break;
        case "/":
            answer = (int)(first / second); //simplification!
            break;
    }
    def sAnswer
    switch (numeral_system) {
        case "hex":
            sAnswer = Integer.toHexString(answer);
            break;
        case "oct":
            sAnswer = Integer.toOctalString(answer);
            break;
        case "bin":
            sAnswer = Integer.toBinaryString(answer);
            break;
        default:
            sAnswer = Integer.toString(answer);
    }
    return sAnswer;
}

//All configuration map global and build variable
def config = new HashMap()
def thr = Thread.currentThread()
def build = thr?.executable

//Get build variable
def buildMap = build.getBuildVariables()
//Add build variable to global configuration map
config.putAll(buildMap)

//Get global environments
def envVarsMap = build.parent.builds[0].properties.get("envVars")
//Add global environments to global configuration map
config.putAll(envVarsMap)

//Build variable you can get using only name!

int first = config.get("first").toInteger();
int second = config.get("second").toInteger();
def operation = config.get("operation");
def numeral_system = config.get("numeral_system")

def answer = 'nan';

if (operation != "/" || second != 0) {
    answer = calculate(first, second, operation, numeral_system);
}

println first + " " + operation + " " + second + " = " + answer;

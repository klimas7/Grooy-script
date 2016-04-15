def config = new HashMap()
def thr = Thread.currentThread()
def build = thr?.executable
def buildMap = build.getBuildVariables()
config.putAll(buildMap)

def envVarsMap = build.parent.builds[0].properties.get("envVars")
config.putAll(envVarsMap)
def numeral_system = config.get("numeral_system")

int first=a.toInteger();
int second=b.toInteger();

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
     if (second == 0)
     {
       answer = 'nan'
     }
     else
     {
       answer = first / second;
     }
    break;
}

def sAnswer;

switch (numeral_system)
{
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

 

println first + " " + operation + " " + second + " = " + sAnswer

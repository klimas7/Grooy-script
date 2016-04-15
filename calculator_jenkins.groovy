def config = new HashMap()
def thr = Thread.currentThread()
def build = thr?.executable
def buildMap = build.getBuildVariables()
config.putAll(buildMap)

def envVarsMap = build.parent.builds[0].properties.get("envVars")
config.putAll(envVarsMap)
def numeral_system = config.get("numeral_system")

int first=a.toInteger();
int secand=b.toInteger();

def ansver = 0;

switch (operation) {
    case "+":
     ansver = first + secand;
    break;
    case "-":
     ansver = first - secand;
    break;
    case "*":
     ansver = first * secand;
    break;
    case "/":
     if (secand == 0)
     {
       ansver = 'nan'
     }
     else
     {
       ansver = first / secand;
     }
    break;
}

def sAnsver;

switch (numeral_system)
{
  case "hex":
   sAnsver = Integer.toHexString(ansver);
  break;
  case "oct":
   sAnsver = Integer.toOctalString(ansver);
  break;
  case "bin":
   sAnsver = Integer.toBinaryString(ansver);
  break;
  default:
    sAnsver = Integer.toString(ansver);
}

 

println first + " " + operation + " " + secand + " = " + sAnsver

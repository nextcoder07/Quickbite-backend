# QuickBite Maven Wrapper for PowerShell
$mavenCmd = "C:\Users\creat\tools\apache-maven-3.9.9\bin\mvn.cmd"
if (Test-Path $mavenCmd) {
    & $mavenCmd @args
} else {
    mvn @args
}

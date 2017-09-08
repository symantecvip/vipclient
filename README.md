<h1>HOW TO BUILD VIP WSDL STUBS</h1>

<h2>Output</h2>

1. Java stubs from VIP WSDLS
2. Jar files for VIP User Services (Authenticatino, Management, and Query). VIP Serices(Authentication and Provisioning)

<h2>Dependencies</h2>

1. JDK 1.8
2. Setup JAVA_HOME environment variable

<h2>Steps</h2>

1. Change directory into the vipbuild folder
2. Run ./gradlew vipservices-auth-ws to generate the vipservices auth jar file
4. Run ./gradlew vipuserservices-ws to generate jar file with all vipuserservices apis (auth, query, management)

Note: User gradlew.bat and appropriate format on Windows platform
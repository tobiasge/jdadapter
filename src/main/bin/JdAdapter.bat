@echo off

REM ############################ S E T T I N G S ###################################
set username=username
set password=password
set synoUri=http://192.168.178.30:5000
REM ################################################################################

java -DsynoUri="%synoUri%" -Dpassword="%password%" -Dusername="%username%" -jar winstone-boot-1.7.0.jar --httpPort=9666 --warfile=JdAdapter.war
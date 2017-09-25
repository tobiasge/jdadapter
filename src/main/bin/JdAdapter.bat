@echo off

REM ############################ S E T T I N G S ###################################
set username=username
set password=password
set synoUri=https://diskstation-hostname:5001/
REM ################################################################################

java -DsynoUri="%synoUri%" -Dpassword="%password%" -Dusername="%username%" -jar winstone.jar --httpPort=9666 --warfile=JdAdapter.war
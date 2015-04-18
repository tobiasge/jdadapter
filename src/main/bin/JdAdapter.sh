#!/bin/sh

############################### S E T T I N G S ###############################
username="username"
password="password"
synoUri="http://192.168.178.30:5000"
###############################################################################

java -DsynoUri="$synoUri" -Dpassword="$password" -Dusername="$username" -jar winstone-boot-1.7.0.jar --httpPort=9666 --warfile=JdAdapter.war
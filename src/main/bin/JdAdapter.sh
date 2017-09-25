#!/bin/sh

############################### S E T T I N G S ###############################
username="username"
password="password"
synoUri="https://diskstation-hostname:5001/"
###############################################################################

java -DsynoUri="$synoUri" -Dpassword="$password" -Dusername="$username" -jar winstone.jar --httpPort=9666 --warfile=JdAdapter.war
#!/bin/bash
export currentPath="$(cd "$(dirname "$0")" && pwd)"
cd ..
cd ..
export pluginBasePath=`pwd`

export appName="xxx-server"
export appPath="$pluginBasePath/$appName/target/$appName.jar"
export deployPluginPath="$pluginBasePath/deploy/deploy.jar"

cd "$currentPath"
java -jar $deployPluginPath -s $appPath -h "47.112.167.xxx" -u "root" -p "xxx" -d "/root/$appName.jar" -c "cd /root && sh $appName.sh restart"
set currentPath=%cd%
cd ..
cd ..

set appName=xxx-server
set appPath="%cd%\%appName%\target\%appName%.jar"
set deployPluginPath="%cd%\deploy\deploy.jar"

cd %currentPath%
java -jar %deployPluginPath% -s %appPath% -h "47.112.167.xxx" -u "root" -p "xxx" -d "/root/%appName%.jar" -c "cd /root && sh %appName%-test.sh restart"
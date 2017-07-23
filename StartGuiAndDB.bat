cd ../R-D-Frontend\code
REM call npm install
call npm run build:dll
call npm run start:dev-server
cd ../../\R-D-SensorRest
call java -cp h2-latest.jar org.h2.tools.Server -baseDir ./ -webAllowOthers -tcpAllowOthers
pause
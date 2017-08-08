cd ../R-D-Frontend/code
call npm install
call npm run build:dll
start npm run start:dev-server
cd ../../R-D-SensorRest
start java -cp h2-latest.jar org.h2.tools.Server -baseDir ./ -webAllowOthers -tcpAllowOthers
pause
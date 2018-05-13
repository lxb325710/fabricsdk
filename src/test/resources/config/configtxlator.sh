#  启动容器，运行 configtxlator 命令
docker run -d -p 7059:7059 --name configtxlator hyperledger/fabric-tools:x86_64-1.1.0 configtxlator start --hostname="0.0.0.0" --port 7059
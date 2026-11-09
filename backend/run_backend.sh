#!/bin/bash
# 后端启动脚本 - 使用 Boot 4.1.1 完整 Maven 依赖
# 首次运行: mvn dependency:resolve
# 然后: mvn spring-boot:run

cd "$(dirname "$0")"

echo "=== 依赖检查（首次运行请先执行 mvn dependency:resolve）"
# 用 Maven 正常启动（IDEA 也可以直接运行 BackendApplication.main()）
mvn spring-boot:run

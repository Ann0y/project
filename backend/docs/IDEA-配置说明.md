# IDEA 打开后端项目「爆红但能运行」的排查与修复

> 现象：IDEA 里大量 `@Data` 相关代码标红（`Cannot resolve method getName()` 等），
> 但 `mvn spring-boot:run` / `java -jar` 都能正常启动和运行。

这是 **IDE 的代码检查**与 **实际编译** 使用了不同配置导致的，编译实际由 Maven + Lombok
注解处理器完成，所以运行时没问题。下面按出现频率排序，逐项检查。

---

## 1. 项目 JDK 指向了不存在的版本（本次已修复）

**原因**：`.idea/misc.xml` 里 `project-jdk-name` 写的是 `openjdk-26`，
但本机安装的是 JDK 21 和 27，IDEA 找不到这个 SDK，导致整个项目无法解析 JDK、
所有类连带报错。

**本次修复**：改为 `project-jdk-name="21"`、`languageLevel="JDK_21"`（与 pom 的
`java.version=21` 一致）。

**你还需要在 IDEA 里确认**：
```
File → Project Structure → Project
  SDK: 选择本机已安装的 JDK 21（若列表里没有，先 Add SDK 指向 JDK 21 安装目录）
  Language level: 21
```

---

## 2. 未启用 Lombok 插件 / 注解处理

**原因**：`@Data` 等注解由 Lombok 在编译期生成 getter/setter。
IDEA 若没装 Lombok 插件，或没开启注解处理，就会认为这些方法不存在 → 满屏标红。

**检查步骤**：
1. `File → Settings → Plugins`，搜索 **Lombok**，安装并重启 IDEA
2. `File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors`
   勾选 **Enable annotation processing**
3. 确认 Lombok 版本与 pom 一致（pom 里是 `1.18.48`，支持 JDK 21/27）

> `compiler.xml` 里原先写死了 lombok jar 的 `$MAVEN_REPOSITORY$` 路径，
> 若该路径在你的机器上不存在或指向只读仓库，IDEA 会解析失败。
> 本次已移除这段写死路径，交给 IDEA 从 Maven 依赖里自动识别。

---

## 3. `~/.m2` 只读导致 Maven 索引失败（本机环境问题）

**原因**：Maven 4 需要往本地仓库写 `.locks` 锁文件。若 `~/.m2` 为只读，
IDEA 的 Maven 同步会失败，依赖标红。

**本机绕开方式**：已验证可用的仓库副本在 `projects/.m2-repo`。

在 IDEA 里指向它：
```
File → Settings → Build, Execution, Deployment → Build Tools → Maven
  Local repository: /home/annoy/study/projects/.m2-repo
```

或在项目根 `pom.xml` 同目录新增 `.mvn/maven.config`：
```
-Dmaven.repo.local=/home/annoy/study/projects/.m2-repo
```
（本次未提交该文件，避免影响其他机器构建）

---

## 4. 先执行一次 Maven 同步与重新构建

配置改完后，在 IDEA 里依次执行：

```
1. 右键 pom.xml → Maven → Reload project
2. Build → Rebuild Project
3. （仍标红时）File → Invalidate Caches / Restart
```

---

## 5. 验证命令行构建仍然正常

爆红如果只是 IDE 显示问题，命令行 `mvn compile` 应当成功：

```bash
cd backend
mvn -o compile -Dmaven.repo.local=/home/annoy/study/projects/.m2-repo
```

若命令行也失败，那就是真实编译错误，需要按报错修复。

---

## 本次实际改动清单

| 文件 | 改动 |
| --- | --- |
| `.idea/misc.xml` | `project-jdk-name` 从 `openjdk-26` 改为 `21`，补 `languageLevel="JDK_21"` |
| `.idea/compiler.xml` | 移除写死的 lombok 注解处理器路径 |

> `.idea/` 目录已加入 `.gitignore`，这些是本地 IDE 配置，不影响他人克隆后的构建。

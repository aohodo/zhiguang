# 面试讲解：Outbox 可靠消息链路

> 对应版本：知光 `dev` 分支第三批后端重构

## 1. 30 秒面试说法

原系统虽然用了 Outbox、Canal 和 Kafka，但 Canal 调用异步 `send` 后没有等待结果就确认位点，Kafka 发送失败会永久丢事件；消费者又会吞异常，不同业务事件还会互相反序列化失败。我把链路改成至少一次投递：业务数据和 Outbox 同事务，Canal 等待 Kafka Broker 确认后才 ack；失败则 rollback Canal 批次并重试，断线自动重连。消费者只处理自己的事件，成功后才提交 Kafka 位点；失败重试三次后进入 DLT。事件按 Outbox ID 做 Redis 去重，搜索和关系投影本身也设计为可安全重放。

## 2. 完整业务链

```text
业务 Service（MySQL 本地事务）
  ├─ 更新业务表
  └─ 插入 Outbox
       ↓ commit
MySQL binlog
       ↓
Canal getWithoutAck
       ↓
序列化完整 Outbox 行并发送 Kafka
       ↓
等待 Kafka Broker 确认
  ├─ 成功 → ack Canal batchId
  └─ 失败 → rollback Canal batchId，稍后重发
       ↓
canal-outbox Topic
  ├─ search-index-consumer
  └─ relation-outbox-consumer
       ↓
按 entity / eventType 路由
       ↓
按 consumer + eventId 检查完成标记
  ├─ 已完成 → 跳过重复事件
  └─ 未完成 → 执行业务投影
       ↓
成功写入 30 天完成标记并提交 Kafka 位点
  └─ 失败 → 1 秒间隔重试 3 次 → canal-outbox.DLT
```

## 3. 三个确认点

### MySQL 提交点

业务表与 Outbox 必须处于同一个本地事务。Outbox 插入失败会抛出异常，业务修改一起回滚。

### Canal 位点

必须在 `KafkaTemplate.send(...).get(...)` 确认成功之后才能 `connector.ack(batchId)`。发送或序列化失败时调用 `rollback(batchId)`，Canal 会重新投递该批数据。

### Kafka 消费位点

消费者完成投影和幂等标记后才调用 `Acknowledgment.acknowledge()`。异常不能被吞掉，必须抛给统一错误处理器。

## 4. 为什么仍然可能重复

至少一次投递解决“不丢”，代价是可能重复。例如消费者完成 Elasticsearch 写入后进程宕机，还没提交 Kafka 位点，重启后会再次处理。

系统使用两层手段应对：

1. `dedup:outbox:{consumer}:{eventId}` 保存 30 天完成标记，不同消费组互不干扰。
2. 投影操作自身幂等：搜索使用相同文档 ID 覆盖；关系表使用唯一约束和 upsert；用户关注数、粉丝数从数据库事实重新计算，不再依靠可能重复执行的增量加减。

不能只依赖 Redis 去重。进程可能在副作用完成后、写入去重标记前宕机，因此最终处理逻辑本身也必须支持重放。

## 5. 事件隔离

同一个 `canal-outbox` Topic 使用不同 consumer group 广播给多个投影：

- 搜索消费者只处理 `entity=knowpost`。
- 关系消费者只处理 `entity=relation` 或历史 Follow 事件。
- 不属于自己的事件直接跳过并正常提交，不能尝试强制反序列化。
- 属于自己的事件字段缺失属于毒消息，直接进入 DLT，不进行无意义重试。

## 6. 重试、死信和监控

- 瞬时异常：固定间隔 1 秒，最多重试 3 次。
- 格式错误：`IllegalArgumentException` 不重试，直接进入 DLT。
- DLT：`canal-outbox.DLT`，保留原消息以及异常、原 Topic、分区和 Offset 等 Header。
- 指标：
  - `zhiguang.outbox.consumer.processed`
  - `zhiguang.outbox.consumer.duplicate`
  - `zhiguang.outbox.consumer.failed`
  - `zhiguang.kafka.deadletter.published`

本地查看死信：

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 \
  --topic canal-outbox.DLT \
  --from-beginning \
  --property print.headers=true
```

人工重放前必须先确认根因已经修复，再把死信的原始 value 投递回 `canal-outbox`。重复投递仍由事件 ID 去重。

## 7. 高频追问

### 这是不是 exactly-once？

不是。它是业务数据与 Outbox 本地原子提交，加上 Canal/Kafka 至少一次投递和幂等消费。跨 MySQL、Kafka、Redis、Elasticsearch 宣称 exactly-once 不现实，也没有必要。

### 为什么 Canal 不能发送后立刻 ack？

`KafkaTemplate.send` 是异步的。方法返回只表示请求已进入客户端流程，不代表 Broker 已确认；必须等待 Future 成功。

### 为什么搜索和关系要使用不同 consumer group？

不同 group 能让同一事件广播到多个独立投影。若共用 group，一条消息只会被其中一个消费者拿到，其他投影收不到。

### 去重为什么包含 consumer 名称？

同一个事件需要分别构建搜索、关系或其他投影。只使用事件 ID 会导致第一个消费者写完标记后，其他消费者误判为已处理。

### 当前边界是什么？

当前 DLT 已可查看和人工命令行重放，但尚未建设带权限和审计的运营后台；Outbox 表也还需要增加归档与保留策略。这两项适合在管理端模块阶段实现。

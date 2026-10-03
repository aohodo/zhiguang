package com.chuanqing.service;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.chuanqing.utils.OutboxTopicUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CanalKafkaBridgeService implements SmartLifecycle {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;
    private final boolean enabled;
    private final String host;
    private final int port;
    private final String destination;
    private final String username;
    private final String password;
    private final String filter;
    private final int batchSize;
    private final long intervalMs;

    private volatile boolean running;
    private volatile CanalConnector connector;

    public CanalKafkaBridgeService(
            KafkaTemplate<String, String> kafka,
            ObjectMapper objectMapper,
            @Qualifier("taskExecutor") TaskExecutor taskExecutor,
            @Value("${canal.enabled}") boolean enabled,
            @Value("${canal.host}") String host,
            @Value("${canal.port}") int port,
            @Value("${canal.destination}") String destination,
            @Value("${canal.username}") String username,
            @Value("${canal.password}") String password,
            @Value("${canal.filter}") String filter,
            @Value("${canal.batchSize}") int batchSize,
            @Value("${canal.intervalMs}") long intervalMs
    ) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.destination = destination;
        this.username = username;
        this.password = password;
        this.filter = filter;
        this.batchSize = batchSize;
        this.intervalMs = intervalMs;
    }

    @Override
    public void start() {
        if (running || !enabled) {
            log.info("Canal bridge start skipped: running={} enabled={}", running, enabled);
            return;
        }
        running = true;
        taskExecutor.execute(this::runBridgeLoop);
    }

    private void runBridgeLoop() {
        while (running) {
            try {
                connect();
                consumeUntilDisconnected();
            } catch (Exception exception) {
                if (running) {
                    log.error("Canal bridge connection failed; retrying", exception);
                }
            } finally {
                disconnect();
            }
            pauseBeforeRetry();
        }
    }

    private void connect() {
        connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(host, port),
                destination,
                username,
                password
        );
        connector.connect();
        connector.subscribe(filter);
        connector.rollback();
        log.info("Canal connected: host={} port={} destination={} filter={}",
                host, port, destination, filter);
    }

    private void consumeUntilDisconnected() throws Exception {
        while (running) {
            Message message = connector.getWithoutAck(batchSize);
            long batchId = message.getId();
            if (batchId == -1 || message.getEntries() == null || message.getEntries().isEmpty()) {
                pause(intervalMs);
                continue;
            }

            try {
                publishBatch(message);
                connector.ack(batchId);
                log.debug("Canal batch acknowledged after Kafka confirmation, batchId={}", batchId);
            } catch (Exception exception) {
                connector.rollback(batchId);
                log.error("Canal batch rolled back because Kafka publishing failed, batchId={}",
                        batchId, exception);
                pause(intervalMs);
            }
        }
    }

    private void publishBatch(Message message) throws Exception {
        for (CanalEntry.Entry entry : message.getEntries()) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }

            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            CanalEntry.EventType eventType = rowChange.getEventType();
            if (eventType != CanalEntry.EventType.INSERT && eventType != CanalEntry.EventType.UPDATE) {
                continue;
            }

            ArrayNode data = objectMapper.createArrayNode();
            String eventKey = null;
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                ObjectNode row = objectMapper.createObjectNode();
                boolean hasPayload = false;
                for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                    if (column.getIsNull()) {
                        row.putNull(column.getName());
                    } else {
                        row.put(column.getName(), column.getValue());
                    }
                    if ("payload".equalsIgnoreCase(column.getName())) {
                        hasPayload = true;
                    }
                    if (eventKey == null && "id".equalsIgnoreCase(column.getName())) {
                        eventKey = column.getValue();
                    }
                }
                if (!hasPayload) {
                    throw new IllegalArgumentException("Canal outbox row is missing payload");
                }
                data.add(row);
            }

            ObjectNode kafkaMessage = objectMapper.createObjectNode();
            kafkaMessage.put("table", entry.getHeader().getTableName());
            kafkaMessage.put("type", eventType.name());
            kafkaMessage.set("data", data);

            kafka.send(
                    OutboxTopicUtils.CANAL_OUTBOX,
                    eventKey,
                    objectMapper.writeValueAsString(kafkaMessage)
            ).get(30, TimeUnit.SECONDS);
        }
    }

    private void disconnect() {
        CanalConnector current = connector;
        connector = null;
        if (current == null) {
            return;
        }
        try {
            current.disconnect();
            log.info("Canal disconnected: destination={}", destination);
        } catch (Exception exception) {
            log.warn("Canal disconnect failed: destination={}", destination, exception);
        }
    }

    private void pauseBeforeRetry() {
        if (running) {
            pause(Math.max(intervalMs, 1000L));
        }
    }

    private void pause(long durationMs) {
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    @Override
    public void stop() {
        running = false;
        disconnect();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}

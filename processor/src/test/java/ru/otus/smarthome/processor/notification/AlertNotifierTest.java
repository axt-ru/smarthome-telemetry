package ru.otus.smarthome.processor.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AlertNotifierTest {

    private static final long RULE_ID = 1L;
    private static final long DEVICE_ID = 7L;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private RecordingChannel channel;
    private AlertNotifier notifier;

    @BeforeEach
    void setUp() {
        channel = new RecordingChannel();
        notifier = new AlertNotifier(jdbcTemplate, List.of(channel));
    }

    @Test
    @DisplayName("первое оповещение уходит в канал и сохраняется как доставленное")
    void shouldDeliverAndStore() {
        notifier.fire(RULE_ID, DEVICE_ID, 60, "TEST", 25.0, "жарко");

        assertThat(channel.messages).containsExactly("жарко");

        var captor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(anyString(), captor.capture());
        assertThat(captor.getValue()[5]).isEqualTo(true);
    }

    @Test
    @DisplayName("повторное оповещение подавляется, пока не истёк cooldown")
    void shouldRespectCooldown() {
        notifier.fire(RULE_ID, DEVICE_ID, 3600, "TEST", 25.0, "жарко");
        notifier.fire(RULE_ID, DEVICE_ID, 3600, "TEST", 26.0, "ещё жарче");

        assertThat(channel.messages).hasSize(1);
        verify(jdbcTemplate, times(1)).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("cooldown считается отдельно для каждого устройства")
    void shouldTrackCooldownPerDevice() {
        notifier.fire(RULE_ID, DEVICE_ID, 3600, "TEST", 25.0, "первое");
        notifier.fire(RULE_ID, DEVICE_ID + 1, 3600, "TEST", 25.0, "второе");

        assertThat(channel.messages).hasSize(2);
    }

    @Test
    @DisplayName("неизвестный канал не роняет обработку, событие пишется как недоставленное")
    void shouldStoreUndeliveredOnUnknownChannel() {
        notifier.fire(RULE_ID, DEVICE_ID, 60, "TELEGRAM", 25.0, "жарко");

        assertThat(channel.messages).isEmpty();

        var captor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(anyString(), captor.capture());
        assertThat(captor.getValue()[5]).isEqualTo(false);
    }

    private static class RecordingChannel implements NotificationChannel {
        private final List<String> messages = new ArrayList<>();

        @Override
        public String name() {
            return "TEST";
        }

        @Override
        public void send(String message) {
            messages.add(message);
        }
    }
}

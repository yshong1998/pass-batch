package com.fastcampus.pass.job.notification;

import com.fastcampus.pass.repository.booking.BookingEntity;
import com.fastcampus.pass.repository.notification.NotificationEntity;
import com.fastcampus.pass.repository.notification.NotificationEvent;
import com.fastcampus.pass.repository.notification.NotificationModelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import javax.persistence.EntityManagerFactory;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class SendNotificationBeforeClassJobConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final SendNotificationItemWriter sendNotificationItemWriter;
    private final Integer CHUNK_SIZE = 1000;

    @Bean
    public Job sendNotificationBeforeClassJob() {
        return new JobBuilder("sendNotificationBeforeClassJob")
                .start(addNotificationStep())
                .next(sendNotificationStep())
                .build();
    }

    @Bean
    public Step addNotificationStep(){
        return new StepBuilder("addNotificationStep")
                .<BookingEntity, NotificationEntity>chunk(CHUNK_SIZE)
                .reader(addNotificationItemReader())
                .processor(addNotificationItemProcessor())
                .writer(addNotificationItemWriter())
                .build();
    }

    /**
     * BookingEntity 테이블에 대해
     * queryString의 쿼리 수행 결과 중
     * pagesize 만큼 가져온다.
     */
    @Bean
    public JpaPagingItemReader<BookingEntity> addNotificationItemReader() {
        return new JpaPagingItemReaderBuilder<BookingEntity>()
                .name("addNotificationItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("select b from BookingEntity b join fetch b.userEntity " +
                        "where b.status = :status and b.startedAt <= :startedAt " +
                        "order by Desc")
                .build();
    }

    @Bean
    public ItemProcessor<BookingEntity, NotificationEntity> addNotificationItemProcessor(){
        return (bookingEntity -> NotificationModelMapper.INSTANCE.toNotificationEntity(bookingEntity, NotificationEvent.BEFORE_CLASS));
    }

    @Bean
    public JpaItemWriter<NotificationEntity> addNotificationItemWriter(){
        return new JpaItemWriterBuilder<NotificationEntity>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    // 2번째 스탭
    @Bean
    public Step sendNotificationStep(){
        return new StepBuilder("sendNotificationStep")
                .<NotificationEntity, NotificationEntity>chunk(CHUNK_SIZE)
                .reader(sendNotificationItemReader())
                .writer(sendNotificationItemWriter)
                .taskExecutor(new SimpleAsyncTaskExecutor())
                .build();
    }

    // cursor 기반의 방식은 thread safe 하지 않음.
    // 보통 thread safe 해야 하는 상황에서는 page 기반의 방식을 채용
    // 이 기능의 경우는 thread safe 하지 않은 방식이지만 thread safe 해야 할 필요가 있는 경우.
    // 그래서 thread safe 하도록 만들기 위해
    // reader를 synchronized한 클래스를 이용해 동기적으로 처리하도록 하여 safe 하도록 구현
    // 동기 방식이라 느려지긴 하지만 write하는 부분을 비동기로 할 수 있게 된다.
    @Bean
    public SynchronizedItemStreamReader<NotificationEntity> sendNotificationItemReader(){
        // 이벤트(event)가 수업 전이며, 발송 여부(sent)가 미발송인 알람이 조회 대상이 된다.
        JpaCursorItemReader<NotificationEntity> itemReader = new JpaCursorItemReaderBuilder<NotificationEntity>()
                .name("sendNotificationItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("select n from NotificationEntity m where n.event = :event and n.sent = :sent")
                .parameterValues(Map.of("event", NotificationEvent.BEFORE_CLASS, "sent", false))
                .build();
        return new SynchronizedItemStreamReaderBuilder<NotificationEntity>()
                .delegate(itemReader)
                .build();
    }


}

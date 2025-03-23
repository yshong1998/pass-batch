package com.fastcampus.pass.job.pass;

import com.fastcampus.pass.repository.pass.PassEntity;
import com.fastcampus.pass.repository.pass.PassStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Map;

@Configuration
public class ExpirePassesJobConfig {
    // 1개의 chunk 마다 처리할 데이터의 갯수, 이러면 5개의 데이터가 처리된다.
    private final int CHUNK_SIZE = 5;
    private final EntityManagerFactory entityManagerFactory;

    public ExpirePassesJobConfig(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * 순서
     * 1. Job을 생성한다.
     * 2. 해당 Job에서 필요한 Step을 생성한다.(필요한 경우 여러 개)
     * 3. Step의 과정이 되는 read / process / write (또는 read / write)를 각각 생성한다.
     */

    @Bean
    public Job expirePassesJob() {
        return new JobBuilder("expirePassesJob")
                .start(expirePassesStep())
                // .start를 여러 개 체인으로 해서 여러 개의 step을 사용할 수 있다.
                .build();
    }

    @Bean
    public Step expirePassesStep() {
        return new StepBuilder("expirePassesStep")
                .<PassEntity, PassEntity>chunk(CHUNK_SIZE)
                .reader(expirePassesItemReader())
                .processor(expirePassesItemProcessor())
                .writer(expirePassesItemWriter())
                .build();
    }

    // reader
    @Bean
    @StepScope
    public JpaCursorItemReader<PassEntity> expirePassesItemReader() {
        // 커서 기법을 사용, 페이징 기법보다 높은 성능, 데이터 변경에 무관한 무결성 조회가 가능하다.
        return new JpaCursorItemReaderBuilder<PassEntity>()
                .name("expirePassesItemReader")
                .entityManagerFactory(entityManagerFactory)
                // parameterValue에 key : value 로 파라미터들과 함께 queryString이 실행된다.
                // :status가 변수임을 알리는 문법이다.
                .queryString("select p from PassEntity p where p.status = :status and p.endedAt <= :endedAt")
                .parameterValues(Map.of("status", PassStatus.PROGRESSED, "endedAt", LocalDateTime.now()))
                .build();
    }

    // processor, ItemProcessor<I,O> 즉 I를 받아서 O를 반환한다.
    // 여기서는 받는 객체와 주는 객체가 같아서 둘이 같다.
    @Bean
    public ItemProcessor<PassEntity, PassEntity> expirePassesItemProcessor() {
        // reader에서 읽어온 엔티티 각각에 대한 수정 작업, 이후 writer에 전달
        return passEntity -> {
            passEntity.setStatus(PassStatus.EXPIRED);
            passEntity.setExpiredAt(LocalDateTime.now());
            return passEntity;
        };
    }

    // writer, JPAItemWriter는 거의 entityManager를 감싸고 있는 정도의 객체.
    @Bean
    public JpaItemWriter<PassEntity> expirePassesItemWriter() {
        return new JpaItemWriterBuilder<PassEntity>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}

package com.fastcampus.pass.job.pass;

import com.fastcampus.pass.config.TestBatchConfig;
import com.fastcampus.pass.repository.pass.PassEntity;
import com.fastcampus.pass.repository.pass.PassRepository;
import com.fastcampus.pass.repository.pass.PassStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {ExpirePassesJobConfig.class, TestBatchConfig.class})
class ExpirePassesJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private PassRepository passRepository;

    @Test
    void test_expirePassesJob() throws Exception {
        // given : 데이터 10개 저장하기
        addPassEntities(10);
        // when jobLauncherTestUtils는 등록된 job을 실행해 준다.
        // job이 여러 개가 있다면 jobLauncherTestUtils.setJob을 통해 어떤 job을 실행할 것인지
        // 명시해 주어야 한다. 의존성 주입 시 같은 타입의 빈 객체가 둘 이상일 경우
        // @Qualifier를 통해 식별해 주듯 여기서도 @Qualifier를 통해 식별해 줄 수 있다.
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        JobInstance jobInstance = jobExecution.getJobInstance();

        // then
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
        assertEquals("expirePassesJob", jobInstance.getJobName());
    }

    private void addPassEntities(int size) throws Exception {
        final LocalDateTime now = LocalDateTime.now();
        final Random random = new Random();
        ArrayList<PassEntity> passEntities = new ArrayList<>();

        for (int i = 0; i < size; ++i) {
            PassEntity passEntity = new PassEntity();
            passEntity.setPackageSeq(1);
            passEntity.setUserId("A" + 1000000 + i);
            passEntity.setStatus(PassStatus.PROGRESSED);
            passEntity.setRemainingCount(random.nextInt(11));
            passEntity.setStartedAt(now.minusDays(60));
            passEntity.setEndedAt(now.minusDays(1));
            passEntities.add(passEntity);
        }
        passRepository.saveAll(passEntities);
    }
}
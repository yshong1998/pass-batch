package com.fastcampus.pass;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@EnableBatchProcessing
@SpringBootApplication
public class PassBatchApplication {

    public static void main(String[] args) {
		SpringApplication.run(PassBatchApplication.class, args);
	}

    @Bean
    public Step passStep() {
        return new StepBuilder("passStep")
                .tasklet((stepContribution, chunkContext) -> {
                    System.out.println("Execute PassStep");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Job passJob() {
        return new JobBuilder("passJob")
                .start(passStep())
                .build();
    }

}

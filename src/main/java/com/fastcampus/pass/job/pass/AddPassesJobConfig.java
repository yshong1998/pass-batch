package com.fastcampus.pass.job.pass;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AddPassesJobConfig {
    private final AddPassesTasklet addPassesTasklet;

    public AddPassesJobConfig(AddPassesTasklet addPassesTasklet) {
        this.addPassesTasklet = addPassesTasklet;
    }

    @Bean
    public Job addPassesJob() {
        return new JobBuilder("addPassesJob")
                .start(addPassesStep())
                .build();

    }

    @Bean
    public Step addPassesStep() {
        return new StepBuilder("addPassesStep")
                .tasklet(addPassesTasklet)
                .build();

    }
}
